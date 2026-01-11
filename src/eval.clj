(ns eval
  (:require
   [clojure.core.match :refer [match]]
   [clojure.set :as set]
   [nextjournal.clerk :as clerk]

   read)
  (:refer-clojure :rename {eval core-eval}))

;; # Evaluating
;; Evaluating is the task of walking the datastructure we have and returning a value
;; In this case we have to define the evaluation rules for certain forms.
;; every rule must return two things, an environment, which contains a mapping of names to values
;; and the value of the form itself after evaluation.
(declare eval)

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
(defn eval-lambda [env lambda]
  (let [lambda-symbols (set (flatten lambda))
        env-names (set (keys env))
        capture (select-keys env (set/intersection lambda-symbols env-names))]
    {:env env
     :val {:lambda lambda
           :capture capture}}))

(defn eval-function [env func arguments]
  (let [arguments (mapv (comp :val (partial eval env)) arguments)]
    (if (closure? func)
      (let [{capture :capture [_ arglist body] :lambda} func
            lambda-env (merge env capture (zipmap arglist arguments))]
        (merge
         (eval lambda-env body)
         {:env env}))
      {:env env :val (apply func arguments)})))

(defn eval-if [env condition true-body false-body]
  (eval env (if (:val (eval env condition))
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

(defn eval [env expr]
  (match expr
    ;; if we encounter a quoted form we return it unevaluated this is the lisp
    ;; way to produce lists and cements the fact that the representation of the
    ;; language is its own list type
    [:quote form] {:env env :val form}

    ;; if the expr is a primitive then we return the value unchanged
    (prim :guard #(read/primitive? %)) {:env env :val prim}

    ;; if the expr is a keyword we get the corresponding value
    (sym :guard #(keyword? %)) {:env env :val (get env sym)}

    ;; if we run into a define form we add a kv pair to the env,
    ;; we also return the value
    [:define name val]
    (let [{_ :env val :val} (eval env val)]
      {:env (merge env {name val}) :val val})

    ;; if we run into a begin form we run all the forms inside it
    [:begin & forms]
    (reduce (fn [{env :env _ :val} form]
              (eval env form))
            {:env env :val nil} forms)

    ;; if we run into an... if we conditionally evaluate a body
    [:if condition true-body false-body]
    (eval-if env
             condition
             true-body
             false-body)

    ;; if we encounter a lambda we evaluate it into a closure
    (lambda :guard #(read/lambda? %)) (eval-lambda env lambda)

    ;; if we run into a let body
    ;; we transform the let binding into a lambda and run that
    [:let bindings body]
    (let [[fn-body argument-list] (transform-let bindings body)
          {_ :env closure :val} (eval-lambda env fn-body)]
      (eval-function env closure argument-list))

    ;; if we run into a [:key & argument] form we treat this as a function call
    ;; we extract the function from the environment and evaluate it with its arguments
    [(func :guard #(keyword? %)) & arguments]
    (eval-function env (get env func) arguments)

    ;; if we find a lambda in the head position
    ;; we evaluate the lambda into a closure
    ;; then evaluate the function
    [(func :guard #(read/lambda? %)) & arguments]
    (let [{_ :env  closure :val} (eval-lambda env func)]
      (eval-function env closure arguments))

    [(func :guard #(closure? %)) & arguments]
    (eval-function env func arguments)

    [(func :guard #(fn? %)) & arguments]
    (eval-function env func arguments)

    ;; Our most general form of function evaluation
    ;; if we encounter an arbitrary expression in the head position
    ;; we evaluate it in hopes the user returns a function
    ;; we then evaluate the returned function with the arguments
    [func-expr & arguments]
    ;; evaluate the head form
    (let [{_ :env result :val} (eval env func-expr)]
           ;; check if the result is a callable form
      (if (or (keyword? result)
              (fn? result)
              (closure? result))
        (eval env (into [result] arguments))
        (throw (Exception.
                (str "form: "
                     func-expr
                     " does not evaluate to callable. result: "
                     result
                     " "
                     (type result))))))
    ;;   ;; if this somehow passes all of our forms we just :noop for the time being
    :else) {:noop expr})

(clerk/comment
  (match (read/read "((if true + -) 20 30)")
    [expr & arguments] {:expr expr :args arguments}
    :else :noop))
