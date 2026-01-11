;; # Implementing a Toy Lisp in Clojure
(ns core
  (:require
   read eval
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

;; Running
(defn run
  ([]
   (run stdenv))
  ([env] (comp (partial #'eval/eval env) read/read)))

(clerk/comment
  (read/read "((hofn 10) 20 30)")
  (read/read "((if true + -) 20 30)")
  ((run (:env
         ((run) " (begin (define hofn (lambda (y) (lambda (x z) (+ x y z)))) (define test (lambda () (if true + -))))")))
   "((if false 10 30) 20 30)")

  (:val (run (slurp "lisp/lambda.lisp"))))
