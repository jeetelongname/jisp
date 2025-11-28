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
(read (slurp "lisp/function.lisp"))

;; # Evaluating
;; Evaluating is the task of walking the datastructure we have and returning a value
;; In this case we have to define the evaluation rules for certain forms.
;; every rule must return two things, an environment, which contains a mapping of names to values
;; and the value of the form itself after evaluation.
(declare evaluate)

(def stdenv {:+ +
             :- -
             :* *
             :/ /
             := =
             :first first
             :rest rest
             :type type
             :display println
             :pi Math/PI
             :e Math/E})

;; if we encounter a lambda we should evaluate it into a closure (haha)
;; A closure captures the environment avalible to the lambda at the point of its creation and bundles it with the code
;; this means when we call the lambda it will be able to see what it captured,
;; the simple way to do this would be to bundle the current env map and call it a day,
;; This would be faster in a time complexity sense as each call would at most look in one map
;; however this would in theory balloon the memory footprint of the language
;; a more efficent usecase would be to extract all the symbols from the lambda and only store those ones
;; then at time of execution merging the outer environment with the closures environment and execute the body in that.
;; however that would involve a rethinking of the entire env datastructure from a nice simple mapping to something more complex
(defn closure? [token]
  (match token
    {:lambda _ :capture _} true
    :else false))

;; as we are trying to emulate lexical bindings
;; before we move on we remove the local variables
;; This means if we ever use define in a function
;; it will remain local to the function body.
(defn evaluate-lambda [env lambda]
  {:env env
   :val {:lambda lambda :capture env}})

(defn evaluate-function [env func arguments]
  (let [arguments (mapv (comp :val (partial evaluate env)) arguments)]
    (if (closure? func)
      (let [{capture :capture [_ arglist body] :lambda} func
            lambda-env (merge env capture (zipmap arglist arguments))]
        (merge
         (evaluate lambda-env body)
         {:env env}))
      {:env env :val (apply func arguments)})))

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
    ;; if we encounter a quoted form we return it unevaluated this is the lisp
    ;; way to produce lists and cements the fact that the representation of the
    ;; language is its own list type
    [:quote form] {:env env :val form}

    ;; if the expr is a primitive then we return the value unchanged
    (prim :guard #(primitive? %)) {:env env :val prim}

    ;; if the expr is a keyword we get the corresponding value
    (sym :guard #(keyword? %)) {:env env :val (get env sym)}

    ;; if we run into a define form we add a kv pair to the env,
    ;; we also return the value
    [:define name val]
    (let [{_ :env val :val} (evaluate env val)]
      {:env (merge env {name val}) :val val})

    ;; if we run into a begin form we run all the forms inside it
    [:begin & forms]
    (reduce (fn [{env :env _ :val} form]
              (evaluate env form))
            {:env env :val nil} forms)

    ;; if we encounter a lambda we evaluate it into a closure
    (lambda :guard #(lambda? %)) (evaluate-lambda env lambda)

    ;; if we run into an... if we conditionally evaluate a body
    [:if condition true-body false-body]
    (evaluate-if env
                 condition
                 true-body
                 false-body)

    ;; if we run into a let body
    ;; we transform the let binding into a lambda and run that
    [:let bindings body]
    (let [[fn-body argument-list] (transform-let bindings body)
          {_ :env closure :val} (evaluate-lambda env fn-body)]
      (evaluate-function env closure argument-list))

    ;; if we run into a [:key & argument] form we treat this as a function call
    ;; we extract the function from the environment and evaluate it with its arguments
    [(func :guard #(keyword? %)) & arguments]
    (evaluate-function env (get env func) arguments)

    ;; if we find a lambda in the head position
    ;; we evaluate the lambda into a closure
    ;; then evaluate the function
    [(func :guard #(lambda? %)) & arguments]
    (let [{_ :env  closure :val} (evaluate-lambda env func)]
      (evaluate-function env closure arguments))

    ;; Our most general form of function evaluation
    ;; if we encounter an arbitrary expression in the head position
    ;; we evaluate it in hopes the user returns a function
    ;; we then evaluate the returned function with the arguments
    ;; [expr & arguments]
    ;; (let [{_ :env closure :val} (evaluate env expr)]
    ;;   (evaluate-function env closure arguments))

;; if this somehow passes all of our forms we just :noop for the time being
    :else {:noop expr}))

;; Running 
(def run (comp (partial evaluate stdenv) read))

(:env (run (slurp "lisp/lambda.lisp")))
