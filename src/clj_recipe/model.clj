(ns clj-recipe.model
    (:require [clojure.spec.alpha :as s])
)

(s/def ::recipe-title (s/and 
                       string?
                       #(re-matches #"" %)
                       #(let [len (count %)] (and (< 0 count) (< count 100)))
                       ))
(s/def ::unvalidated-recipe-title string?)
(s/def ::unsanitized-markdown string?)
(s/def ::unvalidated-ingredients #(s/valid? ::unsanitized-markdown %))
(s/def ::unvalidated-instructions #(s/valid? ::unsanitized-markdown %))

(s/def ::sanitized-markdown string?)

;;would derive be better here?
(s/def ::ingredient-list #(s/valid? ::sanitized-markdown %))
(s/def ::instruction-list #(s/valid? ::sanitized-markdown %))
(s/def ::recipe-id uuid?)

(s/def ::unvalidated-recipe 
       (s/keys :req [
                     ::unvalidated-recipe-title
                     ::unvalidated-ingredients
                     ::unvalidated-instructions
                     ]))

(s/def ::validated-recipe
  (s/keys :req [::recipe-id
                ::recipe-title
                ::ingredient-list
                ::instruction-list]))

(s/def ::create-recipe-command 
       (s/key :req [
                    ::unvalidated-recipe
       ]))

(s/def ::recipe-created (s/key :req [::validated-recipe]))

(def create-recipe (s/fspec {:args ::create-recipe-command :ret ::recipe-created} ))