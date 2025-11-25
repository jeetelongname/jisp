(ns dev.user
  (:require [nextjournal.clerk :as clerk]))

(def default-port 7777)

(defn serve! [{port :port}]
  (clerk/serve! {:browse? true
                 :watch-paths ["src" "notebooks" "lisp"]
                 :port port}))

(comment
  (clerk/serve! {:browse? true
                 :watch-paths ["src" "notebooks" "lisp"]
                 :port 7776})

  (clerk/show! "src/core.clj")
  (clerk/recompute!)

  (clerk/halt!))

