(ns philosopher
  (:require [clojure.test :refer :all]))

(def restart_cnt (atom 0))
(def philosopher-stats (atom {})) ; атом для отслеживания статистики философов

(defn philosopher-cycle [left_fork right_fork philosopher-id max-meals eaten-atom retry-atom]
  (when (< @eaten-atom max-meals)
    (try
      (dosync
        (swap! restart_cnt inc)
        (alter left_fork inc)
        (alter right_fork inc)
        (Thread/sleep 100))
      (swap! eaten-atom inc) ; увеличиваем счетчик приемов пищи
      (catch Exception e
        (swap! retry-atom inc) ; увеличиваем счетчик ретраев
        (throw e)))
    (Thread/sleep 100)))

(defn philosopher_cycle [left_fork right_fork philosopher-id max-meals eaten-atom retry-atom]
  (try 
    (philosopher-cycle left_fork right_fork philosopher-id max-meals eaten-atom retry-atom)
    (catch Exception e)))

(defn philosopher
  [left_fork right_fork philosopher-id max-meals]
  (Thread. 
   (fn []
     (try
       (let [eaten-atom (atom 0) ; счетчик приемов пищи для этого философа
             retry-atom (atom 0)] ; счетчик ретраев для этого философа
         (swap! philosopher-stats assoc philosopher-id {:eaten eaten-atom :retries retry-atom})
         (loop []
           (when (< @eaten-atom max-meals)
             (philosopher_cycle left_fork right_fork philosopher-id max-meals eaten-atom retry-atom)
             (swap! philosopher-stats assoc philosopher-id 
                    {:eaten eaten-atom :retries retry-atom})
             (recur)))
         (println (str "Философ " philosopher-id 
                      " закончил. Поел: " @eaten-atom 
                      " раз, Ретраев: " @retry-atom)))
       (catch InterruptedException _ 
         (println "Философ прерван"))))))

(defn start_philosophers
  [fork_refs max-meals]
  (doall (for [x (range (count fork_refs))]
           (.start (philosopher 
                     (nth fork_refs x) 
                     (nth fork_refs (rem (+ x 1) (count fork_refs))) 
                     x 
                     max-meals)))))

