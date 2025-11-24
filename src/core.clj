(ns core
  (:require
   [clojure.core.match :refer [match]]
   [clojure.edn :as edn]
   [nextjournal.clerk :as clerk]))

;; Hello Chat, here we will look at writing a lisp in clojure
;; the idea is to write as little code as possible 👍
;; I would also like the code to be as simple as possible,
;; where running some kind of program is just the composition of read and eval
;; in fact `run` is defined as such with the heady idea that that will happen!
;; The concrete goals are:
;; 1. have a lisp that can do something...
;; 2. have a lisp implemented purely functionally
;;    1. operations are modeled through a composition of functions
;; 3. do as little wrangling as possible
;;    1. types are not "boxed" meaning a jisp type is a clojure / jvm type on the host
;;    2. instead of writing our own tokeniser we leverage `clojure.edn` instead!
;; 
;; I am trying to make this as easy as possible to
;; 1. be a base for further plt experiments
;; 2. make sure I get to the end of the project...
;; In any case lets get into it
;;
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

(defn proc? [expr]
  (match expr
    [:lambda [_ & _] & _] true
    :else false))

(defn primitive? [expr]
  (let [prim-preds [int? string? proc? nil? boolean?]]
    (some identity ((apply juxt prim-preds) expr))))

(defn parse [token]
  (let [parsed (match [token]
                      ;; if its a primitive we return the primitive
                 [(_ :guard #(primitive? %))] token
                 [(_ :guard #(symbol? %))] (keyword token)
                      ;;  if its a lisp we recurse down the list 
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
(read (slurp "resources/function.lisp"))

;; # Evaluating

(def stdenv {:+ +
             := =
             :first first
             :rest rest
             :type type
             :display println})

(defn function-eval [env func arguments]
  (if (proc? func)
    (let [[_ arglist body] func]
      (eval (merge env (zipmap arglist arguments))
            body))
    {:env env :val (apply func arguments)}))

(defn if-eval [env condition true-body false-body]
  (eval env (if (:val (eval env condition))
              true-body
              false-body)))

(defn eval [env expr]
  (match expr
    ;; if the expr is a keyword we get the corresponding value
    (sym :guard #(keyword? %)) {:env env :val (get env sym)}

    ;; if the expr is a primitive then we return the value unchanged
    (prim :guard #(primitive? %)) {:env env :val prim}
    ;; if we run into a define form we add a kv pair to the env,
    ;; we also return the value
    [:define name val] {:env (merge env {name val}) :val val}
    ;; if we run into a begin form we run all the forms inside it
    [:begin & forms] (reduce (fn [{env :env _ :val} form]
                               (eval env form))
                             {:env env :val nil} forms)
    ;; if we run into an... if we conditionally evaluate a body
    [:if condition true-body false-body]  (if-eval env condition true-body false-body)
    ;; if we run into a (:key & argument) form we treat this as a function call
    ;; we get the fn from the environment
    ;; we evaluate all the arguments and extract the values
    ;; (no function arguments should modify the environment...)
    [(func :guard #(keyword? %)) & arguments] (function-eval
                                               env
                                               (get env func)
                                               (mapv (comp :val (partial eval env)) arguments))

    ;; if we encounter this form we return a form and hopefully short circut
    :syntax-error {:error :syntax-error :reason "Bad code innit"}
    ;; else we 
    :else :noop))

;; Running 
(def run (comp (partial eval stdenv) read))

(eval stdenv (read (slurp "resources/function.lisp")))

