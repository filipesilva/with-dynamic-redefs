(ns with-dynamic-redefs-test
  (:require
   [with-dynamic-redefs :as sut]
   [clojure.test :refer [deftest is]]))

(defn ^:bar foo []
  :none)

(deftest marks-wrapped-test
  (sut/with-dynamic-redefs [foo (constantly :foo)])
  (is (-> foo meta ::sut/wrapped)))

(deftest thread-safe-test
  (let [results (pmap (fn [i]
                        (sut/with-dynamic-redefs [foo (constantly i)]
                          (foo)))
                      (range 100))]
    (is (= (range 100) results))))

(deftest meta-test
  ;; Redef once to add wrapper.
  (sut/with-dynamic-redefs [foo (constantly true)])
  (let [fn-meta (meta foo)
        var-meta (meta #'foo)]
    (is (::sut/wrapped fn-meta))
    (is (::sut/original fn-meta))
    (is (:bar var-meta))
    (sut/with-dynamic-redefs [foo (with-meta (constantly 1) {:baz true})]
      (is (-> foo meta :baz))
      (is (= var-meta (meta #'foo))))
    (is (= fn-meta (meta foo)))
    (is (= var-meta (meta #'foo)))))

(deftest nested-test
  (sut/with-dynamic-redefs [foo (constantly :one)]
    (is (= :one (foo)))
    (sut/with-dynamic-redefs [foo (constantly :two)]
      (is (= :two (foo)))
      (sut/with-dynamic-redefs [foo (constantly :three)]
        (is (= :three (foo))))
      (is (= :two (foo))))
    (is (= :one (foo)))))

(deftest original-test
  (sut/with-dynamic-redefs [foo (constantly 1)]
    (is (= 1 (foo)))
    (is (= :none ((sut/original foo))))))

(deftest conveyance-test
  (sut/with-dynamic-redefs [foo (constantly 1)]
    ;; NB: the foo binding itself is not conveyed since foo is not a dynamic var.
    ;; Instead, sut/*thread-redefs* is conveyed, and the root binding for var
    ;; is altered to look it up in all threads.
    @(future (is (= 1 (foo))))))
