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
    (is (= '(1 2 :not-recognized 4)
           (parse '("    _  _    "
                    "  | _|||||_|"
                    "  ||_ |||  |"))))))

(deftest composable-tests
  (testing "can read each others output"
    (doall (for [x (range 34000 35000)]
             (do (is (= x (intparse (get-digit-rows x))))
                 (is (= x (-> x
                              get-digit-rows
                              intparse
                              get-digit-rows
                              intparse))))))))
(deftest checksum?-tests
  (testing "recognizes correct sums"
    (is (-> 457508000 int->digits checksum?)))
  (testing "recognizes incorrect sums"
    (is (not (-> 664371495 int->digits checksum?)))))
