(ns clj-recipe.model
    (:require [clojure.spec.alpha :as s])
)

(defmacro spec-derive [spec-key]
    `(s/with-gen 
     #(s/valid? ~spec-key %)
     #(s/gen ~spec-key)
      )
)

(s/def ::recipe-title (s/and 
                       string?
                       #(re-matches #"[\w ]+" %)
                       #(let [len (count %)] (and (< 0 len) (< len 100)))
                       ))
(s/def ::unvalidated-recipe-title string?)
(s/def ::unsanitized-markdown string?)
(s/def ::unvalidated-ingredients  (spec-derive ::unsanitized-markdown))
(s/def ::unvalidated-instructions (spec-derive ::unsanitized-markdown))

(s/def ::sanitized-markdown string?)

;;would derive be better here?
(s/def ::ingredient-list (spec-derive ::sanitized-markdown))
(s/def ::instruction-list (spec-derive ::sanitized-markdown))
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
       (s/keys :req [
                    ::unvalidated-recipe
       ]))
(s/def ::recipe-created (s/keys :req [::validated-recipe]))

;; (def create-recipe (s/fspec {:args ::create-recipe-command :ret ::recipe-created} ))
(s/fdef create-recipe 
        :args ::create-recipe-command 
        :ret ::recipe-created)

(s/def ::str-pred (s/fspec :args string? :ret boolean?))

(s/valid? ::str-pred (fn [s] true))
;; (s/fdef delete-recipe
;;   :args ::create-recipe-command
;;   :ret ::recipe-created)