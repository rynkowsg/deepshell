#!/usr/bin/env bb

#?(:bb (do (require '[babashka.deps])
           (babashka.deps/add-deps '{:deps {;; sorted
                                            babashka/fs {:mvn/version "0.2.12"}
                                            babashka/process {:mvn/version "0.5.22"}
                                            com.hyperfiddle/rcf {:mvn/version "20220926-202227"}
                                            org.babashka/cli {:mvn/version "0.8.57"}
                                            org.clojure/clojure {:mvn/version "1.11.1"}}})))

(ns pl.rynkowski.shellpack
  (:require
    [babashka.cli :as cli]
    [babashka.fs :as fs]
    [babashka.process :refer [shell]]
    [clojure.string :as str]
    [hyperfiddle.rcf :refer [tests]]))

;; ---------- CORE --------------------

(defn evaluate-path
  [{:keys [source-script path-in-code] :as opts}]
  (let [cmd (-> (str/join "" ["bash -c '"
                              "if [ -f \"%s\" ]; then "
                              "source %s >/dev/null; "
                              "else "
                              "exit 54; "
                              "fi; "
                              "printf \"%%s\" %s"
                              "'"])
                (format source-script source-script path-in-code))
        {:keys [exit out] :as res} (shell {:out :string} cmd)]
    (if (= exit 0)
      out
      (throw (ex-info "path resolution failed" {:opts opts :res res})))))

(tests
  "the path SCRIPT_DIR should match the path where the script is"
  (evaluate-path {:source-script "./test/res/test_suite/3_import_with_variables/entry.bash"
                  :path-in-code "${SCRIPT_DIR}/lib/lib1.bash"}) :=
  (str (fs/normalize (fs/path (fs/cwd) "./test/res/test_suite/3_import_with_variables/lib/lib1.bash")))
  :tests)

(defn process-file
  [{:keys [line filename cwd top?] :or {top? true} :as opts}]
  (let [path (str (if (fs/absolute? filename)
                    filename
                    (fs/absolutize (fs/path cwd filename))))
        content (-> (slurp path)
                    (str/split-lines))
        result (->> content
                    (map (fn [l]
                           (if (str/starts-with? l "source")
                             (let [[_ sourced-file] (str/split l #" ")
                                   resolved-filepath (evaluate-path {:source-script path :path-in-code sourced-file})]
                               (process-file {:line l
                                              :filename resolved-filepath
                                              :cwd cwd
                                              :top? false}))
                             l)))
                    (str/join "\n"))]
    (str (when line (format "# %s # BEGIN\n" line))
         result
         "\n"
         (when line (format "# %s # END\n" line)))))

(defn process
  [{:keys [cwd entry output] :as opts :or {cwd (str (fs/cwd))}}]
  (let [res (process-file {:filename entry :cwd cwd})]
    (fs/create-dirs (fs/parent output))
    (spit output res)
    res))

(tests
  (def test-case "./test/res/test_suite/0_nothing")
  (def test-case "./test/res/test_suite/1_import_two_files")
  (def test-case "./test/res/test_suite/2_import_nested_files")
  (def test-case "./test/res/test_suite/3_import_with_variables")
  (process {:cwd test-case :entry "entry.bash" :output (str test-case "/output.bash")})
  (slurp (str test-case "/output.bash")) := (slurp (str test-case "/expected.bash"))
  :tests-end)

;; ---------- MAIN --------------------

(def cli-options {:cwd {:coerce :string}
                  :entry {:coerce :string}
                  :output {:coerce :string}
                  :help {:coerce :boolean}})

(defn -main [& _args]
  (let [opts (cli/parse-opts *command-line-args* {:spec cli-options})]
    (prn opts)
    (process opts)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
