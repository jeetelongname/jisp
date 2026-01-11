(ns read
  (:require
   [nextjournal.clerk :as clerk]
   [clojure.edn :as edn]
   [clojure.core.match :refer [match]])
  (:refer-clojure :rename {read core-read}))

;; # Reading
;; 
;; Reading is the process of taking a string and turning it into a
;; datastructure, for a lisp that means turning a string into a representation
;; the language itself uses. In this case I am using clojures lists because of
;; goal 3.1
;;
;; language primitives
;; - "string" : java.lang.String
;; - 10 : java.lang.Long
;; - sym : clojure.lang.Keyword
;; 
;; language composites
;; - vector innit

(defn lambda? [expr]
  (match expr
    [:lambda [& _] _] true
    :else false))

(defn primitive? [expr]
  (let [prim-preds [boolean? int? nil? string?]]
    (some identity ((apply juxt prim-preds) expr))))

(defn parse [token]
  (let [parsed (match [token]
                 [(_ :guard #(primitive? %))] token
                 [(_ :guard #(symbol? %))] (keyword token)
                 [(_ :guard #(list? %))] (mapv parse token)
                 :else :syntax-error)]
    (if (some (partial = :syntax-error) (flatten parsed))
      {:syntax-error token}
      parsed)))

(defn read [string]
  (-> string
      edn/read-string
      parse))

^::clerk/no-cache
(clerk/comment
  (read (slurp "lisp/function.lisp")))
