(ns with-dynamic-redefs
  "Inspired by https://github.com/mourjo/dynamic-redef and
  https://gist.github.com/mourjo/c7fc03e59eb96f8a342dfcabd350a927,
  but preserves function metadata.")

(def ^:dynamic *thread-redefs* {})

(defn proxy-fn
  [f a-var]
  (if (-> f meta ::wrapped)
    f
    (proxy [clojure.lang.IMeta clojure.lang.IFn clojure.lang.IObj][]
      (meta [] (assoc (meta (get *thread-redefs* a-var f))
                      ::wrapped true
                      ::original f))
      (invoke [& args] (apply (get *thread-redefs* a-var f) args))
      (applyTo [args] (apply (get *thread-redefs* a-var f) args)))))

(defn with-dynamic-redefs-fn
  [vars func]
  ;; Optimistically try to filter out vars that are already proxied.
  ;; Still have to check in proxy-fn because it is an atomic operation
  ;; and it is possible that another thread has already proxied it.
  (doseq [a-var (remove #(-> % var-get meta ::wrapped) vars)]
    (alter-var-root a-var proxy-fn a-var))
  (func))

(defn xs->map
  [xs]
  (reduce (fn [acc [k v]] (assoc acc `(var ~k) v))
          {}
          (partition 2 xs)))

(defmacro with-dynamic-redefs
  "Like with-redefs, but is thread safe via dynamic vars.
  Only supports redefining functions.
  Permanently proxies redefined functions to preserve metadata.
  Not recommended outside of tests."
  [bindings & body]
  (let [map-bindings (xs->map bindings)]
    `(let [tr# *thread-redefs*]
       (binding [*thread-redefs* (merge tr# ~map-bindings)]
         (with-dynamic-redefs-fn ~(vec (keys map-bindings))
                         (fn [] ~@body))))))

(defn original
  "Returns the original function, if any, proxied by with-dynamic-redefs."
  [f]
  (or (-> f meta ::original)
      f))