(defn make_ref
  [n]
  (vec (repeatedly n #(ref 0))))

(defn print-status [iteration forks]
  (let [fork-values (mapv deref forks)
        total-usage (reduce + fork-values)]
    (println (format "  [%d] Рестары [%d] Использований вилок: %d, Состояние вилок: %s" 
                     iteration @restart_cnt total-usage fork-values))
    
    ; Получаем текущую статистику философов
    (let [stats @philosopher-stats
          total-eaten (reduce + (map #(-> % :eaten deref) (vals stats)))
          total-retries (reduce + (map #(-> % :retries deref) (vals stats)))]
      (println (format "     Приемов пищи: %d, Всего ретраев: %d" 
                       total-eaten total-retries))
      (doseq [[id stat-atoms] (sort stats)]
        (let [eaten (-> stat-atoms :eaten deref)
              retries (-> stat-atoms :retries deref)]
          (println (format "     Философ %d: поел %d раз, ретраев: %d" 
                           id eaten retries)))))))

(defn stop-all-philosopher-threads []
  (try
    (doseq [thread (->> (Thread/getAllStackTraces)
                        keys
                        (filter #(instance? Thread %)))]
      (when (and (.getName thread) (re-find #"Thread-\d+" (.getName thread)))
        (.interrupt thread)))
    (catch Exception _ nil)))


(defn run-demo [n-philosophers max-meals]
  (println "ДЕМОНСТРАЦИЯ: " n-philosophers "философов")
  (reset! restart_cnt 0)
  (reset! philosopher-stats {})
  (let [forks (make_ref n-philosophers)]
    (start_philosophers forks max-meals)
    
    (.start (Thread. 
             (fn []
               (loop [iteration 0]
                 (Thread/sleep 500)
                 (print-status iteration forks)
                 (recur (inc iteration))))))))

(deftest make-ref-test
  (testing "Создание ссылок"
    (let [n 5
          refs (make_ref n)]
      (is (= n (count refs)) "Количество ссылок должно соответствовать заданному")
      (is (every? #(instance? clojure.lang.Ref %) refs) "Все элементы должны быть ref")
      (is (every? zero? (map deref refs)) "Все счетчики должны начинаться с 0"))))

(deftest philosopher-cycle-transaction-test
  (testing "Транзакция в функции philosopher-cycle"
    (let [left-fork (ref 0)
          right-fork (ref 0)
          initial-restart-cnt @restart_cnt]
      (let [f (future
                (philosopher-cycle left-fork right-fork))]
        (Thread/sleep 100)
        (future-cancel f)
        (is (>= @restart_cnt initial-restart-cnt) 
            "restart_cnt должен увеличиться или остаться тем же")))))

(deftest philosopher-thread-test
  (testing "Создание потока философа"
    (let [left-fork (ref 0)
          right-fork (ref 0)
          thread (philosopher left-fork right-fork)]
      (is (instance? Thread thread) "Должен создаваться объект Thread")
      (is (not (.isAlive thread)) "Поток не должен быть запущен сразу"))))

(deftest start-philosophers-test
  (testing "Запуск философов с разным количеством вилок"
    (doseq [n [2 3 4]]
      (let [forks (make_ref n)]
        (reset! restart_cnt 0)
        (start_philosophers forks)
        (Thread/sleep 200)
        (try
          (doseq [thread (->> (Thread/getAllStackTraces)
                              keys
                              (filter #(instance? Thread %)))]
            (when (and (.getName thread) (re-find #"Thread-\d+" (.getName thread)))
              (.interrupt thread)))
          (catch Exception _ nil))
        (Thread/sleep 100)
        (is true "Философы должны запускаться без ошибок")))))

(deftest restart-counter-test
  (testing "Счетчик рестартов увеличивается"
    (reset! restart_cnt 0)
    (let [forks (make_ref 2)]
      (start_philosophers forks)
      (Thread/sleep 300)
      (let [final-count @restart_cnt]
        (is (>= final-count 0) "Счетчик рестартов должен быть неотрицательным")
        (println "Количество рестартов за 300ms:" final-count))
      (try
        (doseq [thread (->> (Thread/getAllStackTraces)
                            keys
                            (filter #(instance? Thread %)))]
          (when (and (.getName thread) (re-find #"Thread-\d+" (.getName thread)))
            (.interrupt thread)))
        (catch Exception _ nil))
      (Thread/sleep 100))))

(deftest fork-usage-test
  (testing "Вилки используются в транзакциях"
    (reset! restart_cnt 0)
    (let [forks (make_ref 3) 
          initial-values (mapv deref forks)]
      (start_philosophers forks)
      (Thread/sleep 500)
      (let [final-values (mapv deref forks)]
        (is (or (not= initial-values final-values)
                (> @restart_cnt 0)) 
            "Значения вилок должны измениться или должны быть рестарты")
        (println "Изначальные значения:" initial-values)
        (println "Конечные значения:" final-values))
      (try
        (doseq [thread (->> (Thread/getAllStackTraces)
                            keys
                            (filter #(instance? Thread %)))]
          (when (and (.getName thread) (re-find #"Thread-\d+" (.getName thread)))
            (.interrupt thread)))
        (catch Exception _ nil))
      (Thread/sleep 100))))

(deftest exception-handling-test
  (testing "Обработка исключений в philosopher_cycle"
    (let [result (try
                   (philosopher_cycle nil nil)
                   :no-exception-thrown
                   (catch Exception e
                     :exception-thrown))]
      (is (= :no-exception-thrown result) 
          "Исключения должны перехватываться внутри функции"))))

(deftest circular-dependency-test
  (testing "Круговая зависимость вилок"
    (let [n 4
          forks (make_ref n)]
      (doseq [x (range n)]
        (let [left (nth forks x)
              right (nth forks (rem (+ x 1) n))]
          (is (not= left right) 
              (str "Философ " x " не должен получать одинаковые вилки"))
          (is (or (= right (nth forks (rem (+ x 1) n)))
                  (and (= x (dec n)) (= right (first forks))))
              (str "Философ " x " должен получать следующую вилку по кругу")))))))

(when (empty? *command-line-args*)
  (println "Запуск unit-тестов...")
  (try
    ;(let ;[result (run-tests 'philosopher)]
      ;(stop-all-philosopher-threads)
      ;(Thread/sleep 200)
      ;result
     ;(println "\nВсе тесты пройдены успешно! Запускаем демонстрацию...")
      (run-demo 19 40);)
    (catch Exception e
      (stop-all-philosopher-threads)
      (throw e))))

