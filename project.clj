(defproject clj-recipe "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
                 [org.clojure/clojure "1.10.1"]  
                 [com.datomic/dev-local "0.9.225"]
                 [org.clojure/test.check "1.1.0"]
                 ]
  :repositories [
                 ["cognitect-dev-tools" {:url      "https://dev-tools.cognitect.com/maven/releases/"
                                         :username :env/datomic_username
                                         :password :env/datomic_password}]]
  :repl-options {:init-ns clj-recipe.core})
