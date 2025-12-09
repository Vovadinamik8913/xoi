(ns pfilter
  (:require [clojure.test :refer [deftest testing is]]))

(defn parallel-filter
  ([pred coll] (parallel-filter pred coll 10 8))
  ([pred coll chunk-size] (parallel-filter pred coll chunk-size 8))
  ([pred coll chunk-size group-size]
   (letfn [(process-chunk [chunk]
             (future (doall (filter pred chunk))))
           
           (lazy-pfilter [s]
             (lazy-seq
               (when (seq s)
                (let [chunks (take group-size
                                    (partition-all chunk-size s))
                       rest-seq (drop (* chunk-size group-size) s)
                       futures (doall (map process-chunk chunks))
                       results (mapcat deref futures)]
                   (concat results (lazy-pfilter rest-seq))))))]

     (lazy-pfilter coll))))

(deftest pfilter-test
  (testing "Базовая функциональность - конечная коллекция"
    (let [result (parallel-filter even? [1 2 3 4 5 6 7 8 9 10] 3)]
      (is (= [2 4 6 8 10] result))
      (is (sequential? result))))
  
  (testing "Пустая коллекция"
    (is (empty? (parallel-filter even? [] 10))))
  
  (testing "Все элементы удовлетворяют предикату"
    (let [result (parallel-filter pos? [1 2 3 4 5] 2)]
      (is (= [1 2 3 4 5] result))))
  
  (testing "Ни один элемент не удовлетворяет предикату"
    (is (empty? (parallel-filter neg? [1 2 3 4 5] 2))))
  
  (testing "Работа с бесконечными последовательностями"
    (let [result (->> (parallel-filter even? (range) 100)
                      (take 5))]
      (is (= [0 2 4 6 8] result))))
  
  (testing "Разные размеры чанков"
    (let [coll (range 20)
          result1 (parallel-filter odd? coll 5)
          result2 (parallel-filter odd? coll 10)]
      (is (= [1 3 5 7 9 11 13 15 17 19] result1))
      (is (= result1 result2))))
  
  (testing "Строковая коллекция"
    (let [result (parallel-filter #(> (count %) 3) 
                                  ["a" "ab" "abc" "abcd" "abcde"] 2)]
      (is (= ["abcd" "abcde"] result)))))

(defn- heavy-predicate [x]
  ;(println "X = " x)
  (Thread/sleep 1)
  (even? x))

(defn benchmark-filter
  []
  (let [test-data (range 10000)
        chunk-sizes [1 10 25 50 100 500 625 1000 1250 2500 5000]]
    
    (println "=== Сравнение производительности фильтра ===")
    (println "Данные:" (count test-data) "элементов")
    (println "Тяжелый предикат (1ms на элемент)")
    (println)
    
    (println "Стандартный filter:")
    (time (doall (filter heavy-predicate test-data)))
    (println)
    
    (doseq [chunk-size chunk-sizes]
      (println "Параллельный filter (chunk-size:" chunk-size "):")
      (time (doall (parallel-filter heavy-predicate test-data chunk-size)))
      (println))))

(when (empty? *command-line-args*)
  (println "Запуск unit-тестов...")
  (clojure.test/run-tests  'pfilter)
  
  (println "\nЗапуск бенчмарка производительности...")
  (benchmark-filter)
  
  (println "Демонстрация с бесконечной последовательностью:")
  (let [first-10-evens (->> (parallel-filter even? (range) 50)
                            (take 10))]
    (println "Первые 10 четных чисел:" first-10-evens)))
