(ns dev.user
  (:require [nextjournal.clerk :as clerk]))

(comment
  (clerk/serve! {:browse? true
                 :watch-paths ["src" "notebooks" "resources"]
                 :port 7776})

  (clerk/show! "src/core.clj")
  (clerk/recompute!)

  (clerk/halt!))

