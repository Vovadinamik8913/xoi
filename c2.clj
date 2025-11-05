(ns prime-sieve
  (:require [clojure.test :refer [deftest testing is]]))

(defn primes-sieve-eratosthenes []
  (letfn [(sieve [table n]
            (if-let [prime-factors (get table n)]
              (recur (reduce (fn [t p]
                               (update t (+ n p) (fnil conj []) p))
                             (dissoc table n)
                             prime-factors)
                     (inc n))
              (cons n 
                    (lazy-seq 
                      (sieve (assoc table (* n n) [n]) 
                             (inc n))))))]
    (sieve {} 2)))

(defn take-primes [n]
  (take n (primes-sieve-eratosthenes)))

(let [args *command-line-args*]
  (when (seq args)
    (let [n (Integer/parseInt (first args))
          result (take-primes n)]
      (println "Первые" n "простых чисел:" result))))


(deftest test-primes
  (testing "First 10 primes with improved sieve"
    (is (= [2 3 5 7 11 13 17 19 23 29]
           (take 10 (primes-sieve-eratosthenes)))))
  
  (testing "Prime properties"
    (let [first-20 (take 20 (primes-sieve-eratosthenes))]
      (is (every? #(> % 1) first-20))
      (is (apply < first-20))
      (is (= 2 (first first-20)))
    
    (testing "Specific prime checks"
      (let [primes (primes-sieve-eratosthenes)]
        (is (some #(= 97 %) (take 100 primes)))
        (is (some #(= 541 %) (take 100 primes)))
        (is (not (some #(= 1 %) (take 100 primes))))))))
)

(deftest test-take-primes
  (testing "Take specific number of primes"
    (is (= [2 3 5] (take-primes 3)))
    (is (= [2 3 5 7 11 13 17 19 23 29 31 37 41 43 47] 
           (take-primes 15)))))

(when (empty? *command-line-args*)
  (println "Запуск тестов...")
  (clojure.test/run-tests 'prime-sieve))