(ns ocr.core
  (:require [clojure.string :as str]
            [ocr.parse :as p]
            [ocr.files :as f]))

(defn format-account
  [digits]
  (str/join "" (map
                #(if (= (:status %) :not-recognized) "?" (str %))
                digits)))

(defn format-reason
  [reason]
  (case reason
    :not-valid-checksum "ERR"
    :un-recognized-digits "ILL"
    :valid ""))


(defn parse-file
  "Given a filename that will resolve to a correctly formatted
  file (three lines of ascii and a blank line separating from the
  next), print the the parsed account and parsing message.

  If you need to create a file matching this, use the `ocr.files`
  namespace for file creation and reading."
  ([filename parse-fn]
   (let [pairs (->> (f/account-numbers-streams filename)
                    (map parse-fn)
                    (map (juxt identity p/analyze-parse)))]
     (doseq [[account reason] pairs]
       (println (format "%s %s"
                        (format-account account)
                        (format-reason reason))))))
  ([filename] (parse-file filename p/parse-completely)))

(defn raw-parse
  []
  (parse-file "resources/test.txt" p/parse))

(defn corrected-parse
  []
  (parse-file "resources/test.txt" p/parse-completely))
