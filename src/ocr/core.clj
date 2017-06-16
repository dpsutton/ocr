(ns ocr.core
  (:require [clojure.string :as str]))

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

(defn digit->str
  [n]
  (get-in numerals [:num->str n]))

(defn str->digit
  [s]
  (get-in numerals [:str->num s]))

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
  strings), parse out the corresponding numbers."
  [streams]
  (let [rows (map chunk3 streams)
        grouped (group-glyphs rows)
        digits (map str->digit grouped)]
    (first (reduce (fn [[val power] digit]
                     [(+ val (* digit power)) (* 10 power)])
                   [0 1]
                   (reverse digits)))))

(defn checksum?
  [n]
  (let [digit-seq (int->digits n)]
    (as-> digit-seq x 
         (reverse x)
         (map vector x (rest (range)))
         (map (fn [xs] (apply * xs)) x)
         (apply + x)
         (mod x 11)
         (= x 0))))
