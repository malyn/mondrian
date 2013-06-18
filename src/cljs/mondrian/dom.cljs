(ns mondrian.dom)

(defn jsval->clj
  "Converts a JavaScript string into a native Clojure value."
  [s]
  (cond
    (= s "true") true
    (= s "false") false
    (= s "null") nil
    (= (js* "+~{s} + \"\"") s) (js* "+~{s}")
    :else s))

(defn data
  "Returns a map containing the given element's data attributes (with
  Clojure-style hyphenated keyword names, not JavaScript-style
  camelCase)."
  [elem]
  (let [attrs (.-attributes elem)]
    (into {} (for [i (range (.-length attrs))
                   :let [attr (aget attrs i)
                         k (.-name attr)
                         v (.-value attr)]
                   :when (re-matches #"^data-.*" k)]
               [(-> k (subs 5) keyword) (jsval->clj v)]))))
