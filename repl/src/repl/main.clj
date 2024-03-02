(ns repl.main
  (:require
    [hyperfiddle.rcf]))

(defn init
  []
  (hyperfiddle.rcf/enable!)
  :initialized)
#_(init)
