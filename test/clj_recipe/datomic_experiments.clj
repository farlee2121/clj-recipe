(ns clj-recipe.datomic-experiments
  (:require [datomic.client.api :as d]))


(def client (d/client {:server-type :dev-local
                       :system "recipe-experiments"}))

(def db-name "recipes")
(d/create-database client {:db-name db-name})

(def conn (d/connect client {:db-name db-name}))

(def recipe-schema [{:db/ident :recipe/id
                     :db/valueType :db.type/uuid
                     :db/cardinality :db.cardinality/one
                     :db/unique :db.unique/identity
                     :db/doc "The year the movie was released in theaters"}

                    {:db/ident :recipe/title
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/doc "The recipe title"}

                    {:db/ident :recipe/instructions
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/doc "A sanitized markdown string of recipe instructions"}

                    {:db/ident :recipe/ingredients
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one
                     :db/doc "A sanitized markdown string of recipe ingredients"}])

(defn create-db []
  (d/transact conn {:tx-data recipe-schema}))




(def seed-data [{:recipe/id (java.util.UUID/randomUUID)
                 :recipe/title "Do the thing"
                 :recipe/instructions "- pour it \n - mix it \n - cook it \n - done"
                 :recipe/ingredients "- 4 food \n - 1 stuff \n - 1/2 yogurt"}])


(defn seed-database []
  (d/transact conn {:tx-data seed-data}))

(defn get-all []
  ;;https://docs.datomic.com/on-prem/query/indexes.html#EAVT
  (d/index-pull (d/db conn) {:index :avet
                             :selector '[*]
                             :start [:recipe/id]})
  ;;(d/q '[:find *] conn)
  )

(defn get-all-ids []
  ;;https://docs.datomic.com/on-prem/query/indexes.html#EAVT
  (d/index-pull (d/db conn) {:index :avet
                             :selector '[:recipe/id]
                             :start [:recipe/id]})
  ;;(d/q '[:find *] conn)
  )

(defn get-by-id [id]
  ;; for getting an entity when you have a unique identifier
  ;;https://stackoverflow.com/questions/14189647/get-all-fields-from-a-datomic-entity
  (d/pull (d/db conn) '[*] [:recipe/id id]))

(defn upsert [recipe]
  (d/transact conn {:tx-data [recipe]})
  )

(defn gen-recipe 
  ([] )
  ([title] )
  )

;; todo
;; - write a query on recipe
;; - update single property?
;; - join?

;;https://docs.datomic.com/on-prem/query/query.html
;;
;;
;;(d/q {:query '[:find ?movie-genre
;;          :where [_ :movie/genre ?movie-genre]]
;; :timeout 1
;; :args [db]})