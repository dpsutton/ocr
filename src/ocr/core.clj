(ns ocr.core
  (:require [clojure.string :as str]))

;; don't run the formatter. there's a trailing space on the top of the
;; 9 that if lost causes partition to see a partition not long enough
;; so our 9 doesn't get included. not the best
(def numerals-text "
 _     _  _     _  _  _  _  _ 
| |  | _| _||_||_ |_   ||_||_|
|_|  ||_  _|  | _||_|  ||_| _|
  ")

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
  "given a vector of top middle bottom, each of which is partitioned into 3 character glyps, combine the glyphs into their logical units. aka
  (group-glyphs [[abc def] [hij klm] [uvw xyz]]) =>
  ([abc hij uvw] [def klm xyz])."
  [[top middle bottom]]
  (map vector top middle bottom))

(def numerals
  (let [nums (group-glyphs (map chunk3 numeral-streams))
        digits (range 10)] ; this range must be same order as ascii
                                        ; glyphs above
    {:num->str (into {} (map (fn [n d] [d n]) nums digits))
     :str->num (into {} (map (fn [n d] [n d]) nums digits))}))

(defn int->digits
  [n]
  (map #(Integer/parseInt %) (map str (seq (str n)))))

(defn valid-digits?
  [digits]
  (not-any? #{:not-recognized} digits))

(defn digits->int
  [digits]
  (when (valid-digits? digits)
    (first (reduce (fn [[val power] digit]
                     [(+ val (* digit power)) (* 10 power)])
                   [0 1]
                   (reverse digits)))))

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
  (let [rows (map chunk3 streams)
        grouped (group-glyphs rows)]
    (map str->digit grouped)))

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
