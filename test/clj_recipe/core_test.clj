(ns clj-recipe.core-test
  (:require [clojure.test :refer :all]
            [clj-recipe.core :refer :all]
            [clj-recipe.model :as m]))

;; approaches to test api. 1. use a protocol like this 2. 
(defprotocol TestApi
  (create-recipe [this unvalidated-recipe])
  (update-recipe [this recipe-id unvalidated-recipe])
  (delete-recipe [this recipe-id])
  (get-recipe [this recipe-id])
  (list-recipes [this])
  )


(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

;; tests
;; - javascript removed from markdown
;; - can get any valid recipe I create
;; - test title limitations (invalid chars, min and max chars)
;; - 