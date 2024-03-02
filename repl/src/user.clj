(ns user)

;; This file intentionally doesn't require any namespaces.
;; It's to make REPL load time as quick as possible.

(defn init
  []
  (require '[repl.main])
  (let [res ((resolve 'repl.main/init))]
    (in-ns 'repl)
    res))
#_(init)
