#!/usr/bin/env bb

#?(:bb (do (require '[babashka.deps])
           (babashka.deps/add-deps '{:deps {;; sorted
                                            babashka/fs {:mvn/version "0.5.20"}
                                            babashka/process {:mvn/version "0.5.22"}
                                            http-kit/http-kit {:mvn/version "2.7.0"}
                                            org.babashka/cli {:mvn/version "0.8.57"}
                                            org.clojure/clojure {:mvn/version "1.11.1"}
                                            pl.rynkowski.clj-gr/lang {:git/url "https://github.com/rynkowsg/clj-gr.git" :git/sha "5ee201de40df7b07b8979adab9cfdbcb1d8bf9e6" :deps/root "lib/lang"}
                                            #_:deps}})))

(ns pl.rynkowski.shellpack
  (:require
    [babashka.cli :as cli]
    [babashka.fs :as fs]
    [babashka.process :refer [shell]]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [org.httpkit.client :as hk-client]
    [pl.rynkowski.clj-gr.lang.coll :refer [some-insight]]
    [pl.rynkowski.clj-gr.lang.map :refer [vals->keys]]
    [pl.rynkowski.clj-gr.lang.regex :refer [named-groups]])
  (:import
    (java.net URL)))

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

^:rct/test
(comment #_((requiring-resolve 'com.mjdowney.rich-comment-tests/run-ns-tests!) *ns*)
  "the path SCRIPT_DIR should match the path where the script is"
  (resolve-path {:filepath (str (fs/absolutize "./test/res/test_suite/3_import_with_variables/entry.bash"))
                 :lines-read ["#!/usr/bin/env bash"
                              "SCRIPT_DIR=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")\" && pwd)\""]
                 :path-in-code "${SCRIPT_DIR}/lib/lib1.bash"})
  ;=>> (str (fs/normalize (fs/path (fs/cwd) "./test/res/test_suite/3_import_with_variables/lib/lib1.bash")))
  :comment)

(def remote->regex
  (delay (let [dir "(?<dir>[^@]*)"
               path "(?<path>.*)"]
           {:github (re-pattern (str "^" dir "/" "(?<type>@github)/(?<user>[^@/]*)/(?<name>[^@/]*)(@(?<ref>[^/]*))?" "/" path "$"))
            :https (re-pattern (str "^" dir "/" "(?<type>@https)" "/" path "$"))})))
(def regex->remote (delay (vals->keys @remote->regex)))

