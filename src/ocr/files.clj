(ns ocr.files
  (:require [ocr.parse :as p]
            [clojure.java.io :as io]))

(defn random-ascii-number
  []
  (let [n (take 9 (repeatedly #(rand-int 10)))]
    (-> n
        p/digits->int
        p/get-digit-rows
        p/join-nl
        (str "\n"))))

(defn create-file
  "Create a file with `n` ascii numbers in a file named `filename`."
  [filename n]
  (with-open [f (io/writer (io/file "resources" filename))]
    (dotimes [_ n]
      (let [number (random-ascii-number)]
        (.write f number)
        (.write f "\n")))))

(defn account-numbers-streams
  "Returns a lazy sequence of top middle bottom rows of the ascii
  account numbers."
  [filename]
  (partition 3 4 (line-seq (io/reader filename))))
