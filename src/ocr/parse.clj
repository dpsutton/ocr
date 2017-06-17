(ns ocr.parse
  (:require [clojure.string :as str]))

(def numerals-text (str " _     _  _     _  _  _  _  _ \n"
                        "| |  | _| _||_||_ |_   ||_||_|\n"
                        "|_|  ||_  _|  | _||_|  ||_| _|\n"))

(def numeral-streams (->> numerals-text
                          str/split-lines
                          (remove str/blank?)))

(defn chunk3
  "Partition a string into 3 character substrings"
  [s]
  (map #(apply str %) (partition 3 s)))

(defn join-nl
  "join a coll with newlines"
  [coll]
  (str/join "\n" coll))

(defn group-glyphs
  "Given a stream of top, middle, bottom of strings, partition them
  into chunks of three and clump them into the top middle and bottom
  rows for each glyph."
  [streams]
  (let [[top middle bottom] (map chunk3 streams)]
    (map vector top middle bottom)))

(def numerals
  (let [nums (group-glyphs numeral-streams)
        digits (range 10)] ; this range must be same order as ascii
                                        ; glyphs above
    {:num->str (into {} (zipmap digits nums))
     :str->num (into {} (zipmap nums digits))}))

(defn int->digits
  [n]
  (map #(Integer/parseInt %) (map str (seq (str n)))))

(defn valid-digits?
  [digits]
  (not-any? #{:not-recognized} digits))

(defn digits->int
  [digits]
  (when (valid-digits? digits)
    (reduce (fn [val [digit power]]
              (+ val (* digit power)))
            0
            (map vector (reverse digits)
                 (iterate (partial * 10) 1)))))

(defn digit->str
  "This function will always return a digit given the constraint 0 <=
  n <= 9"
  [n]
  (assert (<= 0 n 9) "only single digits should be looked up")
  (get-in numerals [:num->str n]))

(defn str->digit
  "Given a vector of top middle and bottom rows, return the digit
  associated with it or else :not-recognized"
  [s]
  (get-in numerals [:str->num s] :not-recognized))

(def blank ["" "" ""])

(defn get-digit-rows
  "Returns a vector of the top middle and bottom rows as strings from
  n, an integer."
  [n]
  (let [n (int->digits n)
        digits (map digit->str n)]
    (reduce (fn [rows d]
              (map str rows d))
            blank
            digits)))

(defn parse
  "Given a seq of top, middle, and bottom rows (not grouped, just the
  strings), parse out the corresponding numbers. Returns a seq of
  digits and :not-recognized keywords"
  [streams]
  (let [glyphs (group-glyphs streams)]
    (map str->digit glyphs)))

(defn checksum?
  [digits]
  (assert (valid-digits? digits) "Cannot checksum when there are invalid digits")
  (as-> digits x
    (reverse x)
    (map vector x (rest (range)))
    (map (fn [xs] (apply * xs)) x)
    (apply + x)
    (mod x 11)
    (= x 0)))

(defn analyze-parse
  "Given a sequence of digits and :not-recognized, analyzed whether it
  is a valid parsed result"
  [digits]
  (cond
    (not (valid-digits? digits))
    :unrecognized-digits

    (not (checksum? digits))
    :not-valid-checksum

    :else
    :valid))