(defn assess-source-path
  "having source-path, if it looks like a dep, composes url to fetch it"
  [source-path]
  (let [local {:path source-path}
        remote (when-let [{:keys [el res]} (some-insight #(named-groups (val %) source-path) @remote->regex)]
                 (case (-> el first)
                   :github (let [{:keys [dir type user name ref path]} res]
                             {:url (format "https://raw.githubusercontent.com/%s/%s/%s/%s" user name (or ref "HEAD") path)})
                   :https (let [{:keys [dir type path]} res]
                            {:url (format "https://%s" path)})
                   nil))]
    (merge local remote)))

^:rct/test
(comment #_((requiring-resolve 'com.mjdowney.rich-comment-tests/run-ns-tests!) *ns*)
  (assess-source-path "./lib/@github/rynkowsg/shell-gr@main/lib/trap.bash")
  ;=> {:path "./lib/@github/rynkowsg/shell-gr@main/lib/trap.bash" :url "https://raw.githubusercontent.com/rynkowsg/shell-gr/main/lib/trap.bash"}
  (assess-source-path "./lib/@https/raw.githubusercontent.com/rynkowsg/shell-gr/main/lib/trap.bash")
  ;=> {:path "./lib/@https/raw.githubusercontent.com/rynkowsg/shell-gr/main/lib/trap.bash" :url "https://raw.githubusercontent.com/rynkowsg/shell-gr/main/lib/trap.bash"}
  (assess-source-path "./lib/trap.bash")
  ;=> {:path "./lib/trap.bash"}
  (assess-source-path "./lib/@github/rynkowsg/shell-gr/lib/trap.bash")
  ;=> {:path "./lib/@github/rynkowsg/shell-gr/lib/trap.bash" :url "https://raw.githubusercontent.com/rynkowsg/shell-gr/HEAD/lib/trap.bash"}
  :comment)

(defn download-file [url path]
  (println "downloading:" url "->" path)
  (let [{:keys [body status] :as _res} @(hk-client/request {:method :get :url url})]
    (if (= status 200)
      (do (fs/create-dirs (fs/parent path))
          (fs/create-file path)
          (spit path body))
      (throw (ex-info "remote file not available" {:cause-kw :remote-file-not-available :url url :path path})))))

(defn process-fetch-file
  [{:keys [debug? parent-path filename cwd top?] :or {top? true} :as opts}]
  (when debug? (println {:fn :process-fetch-file :opts opts}))
  (let [cwd' (str cwd)
        path (str (if (fs/absolute? filename) filename (fs/absolutize (fs/path cwd' filename))))
        _ (if (fs/exists? path)
            (when debug? (println {:fn :process-fetch-file :msg :file-exists :path path}))
            (throw (ex-info "file under processing does not exist" {:cause-kw :file-under-processing-does-not-exist
                                                                    :path path
                                                                    :cwd cwd'
                                                                    :parent-path parent-path})))
        content (-> (slurp path)
                    (str/split-lines))]
    (->> content
         (reduce (fn [lines-read l]
                   (if (or (str/starts-with? l "source ") (str/starts-with? l ". "))
                     ;; if source, download if necessary and go deeper
                     (let [[_ sourced-file] (str/split l #" ")
                           resolved-filepath (resolve-path {:filepath path
                                                            :lines-read lines-read
                                                            :path-in-code sourced-file})
                           _ (when debug? (println {:fn :process-fetch-file :resolved-filepath resolved-filepath}))
                           {:keys [path url]} (assess-source-path resolved-filepath)]
                       (let [exists? (some-> path fs/exists?)]
                         (cond
                           (and (some? url) (not exists?)) (download-file url path)
                           exists? (println "File already exists:" path)
                           (not exists?) (do (println "File doesn't exist")
                                             (throw (ex-info "source local file does not exist" {:cause-kw :source-local-file-does-not-exist
                                                                                                 :path path
                                                                                                 :cwd cwd'
                                                                                                 :parent-path parent-path})))))
                       (let [sourced-file-lines (process-fetch-file {:parent-path (str (fs/parent path))
                                                                     :filename resolved-filepath
                                                                     :cwd cwd'
                                                                     :top? false})]
                         (into lines-read sourced-file-lines)))
                     ;; otherwise just add a line
                     (conj lines-read l)))
                 []))))

(defn process-fetch
  [{:keys [debug? cwd entry] :or {cwd (str (fs/cwd))} :as opts}]
  (let [cwd-absolute (if (fs/absolute? cwd) cwd (fs/absolutize cwd))]
    (process-fetch-file {:debug? debug? :filename entry :cwd cwd-absolute :parent-path (str (fs/parent entry))})))

(comment
  (def test-case "./test/res/test_suite/3_import_with_variables")
  (process-fetch {:cwd test-case :entry "entry.bash" :output (str test-case "/output.bash")})
  (def test-case2 "./test/res/test_suite/4_import_remote")
  (process-fetch {:cwd test-case2 :entry "entry.bash" :output (str test-case2 "/output.bash")})
  :comment)

(defn process-pack-file
  [{:keys [debug? line parent-path filename cwd top?] :or {top? true} :as opts}]
  (when debug? (println {:fn :process-pack-file :cwd cwd :filename filename}))
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
                                      composed (process-pack-file {:parent-path path
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
         (when line (format "# %s # END" line)))))

(defn process-pack
  [{:keys [cwd entry output] :or {cwd (str (fs/cwd))} :as opts}]
  (let [cwd-absolute (if (fs/absolute? cwd) cwd (fs/absolutize cwd))
        res (process-pack-file {:filename entry :cwd cwd-absolute})]
    (fs/create-dirs (fs/parent output))
    (spit output res)
    res))

(comment
  (def test-case "./test/res/test_suite/0_nothing")
  (def test-case "./test/res/test_suite/1_import_two_files")
  (def test-case "./test/res/test_suite/2_import_nested_files")
  (def test-case "./test/res/test_suite/3_import_with_variables")
  (process-pack {:cwd test-case :entry "entry.bash" :output (str test-case "/output.bash")})
  (slurp (str test-case "/output.bash")) := (slurp (str test-case "/expected.bash"))
  :tests-end)

;; ---------- MAIN --------------------

(def cli-fetch-opts
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
                 :debug? {:alias :d
                          :coerce :boolean
                          :desc "Shows debug logs"}
                 :help {:alias :h
                        :coerce :boolean
                        :desc "Shows this message"}}}))

(defn cli-fetch
  [{:keys [fn dispatch opts]}]
  (let [defaults {}
        opts' (merge defaults opts)]
    (try
      (process-fetch opts')
      (catch Exception e
        (let [{:keys [cause-kw path parent-path] :as data} (ex-data e)]
          (case cause-kw
            :file-under-processing-does-not-exist (do (println "ERROR")
                                                      (println "File does not exist:" path)
                                                      (println "Requested by:       " (if parent-path parent-path "input param"))
                                                      (System/exit 11))
            :source-local-file-does-not-exist (do (println "File does not exist:" path)
                                                  (println "Requested by:       " (if parent-path parent-path "input param"))
                                                  (System/exit 12))
            :remote-file-not-available (do (println "ERROR")
                                           (println "Remote file does not exist")
                                           (pprint (dissoc data :cause-kw))
                                           (System/exit 13))
            :file-does-not-exist (do (println "File does not exist:" path)
                                     (println "Requested by:       " (if parent-path parent-path "input param"))
                                     (System/exit 1))
            (throw e)))))))

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
                 :debug? {:alias :d
                          :coerce :boolean
                          :default false
                          :desc "Shows debug logs"}
                 :help {:alias :h
                        :coerce :boolean
                        :desc "Shows this message"}}}))

(defn cli-pack
  [{:keys [fn dispatch opts]}]
  (let [defaults {:output @make-default-output}
        opts' (merge defaults opts)]
    (try
      (process-pack opts')
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
    "MODES:"
    "\n\n"
    (str "fetch\n"
         (cli/format-table
           {:rows (concat [["alias" "option" "ref" "default" "description"]]
                          (cli/opts->table @cli-fetch-opts))
            :indent 0}))
    "\n\n"
    (str "pack\n"
         (cli/format-table
           {:rows (concat [["alias" "option" "ref" "default" "description"]]
                          (cli/opts->table @cli-pack-opts))
            :indent 0}))))

(def cli-table
  (delay [{:cmds ["pack"] :fn cli-pack :spec (:spec @cli-pack-opts) :args->opts [:entry]}
          {:cmds ["fetch"] :fn cli-fetch :spec (:spec @cli-fetch-opts) :args->opts [:entry]}
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
