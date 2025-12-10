(ns task-c6)

;;;an empty route map
;;;it is enough to use either forward or backward part (they correspond to each other including shared reference to number of tickets)
;;;:forward is a map with route start point names as keys and nested map as values
;;;each nested map has route end point names as keys and route descriptor as values
;;;each route descriptor is a map (structure in fact) of the fixed structure where 
;;;:price contains ticket price
;;;and :tickets contains reference to tickets number
;;;:backward has the same structure but start and end points are reverted 
(def empty-map
  {:forward {},
   :backward {}})

(defn route
  "Add a new route (route) to the given route map
   route-map - route map to modify
   from - name (string) of the start point of the route
   to - name (string) of the end poiunt of the route
   price - ticket price
   tickets-num - number of tickets available"
  [route-map from to price tickets-num]
  (let [tickets (ref tickets-num :validator (fn [state] (>= state 0))),     ;reference for the number of tickets 
        orig-source-desc (or (get-in route-map [:forward from]) {}),
        orig-reverse-dest-desc (or (get-in route-map [:backward to]) {}),
        route-desc {:price price,                                            ;route descriptor
                    :tickets tickets},
        source-desc (assoc orig-source-desc to route-desc),
        reverse-dest-desc (assoc orig-reverse-dest-desc from route-desc)]
    (-> route-map
        (assoc-in [:forward from] source-desc)
        (assoc-in [:backward to] reverse-dest-desc))))

(def restart-counter (atom 0))

(defn book-tickets
  "Tries to book tickets and decrement appropriate references in route-map atomically.
   Returns {:path [...] :price ...} or {:error 'no tickets'}."
  [route-map from to]
  (if (= from to)
    {:path [] :price 0}
    (let [cities (keys (:forward route-map))
          dist (atom (zipmap cities (repeat ##Inf)))
          prev (atom (zipmap cities (repeat nil)))]
      (swap! dist assoc from 0)
      (loop [unvisited (set cities)]
        (when (seq unvisited)
          (let [u (apply min-key @dist unvisited)
                neighbors (get-in route-map [:forward u] {})]
            (doseq [[v {:keys [price tickets]}] neighbors]
              (when (pos? @tickets)
                (let [alt (+ (@dist u) price)]
                  (when (< alt (@dist v))
                    (swap! dist assoc v alt)
                    (swap! prev assoc v u)))))
            (recur (disj unvisited u)))))
      (letfn [(reconstruct-path [v acc]
                (if-let [p (@prev v)]
                  (reconstruct-path p (conj acc v))
                  (conj acc from)))]
        (if (or (nil? (@dist to)) (= (@dist to) ##Inf))
          {:error "no tickets"}
          (let [path (reconstruct-path to [])
                route-pairs (partition 2 1 path)]
            (try
              (dosync
               (doseq [[a b] route-pairs]
                 (let [t (get-in route-map [:forward a b :tickets])]
                   (when (zero? @t)
                     (throw (ex-info "no tickets" {})))
                   (alter t dec))))
              (swap! restart-counter inc)
              {:path path
               :price (reduce + (map #(get-in route-map [:forward (first %) (second %) :price])
                                     route-pairs))}
            (catch Exception _
              {:error "no tickets"}))))))))

(def spec1 (-> empty-map
               (route "City1" "Capital"    200 5)
               (route "Capital" "City1"    250 5)
               (route "City2" "Capital"    200 5)
               (route "Capital" "City2"    250 5)
               (route "City3" "Capital"    300 3)
               (route "Capital" "City3"    400 3)
               (route "City1" "Town1_X"    50 2)
               (route "Town1_X" "City1"    150 2)
               (route "Town1_X" "TownX_2"  50 2)
               (route "TownX_2" "Town1_X"  150 2)
               (route "Town1_X" "TownX_2"  50 2)
               (route "TownX_2" "City2"    50 3)
               (route "City2" "TownX_2"    150 3)
               (route "City2" "Town2_3"    50 2)
               (route "Town2_3" "City2"    150 2)
               (route "Town2_3" "City3"    50 3)
               (route "City3" "Town2_3"    150 2)))

(defn booking-future [route-map from to init-delay loop-delay]
  (future
    (Thread/sleep init-delay)
    (loop [bookings []]
      (Thread/sleep loop-delay)
      (let [booking (book-tickets route-map from to)]
        (println from "->" to "attempt:" booking)

        (if (:error booking)
          bookings
          (recur (conj bookings booking)))))))




(defn print-bookings [name ft]
  (println (str name ":") (count ft) "bookings")
  (doseq [booking ft]
    (println "price:" (booking :price) "path:" (booking :path))))

(defn run []
  (reset! restart-counter 0)
  (let [f1 (booking-future spec1 "City1" "City3" 0 10)
        f2 (booking-future spec1 "City1" "City2" 0 10)
        f3 (booking-future spec1 "City2" "City3" 0 10)
        b1 @f1
        b2 @f2
        b3 @f3]
    (print-bookings "City1->City3" b1)
    (print-bookings "City1->City2" b2)
    (print-bookings "City2->City3" b3)
    (println "Total transaction attempts:" @restart-counter)))

(run)
