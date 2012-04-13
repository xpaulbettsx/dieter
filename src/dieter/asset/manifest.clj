(ns dieter.asset.manifest
  (:use
   [dieter.asset :only [read-asset get-asset make-asset]]
   [dieter.path :only [search-dir find-file]]
   [dieter.util :only [slurp-into string-builder]])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as s])
  (:import
   [java.io FileReader PushbackReader]))

(defn load-manifest
  "a manifest file must be a valid clojure data structure,
namely a vector or list of file names or directory paths."
  [file]
  (let [stream (PushbackReader. (FileReader. file))]
    (read stream)))

(defn distinct-by
  "Returns a lazy sequence of the elements of coll with duplicates removed.
Duplicates are found by comparing the results of the comparison fn.
Implementation stolen from clojure.core/distinct"
  [fun coll]
    (let [step (fn step [xs seen]
                   (lazy-seq
                    ((fn [[f :as xs] seen]
                      (when-let [s (seq xs)]
                        (if (contains? seen (fun f))
                          (recur (rest s) seen)
                          (cons f (step (rest s) (conj seen (fun f)))))))
                     xs seen)))]
      (step coll #{})))

(defn manifest-files
  "return a sequence of files specified by the given manifest.
Duplicates are included only once, the first time they are referenced.
Files not found are not returned and no error is indicated.
We should probably consider outputting some kind of warning in that case."
  [manifest-file]
  (distinct-by #(.getCanonicalPath %)
               (filter #(and (not (nil? %))
                             (not (.isDirectory %)))
                       (flatten
                        (map (fn [filename]
                               (if (re-matches #".*/$" filename)
                                 (file-seq (search-dir filename (.getParentFile manifest-file)))
                                 (find-file filename (.getParentFile manifest-file))))
                             (load-manifest manifest-file))))))

(defrecord Dieter [file last-modified composed-of]
  dieter.asset.Asset
  (read-asset [this options]
    (let [builder (string-builder)
          target-name (s/replace (:file this) #".dieter$" "")
          ;; set result to an asset of the proper type
          ;; for example app.js.dieter => Js
          ;; with file pointing to the .dieter file
          result (make-asset (io/file target-name) {:file file})
          assets  (map #(get-asset % options)
                       (manifest-files (:file this)))]
      (doseq [asset assets]
        (.append builder (:content asset)))

      ;; return final asset which is composed of this dieter asset
      ;; this asset is in turn composed of all concatenated assets
      (assoc result
        :content builder
        :last-modified (.lastModified (:file this))
        :composed-of [(assoc this
                        :last-modified (.lastModified (:file this))
                        :composed-of assets)]))))
