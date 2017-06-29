(ns ocr.parse-test
  (:require [clojure.test :refer :all]
            [ocr.parse :refer :all]))

(def intparse (comp digits->int parse))

(deftest int->digits-tests
  (testing "correctly splits integer into digit stream"
    (is (= '(1 2 4)
           (int->digits 124)))))

(deftest get-digit-rows-tests
  (testing "spits out ascii numbers"
    (is (= (str "    _  _     _  _  _  _  _ \n"
                "  | _| _||_||_ |_   ||_||_|\n"
                "  ||_  _|  | _||_|  ||_| _|")
           (join-nl (get-digit-rows 123456789))))))

(deftest parse-tests
  (testing "can read the ascii"
    (is (= 123456789
           (intparse '("    _  _     _  _  _  _  _ "
                       "  | _| _||_||_ |_   ||_||_|"
                       "  ||_  _|  | _||_|  ||_| _|")))))
  (testing "can skip gibberish"
    (is (= '(1 2 {:status :not-recognized
                  :original [" _ " "|||" "|||"]} 4)
           (parse '("    _  _    "
                    "  | _|||||_|"
                    "  ||_ |||  |"))))))

(deftest composable-tests
  (testing "can read each others output"
    (doall (for [x (range 34000 35000)]
             (is (= x (-> x
                          get-digit-rows
                          intparse
                          get-digit-rows
                          intparse)))))))
(deftest checksum?-tests
  (testing "recognizes correct sums"
    (is (-> 457508000 int->digits checksum?)))
  (testing "recognizes incorrect sums"
    (is (not (-> 664371495 int->digits checksum?))))
  (testing "says false if there are non-digits (ie, unrecognized)"
    (is (not (checksum? [1 2 4 {:status :not-recognized}])))))

(deftest metric-tests
  (testing "metric recornizes the same"
    (is (= 0 (metric "ab" "ab")))
    (is (= 2 (metric "abcd" "adce")))))

(deftest allowable-differences-tests
  (testing "identifies deletions as allowable"
    (is (allowable-difference? "a " "ab"))
    (is (allowable-difference? "a b c" "axbxc")))
  (testing "won't allow for differences where its not a space"
    (is (not (allowable-difference? "ax" "ab")))
    (is (not (allowable-difference? "-|" " |")))
    (is (not (allowable-difference? "1 3 5 7 9" "123456788")))))

(deftest close?-tests
  (testing "recognizes close forms"
    (is (close? (digit->str 9) (digit->str 8)))))

(deftest close-possibilities-tests
  (testing "recognizes digits close to a digit"
    (is (some  #{8}
               (close-possibilities (digit->str 9))))
    (is (some #{8}
               (close-possibilities 9)))))

(deftest recover-all-digits-tests
  )

(deftest recover-misread-tests
  )

(deftest recover-tests
  ;; 27 is correct, 21 will not checksum
  (let [input (parse [" _  _ "
                      " _|  |"
                      "|_    "])]
    (is (= '(2 7) (recover input))))
  (let [input (parse [" _    "
                      " _|  |"
                      "|_   |"])]
    (is (= '(2 1) input))
    (is (= '(2 7) (recover input))))
  (testing "can correct tests"
    (let [core [2 3 4 5 5 7 8 3]
          bad (concat [1] core)
          fixed (concat [7] core)]
      (is (not (checksum? bad)))
      (is (checksum? fixed))
      (let [recovered (recover bad)]
        (is (= recovered fixed))))
    (let [misread {:status :not-recognized, :original ["   " " _|" " _|"]}]
      (testing "can recover from single misread"
        (let [misread [5 4 misread 6]]
          (is (= [5 4 3 6] (recover misread)))))
      (testing "returns original when two misreads"
        (let [original [5 4 misread misread 6]]
          (is (= original (recover  original))))))))
