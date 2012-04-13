(ns dieter.test.asset.hamlcoffee
  (:use dieter.asset.hamlcoffee)
  (:use clojure.test)
  (:use dieter.test.helpers)
  (:use dieter.asset)
  (:require dieter.asset.javascript)
  (:require [clojure.java.io :as io]))

(defn wrap [name output]
  "The hamlcoffee compiler wraps the code, so we need to wrap our expected output"
  (str "(function() {\n  var _ref;\n  if ((_ref = window.HAML) == null) {\n    window.HAML = {};\n  }\n  window.HAML['"
       name
       "'] = function(context) {\n    return (function() {\n      var $o;\n      $o = [];\n      $o.push(\""
       output
       "\");\n      return $o.join(\"\\n\").replace(/\\s(\\w+)='true'/mg, ' $1').replace(/\\s(\\w+)='false'/mg, '');\n    }).call(context);\n  };\n}).call(this);\n"))

(deftest test-preprocess-hamlcoffee
  (testing "basic hamlc file"
    (is (= (wrap "basic"  "<!DOCTYPE html>\\n<html>\\n  <head>\\n    <title>\\n      Title\\n    </title>\\n  </head>\\n  <body>\\n    <h1>\\n      Header\\n    </h1>\\n  </body>\\n</html>")
           (preprocess-hamlcoffee
            (io/file "test/fixtures/assets/javascripts/basic.hamlc"))))))

(deftest test-hamlcoffee-record
  (testing "read-asset returns a Js asset"
    (asset-compiles-to
     (map->HamlCoffee {:file (io/file "test/fixtures/assets/javascripts/basic.hamlc")})
     dieter.asset.javascript.Js)))

  ;; (testing "file with surround and succeed"
  ;;   (is (= "TODO"
  ;;          (preprocess-hamlcoffee
  ;;           (io/file "test/fixtures/assets/javascripts/with-partials.hamlc")))))

;; (testing "file with coffeescript"
  ;;   (is (= "TODO"
  ;;          (preprocess-hamlcoffee
  ;;           (io/file "test/fixtures/assets/javascripts/with-coffee.hamlc")))))

  ;; (testing "bad haml syntax"
  ;;   (is (has-text?
  ;;        (preprocess-hamlcoffee
  ;;         (io/file "test/fixtures/assets/javascripts/badhaml1.hamlc"))
  ;;        "ERROR: Syntax Error on line 1"))

  ;;   (is (has-text?
  ;;        (preprocess-hamlcoffee
  ;;         (io/file "test/fixtures/assets/javascripts/badhaml2.hamlc"))
  ;;        "@import \"includeme.less\"")))

  ;; (testing "bad coffee syntax"
  ;;   (is (has-text?
  ;;        (preprocess-hamlcoffee
  ;;         (io/file "test/fixtures/assets/javascripts/badcoffee.hamlc"))
  ;;        "@import \"includeme.less\""))))