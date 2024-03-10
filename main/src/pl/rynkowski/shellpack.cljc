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

;(defn- log [& data]
;  (spit "/tmp/debug-log.txt" (apply str (conj data "\n")) :append true))

(defn- log [& data]
  (apply println data))

(defn resolve-path
  [{:keys [debug? lines-read script-path path-in-code] :as opts}]
  (when debug? (log {:fn :resolve-path :opts (dissoc opts :lines-read)}))
  (let [temp-file (doto (str (fs/create-temp-file {:dir (fs/parent script-path)
                                                   :prefix (format "%s.%s." (fs/file-name script-path) (str (System/currentTimeMillis)))}))
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
  (resolve-path {:script-path (str (fs/absolutize "./test/res/test_suite/3_import_with_variables/entry.bash"))
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

(defn download-file
  [{:keys [debug? url path] :as params}]
  (when debug? (log {:fn :download-file :msg :enter :params params}))
  (let [{:keys [body status] :as _res} @(hk-client/request {:method :get :url url})]
    (println url)
    (if (= status 200)
      (do (fs/create-dirs (fs/parent path))
          (fs/create-file path)
          (spit path body))
      (throw (ex-info "remote file not available" {:cause-kw :remote-file-not-available :url url :path path})))))

(defn process-fetch-file
  [{:keys [debug? lines-read-before path chain cwd] :or {lines-read-before []} :as opts}]
  (when debug? (log {:fn :process-fetch-file :msg :enter :opts (dissoc opts :lines-read-before)}))
  (let [content (-> (slurp path)
                    (str/split-lines))
        res (->> content
                 (reduce (fn [lines-read l]
                           (if (not (or (str/starts-with? l "source ") (str/starts-with? l ". ")))
                             ;; if not a source line, just add a line
                             (conj (vec lines-read) l)
                             ;; if source, download if necessary and go deeper
                             (let [[_ file-to-source] (str/split l #" ")
                                   ;; resolve variable within path by evaluating everything read until this point
                                   resolved-source-path (resolve-path {:debug? debug?
                                                                       :lines-read lines-read
                                                                       :path-in-code file-to-source
                                                                       :script-path path})
                                   _ (when debug? (log {:fn :process-fetch-file :msg :resolved-source-path :resolved resolved-source-path}))
                                   ;; normalize the path
                                   normalized-source-path (str (if (fs/absolute? resolved-source-path) resolved-source-path (fs/normalize (fs/path cwd resolved-source-path))))
                                   _ (when debug? (log {:fn :process-fetch-file :msg :normalized-source-path :data normalized-source-path}))
                                   ;; assess whether it is a downloadable source path
                                   {url :url assessed-path :path :as assessed} (assess-source-path normalized-source-path)
                                   _ (when debug? (log {:fn :process-fetch-file :msg :assessed-source-path :data assessed}))
                                   exists? (some-> assessed-path fs/exists?)
                                   ;; download
                                   _ (cond
                                       (and (some? url) (not exists?)) (download-file {:debug? debug? :path assessed-path :url url})
                                       exists? (when debug? (log {:fn :process-fetch-file :msg :source-file-exists :path assessed-path}))
                                       (not exists?) (throw (ex-info "source local file does not exist" {:cause-kw :source-local-file-does-not-exist
                                                                                                         :path assessed-path
                                                                                                         :chain chain
                                                                                                         :cwd cwd})))
                                   lines (process-fetch-file {:debug? debug?
                                                              :lines-read-before lines-read
                                                              :path assessed-path
                                                              :chain (conj chain assessed-path)
                                                              :cwd cwd})]
                               lines))
                           #_:fn)
                         lines-read-before))]
    res)
  #_:process-fetch-file)

(defn process-fetch
  [{:keys [debug? cwd entry] :or {cwd (str (fs/cwd))} :as opts}]
  (when debug? (log {:fn :process-fetch :msg :enter :opts opts}))
  (let [cwd-absolute (if (fs/absolute? cwd) cwd (fs/absolutize cwd))
        path (str (if (fs/absolute? entry) entry (fs/absolutize (fs/path cwd entry))))
        _ (if (fs/exists? path)
            (when debug? (log {:fn :process-pack :msg :file-exists :path path}))
            (throw (ex-info "file does not exist" {:cause-kw :file-does-not-exist :path path})))]
    (process-fetch-file {:debug? debug?
                         :path path
                         :cwd cwd-absolute
                         :chain [path]
                         :lines-read-before []})))

(comment
  (def test-case "./test/res/test_suite/3_import_with_variables")
  (process-fetch {:cwd test-case :entry "entry.bash" :output (str test-case "/output.bash")})
  (def test-case2 "./test/res/test_suite/4_import_remote")
  (process-fetch {:cwd test-case2 :entry "entry.bash" :output (str test-case2 "/output.bash")})
  :comment)

(defn process-pack-file
  [{:keys [debug? lines-read-before line path chain cwd top?] :as opts}]
  (when debug? (log {:fn :process-pack-file :opts (dissoc opts :lines-read-before)}))
  (let [content (-> (slurp path)
                    (str/split-lines))
        res (->> content
                 (reduce (fn [{:keys [lines-read lines-read-all] :as acc} l]
                           (if (not (or (str/starts-with? l "source ") (str/starts-with? l ". ")))
                             ;; if not a source line, just add a line
                             {:lines-read (conj (vec lines-read) l)
                              :lines-read-all (conj (vec lines-read-all) l)}
                             ;; if source, download if necessary and go deeper
                             (let [[_ file-to-source] (str/split l #" ")
                                   ;; resolve variable within path by evaluating everything read until this point
                                   resolved-source-path (resolve-path {:debug? debug?
                                                                       :lines-read lines-read-all
                                                                       :path-in-code file-to-source
                                                                       :script-path path})
                                   _ (when debug? (log {:fn :process-pack-file :msg :resolved-source-path :resolved resolved-source-path}))
                                   ;; normalize the path
                                   normalized-source-path (str (if (fs/absolute? resolved-source-path) resolved-source-path (fs/normalize (fs/path cwd resolved-source-path))))
                                   _ (when debug? (log {:fn :process-pack-file :msg :normalized-source-path :data normalized-source-path}))
                                   res (process-pack-file {:debug? debug?
                                                           :line l
                                                           :lines-read-before lines-read-all
                                                           :path normalized-source-path
                                                           :chain (conj chain normalized-source-path)
                                                           :cwd cwd})]
                               {:lines-read (vec (concat lines-read (:lines-read res)))
                                :lines-read-all (:lines-read-all res)}))
                           #_:fn)
                         {:lines-read []
                          :lines-read-all lines-read-before}))]
    (let [pr [[(when line (format "# %s # BEGIN" line))]
              (:lines-read res)
              [(when line (format "# %s # END" line))]]]
      {:lines-read (->> pr
                        (flatten)
                        (filter some?)
                        (vec))
       :lines-read-all (:lines-read-all res)})))

(defn process-pack
  [opts]
  (let [{:keys [debug? cwd entry output] :as opts'} (merge {:cwd (str (fs/cwd))} opts)]
    (when debug? (log {:fn :process-pack :msg :enter :opts opts'}))
    (let [cwd-absolute (if (fs/absolute? cwd) cwd (fs/absolutize cwd))
          path (-> (if (fs/absolute? entry) entry (fs/absolutize (fs/path cwd entry))) (fs/normalize) (str))
          _ (if (fs/exists? path)
              (when debug? (log {:fn :process-pack :msg :file-exists :path path}))
              (throw (ex-info "file does not exist" {:cause-kw :file-does-not-exist :path path})))
          {:keys [lines-read] :as pack-file-res} (process-pack-file {:debug? debug?
                                                                     :path path
                                                                     :cwd cwd-absolute
                                                                     :top? true
                                                                     :chain [path]
                                                                     :lines-read-before []})]
      (fs/create-dirs (fs/parent output))
      (let [output-str (format "%s\n" (str/join "\n" lines-read))
            res (spit output output-str)]
        res))))

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
            :source-local-file-does-not-exist (do (println "ERROR")
                                                  (println "File does not exist:" path)
                                                  (println "Requested by:       " (if parent-path parent-path "input param"))
                                                  (pprint (dissoc data :cause-kw))
                                                  (System/exit 12))
            :remote-file-not-available (do (println "ERROR")
                                           (println "Remote file does not exist")
                                           (pprint (dissoc data :cause-kw))
                                           (System/exit 13))
            :file-does-not-exist (do (println "ERROR")
                                     (println "File does not exist:" path)
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
            (do (println "OHER ERROR")
                (println (ex-data e))
                (throw e))))))))

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
