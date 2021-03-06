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
    {:num->str (zipmap digits nums)
     :str->num (zipmap nums digits)}))

(defn int->digits
  [n]
  (map #(Integer/parseInt %) (map str (seq (str n)))))

(defn valid-digits?
  [digits]
  (not-any? (fn [elem] (= (:status elem) :not-recognized)) digits))

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
  (get-in numerals [:str->num s] {:status :not-recognized
                                  :original s}))

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
  (if (not (valid-digits? digits))
    false
    (as-> digits x
      (reverse x)
      (map vector x (rest (range)))
      (map (fn [xs] (apply * xs)) x)
      (apply + x)
      (mod x 11)
      (= x 0))))

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

(defn metric
  "Metric of how far two glyphs g1 and g2 are. Its just a levistein
  distance, ie, how many characters differ"
  [g1 g2]
  (let [s1 (apply str g1)
        s2 (apply str g2)]
    (->> (map vector s1 s2)
         (map #(apply not= %))
         (filter identity)
         count)))

(defn allowable-difference?
  "Not ever difference is allowable. We allow for missed readings of
  pipes and underscores. This means that if one is a space, it's an
  allowable difference. But we cannot have any deletions"
  [g1 g2]
  (let [s1 (apply str g1)
        s2 (apply str g2)]
    (->> (map vector s1 s2)
         (remove (fn [[c1 c2]] (= c1 c2)))
         (map (fn [[c1 c2]] (= c1 \space)))
         (every? identity))))

(defn close?
  "To be a replacement, it must be close and an allowable difference,
  ie, only one difference and that difference is an addition."
  [g1 g2]
  (and (= 1 (metric g1 g2))
       (allowable-difference? g1 g2)))

(def close-digits
  (reduce (fn [m n]
            (assoc m n (set (->> (range 10)
                                 (map digit->str)
                                 (filter (partial close? (digit->str n)))
                                 (map #(str->digit %))))))
          {}
          (range 10)))

(defprotocol Closeness
  (close-possibilities [g-or-d] "Return digits close to the glyph or digit"))

(extend-protocol Closeness
  clojure.lang.PersistentArrayMap
  (close-possibilities [{:keys [original]}]
    (->> (vals (:num->str numerals))
         (filter (partial close? original))
         (map str->digit)))

  clojure.lang.PersistentVector
  (close-possibilities [g]
    (->> (vals (:num->str numerals))
         (filter (partial close? g))
         (map str->digit)))

  java.lang.Long
  (close-possibilities [d]
    (close-digits d)))

(defn recover [digits]
  (let [possible (reduce (fn [{:keys [previous remaining]} current]
                           (let [others (close-possibilities current)
                                 potential (map #(apply conj previous % remaining)
                                                others)
                                 fixes (filter checksum? potential)]
                             (if (seq fixes)
                               (reduced (first fixes))
                               {:previous (conj previous current)
                                :remaining (rest remaining)})))
                         {:previous []
                          :remaining (rest digits)}
                         digits)]
    (if (:remaining possible)
      digits
      possible)))


(defn parse-completely
  "Parse and then try to recover from bad readings and bad checksums"
  [digits]
  ((comp recover parse) digits))
