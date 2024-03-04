#!/usr/bin/env bb

#?(:bb (do (require '[babashka.deps])
           (babashka.deps/add-deps '{:deps {;; sorted
                                            babashka/fs {:mvn/version "0.5.20"}
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

(defn resolve-path
  [{:keys [lines-read filepath path-in-code] :as opts}]
  (let [temp-file (doto (str (fs/create-temp-file {:dir (fs/parent filepath)
                                                   :prefix (format "%s." (fs/file-name filepath))}))
                        (spit (str/join "\n" lines-read)))
        cmd (-> (str/join "" ["bash -c '"
                              "source %s >/dev/null; "
                              "printf \"%%s\" %s"
                              "'"])
                (format temp-file path-in-code))
        {:keys [exit out] :as res} (shell {:out :string} cmd)]
    (fs/delete temp-file)
    (if (= exit 0)
      out
      (throw (ex-info "path resolution failed" {:opts opts :res res})))))

(tests
  "the path SCRIPT_DIR should match the path where the script is"
  (resolve-path {:filepath (str (fs/absolutize "./test/res/test_suite/3_import_with_variables/entry.bash"))
                 :lines-read ["#!/usr/bin/env bash"
                              ""
                              "SCRIPT_DIR=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")\" && pwd)\""
                              ""]
                 :path-in-code "${SCRIPT_DIR}/lib/lib1.bash"}) :=
  (str (fs/normalize (fs/path (fs/cwd) "./test/res/test_suite/3_import_with_variables/lib/lib1.bash")))
  :tests)

(defn process-file
  [{:keys [line parent-path filename cwd top?] :or {top? true} :as opts}]
  (let [cwd' (str cwd)
        path (str (if (fs/absolute? filename) filename (fs/absolutize (fs/path cwd' filename))))
        _ (when (not (fs/exists? path))
            (throw (ex-info "file does not exist" {:cause-kw :file-does-not-exist
                                                   :path path
                                                   :parent-path parent-path})))
        content (-> (slurp path)
                    (str/split-lines))
        result (->> content
                    (reduce (fn [lines-read l]
                              (if (or (str/starts-with? l "source ") (str/starts-with? l ". "))
                                (let [[_ sourced-file] (str/split l #" ")
                                      resolved-filepath (resolve-path {:filepath path
                                                                       :lines-read lines-read
                                                                       :path-in-code sourced-file})
                                      composed (process-file {:parent-path path
                                                              :line l
                                                              :filename resolved-filepath
                                                              :cwd cwd'
                                                              :top? false})]
                                  (conj lines-read composed))
                                (conj lines-read l)))
                            [])
                    (str/join "\n"))]
    (str (when line (format "# %s # BEGIN\n" line))
         result
         "\n"
         (when line (format "# %s # END\n" line)))))

(defn process
  [{:keys [cwd entry output] :or {cwd (str (fs/cwd))} :as opts}]
  (let [cwd-absolute (if (fs/absolute? cwd) cwd (fs/absolutize cwd))
        res (process-file {:filename entry :cwd cwd-absolute})]
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

(def ^:private make-default-output
  (delay (str (fs/path (fs/cwd) "bundle.bash"))))

(def cli-pack-opts
  (delay {:spec {:cwd {:alias :c
                       :coerce :string
                       :default (str (fs/cwd))
                       :desc "Sets the working directory. Defaults to current directory."
                       :ref "<path>"}
                 :entry {:alias :i
                         :coerce :string
                         :desc "Entry point to the script being processed."
                         :ref "<path>"
                         :require true}
                 :output {:alias :o
                          :coerce :string
                          :default @make-default-output
                          :desc "Output path"
                          :ref "<path>"}
                 :help {:alias :h
                        :coerce :boolean
                        :desc "Shows this message"}}}))

(defn cli-process
  [{:keys [fn dispatch opts]}]
  (let [defaults {:output @make-default-output}
        opts' (merge defaults opts)]
    (try
      (process opts')
      (catch Exception e
        (let [{:keys [cause-kw path parent-path]} (ex-data e)]
          (case cause-kw
            :file-does-not-exist (do (println "ERROR")
                                     (println "File does not exist:" path)
                                     (println "Requested by:       " (if parent-path parent-path "input param")))
            (throw e)))))))

(defn cli-help
  [_]
  (println
    (str "pack\n"
         (cli/format-table
           {:rows (concat [["alias" "option" "ref" "default" "description"]]
                          (cli/opts->table @cli-pack-opts))
            :indent 0}))))

(def cli-table
  (delay [{:cmds ["pack"] :fn cli-process :spec (:spec @cli-pack-opts) :args->opts [:entry]}
          {:cmds [] :fn cli-help}]))

(defn -main [& args]
  (cli/dispatch @cli-table args {:error-fn
                                 (fn [{:keys [_spec type cause msg option] :as _data}]
                                   (if (= :org.babashka/cli type)
                                     (case cause
                                       :require
                                       (do (println (format "Missing required argument: %s\n" option))
                                           (cli-help nil)
                                           (System/exit 1))
                                       :validate
                                       (println
                                         (format "%s does not exist!\n" msg)))))}))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
