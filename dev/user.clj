(ns dev.user
  (:require [nextjournal.clerk :as clerk]))

(def default-port 7777)

(def serve!ops {:browse? true
                :watch-paths ["src" "notebooks" "lisp"]
                :port 7776})

(defn serve! [ops]
  (println "Ran")
  (clerk/serve! (merge serve!ops ops)))

(comment
  (clerk/serve! serve!ops)

  (clerk/show! "src/core.clj")
  (clerk/recompute!)

  (clerk/halt!))

