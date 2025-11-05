(ns string-combinations
  (:require [clojure.string :as str]))

(defn generate-strings [alphabet n]
    (->> (range n)
         (reduce (fn [acc i]
                   (if (empty? acc)
                     alphabet
                     (for [s acc
                           char alphabet
                           :when (not= (str (last s)) char)]
                       (str s char))))
                 [])
         vec))

(let [args *command-line-args*]
    (let [alphabet (str/split (first args) #"")
          n (Integer/parseInt (second args))
          result (generate-strings alphabet n)]
      
      (println "Алфавит:" alphabet)
      (println "Длина строки:" n)
      (println "Комбинации:" result)
      (println "Количество:" (count result))))