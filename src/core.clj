;; # Implementing a Toy Lisp in Clojure
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
(read (slurp "lisp/function.lisp"))

;; # Evaluating
(declare evaluate)

(def stdenv {:+ +
             :- -
             :* *
             :/ /
             := =
             :first first
             :rest rest
             :type type
             :display println})

;; as we are trying to emulate lexical bindings
;; before we move on we remove the local variables
;; This means if we ever use define in a function
;; it will remain local to the function body.
;;
;; It also means if we ever call an unbound name it will return nil
;; but as we do not have the concept of errors thats probably fine.
(defn evaluate-function [env func arguments]
  (if (proc? func)
    (let [[_ arglist body] func]
      (merge
       (evaluate (merge env (zipmap arglist arguments)) body)
       {:env env}))
    {:env env :val (apply func arguments)}))

(defn evaluate-if [env condition true-body false-body]
  (evaluate env (if (:val (evaluate env condition))
                  true-body
                  false-body)))

;; we transform the let into a function as I am pulling heavily
;; from scheme syntax and semantics. what this entails is
;; 1. the only way to introduce a new scope is to introduce a function!
;; 2. let could in theory be implemented by a macro..
(defn transform-let [bindings body]
  (let [arg-names (mapv first bindings)
        arg-values (mapv last bindings)]
    [[:lambda arg-names body]
     arg-values]))

(defn evaluate [env expr]
  (match expr
    ;; if the expr is a keyword we get the corresponding value
    (sym :guard #(keyword? %)) {:env env :val (get env sym)}

    ;; if the expr is a primitive then we return the value unchanged
    (prim :guard #(primitive? %)) {:env env :val prim}

    ;; if we run into a define form we add a kv pair to the env,
    ;; we also return the value
    [:define name val] {:env (merge env {name val}) :val val}

    ;; if we run into a begin form we run all the forms inside it
    [:begin & forms]
    (reduce (fn [{env :env _ :val} form]
              (evaluate env form))
            {:env env :val nil} forms)

    ;; if we run into an... if we conditionally evaluate a body
    [:if condition true-body false-body]
    (evaluate-if env
                 condition
                 true-body
                 false-body)

    ;; if we run into a let body
    ;; we transform the let binding into a lambda and run that

    [:let bindings body]
    (let [[fn-body argument-list] (transform-let bindings body)]
      (evaluate-function env fn-body (mapv (comp :val (partial evaluate env))
                                           argument-list)))

    ;; if we run into a (:key & argument) form we treat this as a function call
    ;; we get the fn from the environment
    ;; we evaluate all the arguments and extract the values
    ;; (no function arguments should modify the environment...)
    [(func :guard #(keyword? %)) & arguments]
    (evaluate-function env
                       (get env func)
                       (mapv (comp :val (partial evaluate env))
                             arguments))
    ;; if this somehow passes all of our forms we just :noop for the time being
    :else :noop))

;; Running 
(def run (comp (partial evaluate stdenv) read))

(run (slurp "lisp/function.lisp"))

