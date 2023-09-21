(ns clj-recipe.order-example
  (:require [datomic.client.api :as d]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [clj-recipe.model :as model]))


(def client (d/client {:server-type :dev-local
                       :system "order-example"}))

(defn new-id [] (java.util.UUID/randomUUID))

(def db-name "orders")
(d/create-database client {:db-name db-name})

(def conn (d/connect client {:db-name db-name}))

(def orders-schema [;; Order
                    {:db/ident :order/id ;; <- Think of this like the field name
                     :db/valueType :db.type/uuid ;; <- this is the field type
                     :db/cardinality :db.cardinality/one ;; <- Can the entity have one, or many values in this field
                     :db/unique :db.unique/identity}

                    {:db/ident :order/delivery-address
                     :db/valueType :db.type/string
                     :db/cardinality :db.cardinality/one}

                    {:db/ident :order/order-items
                     :db/valueType :db.type/ref ;; <- a reference to another type of entity
                     :db/isComponent true
                     :db/cardinality :db.cardinality/many}
                    
                    ;; Order Item
                    {:db/ident :order-item/item-id
                     :db/valueType :db.type/uuid
                     :db/cardinality :db.cardinality/one}
                    {:db/ident :order-item/quantity
                     :db/valueType :db.type/long
                     :db/cardinality :db.cardinality/one}])
                    

(defn create-db []
  (d/transact conn {:tx-data orders-schema}))




(def seed-data [{:order/id (java.util.UUID/randomUUID)
                 :order/delivery-address "1600 Pennsylvania Ave NW, Washington DC 20500"
                 :order/order-items
                 [{:order-item/item-id (java.util.UUID/randomUUID)
                   :order-item/quantity 5}
                  {:order-item/item-id (java.util.UUID/randomUUID)
                   :order-item/quantity 5}]}])      
                 


(defn view-schema []
  (d/index-pull (d/db conn) {:index :avet
                             :selector '[*]
                             :start [:db/ident]}))

(defn seed-database []
  (d/transact conn {:tx-data seed-data}))

(defn get-all []
  ;;https://docs.datomic.com/on-prem/query/indexes.html#EAVT
  (d/index-pull (d/db conn) {:index :avet
                             :selector '[*]
                             :start [:order/id]}))
  ;;(d/q '[:find *] conn)
  

(defn get-all-ids []
  ;;https://docs.datomic.com/on-prem/query/indexes.html#EAVT
  (d/index-pull (d/db conn) {:index :avet
                             :selector '[:order/id]
                             :start [:order/id]}))
  ;;(d/q '[:find *] conn)
  

(defn get-by-id [id]
  ;; for getting an entity when you have a unique identifier
  ;;https://stackoverflow.com/questions/14189647/get-all-fields-from-a-datomic-entity
  (d/pull (d/db conn) '[*] [:order/id id]))

(defn upsert [recipe]
  (d/transact conn {:tx-data [recipe]}))

;; (defn gen-recipe
;;   ([]  (let [recipe (gen/generate (s/gen ::model/validated-recipe))]
;;          {:recipe/id (new-id)
;;           :recipe/title (recipe ::model/recipe-title)
;;           :recipe/instructions (recipe ::model/instruction-list)
;;           :recipe/ingredients (recipe ::model/ingredient-list)}))
;;   ([title] (assoc (gen-recipe) :recipe/title title)))


(defn get-title [title]
  (d/q '[:find ?e 
         :in $ ?title
         :where [?e :recipe/title ?title]]
       (d/db conn)
       title))




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