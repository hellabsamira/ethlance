(ns ethlance.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.personal :as web3-personal]
    [cljs.spec :as s]
    [day8.re-frame.async-flow-fx]
    [day8.re-frame.http-fx]
    [ethlance.db :refer [default-db]]
    [ethlance.ethlance-db :as ethlance-db :refer [get-entity get-entity-list]]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [goog.string.format]
    [madvas.re-frame.google-analytics-fx]
    [madvas.re-frame.web3-fx]
    [ethlance.generate-db]
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path trim-v after debug reg-fx console dispatch]]
    [medley.core :as medley]))

#_(if goog.DEBUG
    (require '[ethlance.generate-db]))

(re-frame-storage/reg-co-fx! :ethlance {:fx :localstorage :cofx :localstorage})

(def interceptors [;check-spec-interceptor
                   ;(when ^boolean goog.DEBUG debug)
                   trim-v])

(defn contract-xhrio [contract-name code-type on-success on-failure]
  {:method :get
   :uri (gstring/format "./contracts/build/%s.%s?_=%s" contract-name (name code-type) (.getTime (new js/Date)))
   :timeout 6000
   :response-format (if (= code-type :abi) (ajax/json-response-format) (ajax/text-response-format))
   :on-success on-success
   :on-failure on-failure})

(def max-gas 4700000)

(defn get-contract [db key]
  (get-in db [:contracts key]))

(defn get-instance [db key]
  (get-in db [:contracts key :instance]))

(defn storage-keys [& args]
  (apply web3-eth/contract-call (get-instance @re-frame.db/app-db :ethlance-db) :storage-keys args))

(comment
  (dispatch [:initialize])
  (dispatch [:print-db])
  (dispatch [:deploy-contracts])
  (dispatch [:estimate-contracts])
  (dispatch [:clean-localstorage true])
  (dispatch [:print-localstorage])
  (dispatch [:contract.config/set-configs])
  (dispatch [:contract.db/add-allowed-contracts])
  (dispatch [:contract/call :ethlance-db :get-allowed-contracts])
  (dispatch [:contract/call :ethlance-db :allowed-contracts-keys 5])
  (dispatch [:contract/call :ethlance-user :get-config "max-user-languages"])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :user/count)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :job/count)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :category/jobs-count 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :skill/jobs-count 5)])
  (dispatch [:contract/call :ethlance-db :get-address-value (u/sha3 :user/address 1)])
  (dispatch [:contract/call :ethlance-db :get-bytes32-value (u/sha3 :user/name 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :freelancer/hourly-rate 1)])
  (dispatch [:contract/call :ethlance-db :get-string-value (u/sha3 :freelancer/description 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (storage-keys 6)])
  (get-entity 1 [:user/address :user/gravatar :user/name :user/country :user/status :user/freelancer?
                 :freelancer/available? :user/employer? :freelancer/job-title
                 :freelancer/hourly-rate :employer/description :freelancer/skills-count :freelancer/categories-count]
              (get-in @re-frame.db/app-db [:contracts :ethlance-db :instance]))

  (get-entity 1 [:job/employer
                 :job/title
                 :job/language
                 :job/budget
                 :job/created-on
                 :job/category
                 :job/payment-type
                 :job/experience-level
                 :job/estimated-duration
                 :job/hours-per-week
                 :job/freelancers-needed
                 :job/status
                 :job/skills-count
                 :job/proposals-count
                 :job/contracts-count
                 :job/invitations-count
                 :job/hiring-done-on
                 :job/total-paid]
              (get-in @re-frame.db/app-db [:contracts :ethlance-db :instance]))

  (get-entity 1 [:freelancer/job-title :user/name :user/gravatar]
              (get-in @re-frame.db/app-db [:contracts :ethlance-db :instance]))

  (get-entity-list [1] [:user/name :user/country :user/freelancer?]
              (get-in @re-frame.db/app-db [:contracts :ethlance-db :instance]))

  (dispatch [:contract.user/register-freelancer {:user/name "Mataaa"
                                                 :user/gravatar "abc"
                                                 :user/country 1
                                                 :user/languages [1]
                                                 :freelancer/available? true
                                                 :freelancer/job-title "Cljs dev"
                                                 :freelancer/hourly-rate 10
                                                 :freelancer/categories [1 2]
                                                 :freelancer/skills [3 4 5]
                                                 :freelancer/description "asdasdasd" #_(doall (reduce str (range 100)))}])

  (dispatch [:contract.user/register-employer {:user/name "Mataaa"
                                               :user/gravatar "abc"
                                               :user/country 1
                                               :user/languages [1]
                                               :employer/description "employdescribptions"}])

  (dispatch [:contract.user/search-freelancers {:search/category 1
                                                :search/skills [3]
                                                :search/min-avg-rating 0
                                                :search/min-contracts-count 0
                                                :search/min-hourly-rate 0
                                                :search/max-hourly-rate 1
                                                :search/country 0
                                                :search/language 0
                                                :search/offset 0
                                                :search/limit 10}])

  (dispatch [:contract.job/add {:job/title "This is Job 1"
                                :job/description "Asdkaas  aspokd aps asopdk ap"
                                :job/skills [3 4 5]
                                :job/budget 10
                                :job/language 1
                                :job/category 1
                                :job/payment-type 1
                                :job/experience-level 1
                                :job/estimated-duration 1
                                :job/hours-per-week 1
                                :job/freelancers-needed 2}])

  (dispatch [:contract.user/search-jobs {:search/category 0
                                         :search/skills []
                                         :search/payment-types []
                                         :search/experience-levels []
                                         :search/estimated-durations []
                                         :search/hours-per-weeks []
                                         :search/min-budget 0
                                         :search/min-employer-avg-rating 0
                                         :search/country 0
                                         :search/language 0
                                         :search/offset 0
                                         :search/limit 10}])
  (dispatch [:contract/state-call :ethlance-user :test-db 5])
  )
(reg-event-fx
  :initialize
  (inject-cofx :localstorage)
  (fn [{:keys [localstorage]} [deploy-contracts?]]
    (let [{:keys [web3 provides-web3?]} default-db]
      (.clear js/console)
      (merge
        {:db (merge-with (partial merge-with merge) default-db localstorage)
         :async-flow {:first-dispatch [:load-contracts]
                      :rules [{:when :seen? :events [:contracts-loaded :blockchain/my-addresses-loaded]
                               :dispatch [:boot-success]}]}}
        (when provides-web3?
          {:web3-fx.blockchain/fns
           {:web3 web3
            :fns [[web3-eth/accounts :blockchain/my-addresses-loaded :blockchain/on-error]]}})))))

(reg-event-fx
  :load-contracts
  interceptors
  (fn [{:keys [db]}]
    {:http-xhrio
     (for [[key {:keys [name]}] (:contracts db)]
       (for [code-type (if goog.DEBUG [:abi :bin] [:abi])]
         (contract-xhrio name code-type [:contract/loaded key code-type] [:log-error])))}))

(reg-event-fx
  :deploy-contracts
  interceptors
  (fn [{:keys [db]}]
    (let [ethance-db (get-in db [:contracts :ethlance-db])]
      {:web3-fx.blockchain/fns
       {:web3 (:web3 db)
        :fns [[web3-eth/contract-new
               (:abi ethance-db)
               {:gas max-gas
                :data (:bin ethance-db)
                :from (:active-address db)}
               :contract/deployed-ethlance-db
               :log-error]]}})))

(reg-event-fx
  :contract/deployed-ethlance-db
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [instance]]
    (when-let [db-address (aget instance "address")]
      (console :log :ethlance-db " deployed at " db-address)
      {:db (update-in db [:contracts :ethlance-db] merge {:address db-address :instance instance})
       :localstorage (assoc-in localstorage [:contracts :ethlance-db] {:address db-address})
       :web3-fx.blockchain/fns
       {:web3 (:web3 db)
        :fns (for [[key {:keys [abi bin]}] (dissoc (:contracts db) :ethlance-db)]
               [web3-eth/contract-new
                abi
                db-address
                {:gas max-gas
                 :data bin
                 :from (:active-address db)}
                [:contract/deployed key]
                [:log-error key]])}})))

(reg-event-fx
  :contract/deployed
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [key instance]]
    (when-let [contract-address (aget instance "address")]
      (console :log key " deployed at " contract-address)
      (merge
        {:db (update-in db [:contracts key] merge {:address contract-address
                                                   :instance instance})
         :localstorage (assoc-in localstorage [:contracts key] {:address contract-address})}
        (when (:setter? (get-contract db key))
          {:dispatch [:contract.db/add-allowed-contracts [key]]})))))

(reg-event-fx
  :estimate-contracts
  interceptors
  (fn [{:keys [db]}]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns (for [[key {:keys [abi bin]}] (:contracts db)]
             [web3-eth/estimate-gas
              {:data bin}
              [:contract/estimate-gas-result key]
              [:log-error key]])}}))

(reg-event-fx
  :contract/estimate-gas-result
  interceptors
  (fn [{:keys [db]} [key result]]
    (console :log key result)
    {}))

(reg-event-fx
  :set-active-page
  interceptors
  (fn [{:keys [db]} [match]]
    {:db (assoc db :active-page match)}))

(reg-event-fx
  :set-active-address
  interceptors
  (fn [{:keys [db]} [address]]
    {:db (assoc db :active-address address)}))

(reg-event-fx
  :blockchain/my-addresses-loaded
  interceptors
  (fn [{:keys [db]} [addresses]]
    (let [addresses-map (reduce #(assoc %1 %2 {:address %2}) {} addresses)]
      (merge
        {:db (-> db
               (assoc :my-addresses addresses)
               (assoc :active-address (first addresses))
               (update :accounts merge addresses-map))
         :web3-fx.blockchain/balances
         {:web3 (:web3 db)
          :watch? true
          :blockchain-filter-opts "latest"
          :db-path [:blockchain :balances]
          :addresses addresses
          :dispatches [:blockchain/balance-loaded :blockchain/on-error]}}))))

(reg-event-db
  :blockchain/balance-loaded
  interceptors
  (fn [db [balance address]]
    (assoc-in db [:accounts address :balance] balance)))

(reg-event-fx
  :contract/loaded
  interceptors
  (fn [{:keys [db]} [contract-key code-type code]]
    (let [code (if (= code-type :abi) (clj->js code) code)]
      (let [new-db (cond-> db
                     true
                     (assoc-in [:contracts contract-key code-type] code)

                     (= code-type :abi)
                     (assoc-in [:contracts contract-key :instance]
                               (when-let [address (:address (get-contract db contract-key))]
                                 (web3-eth/contract-at (:web3 db) code address))))]
        (merge
          {:db new-db}
          (when (every? #(and (:abi %) (:bin %)) (vals (:contracts new-db)))
            {:dispatch [:contracts-loaded]}))))))

(reg-event-fx
  :contracts-loaded
  interceptors
  (fn [{:keys [db]}]
    ))

(reg-event-fx
  :boot-success
  interceptors
  (fn [{:keys [db]}]
    (when (some nil? (map :address (vals (:contracts db))))
      {:dispatch [:deploy-contracts]})))

(reg-event-fx
  :contract.config/set-configs
  interceptors
  (fn [{:keys [db]}]
    (let [{:keys [ethlance-config-values web3 active-address]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract.config/set-configs]
        :fns [[(get-instance db :ethlance-config)
               :set-configs (keys ethlance-config-values) (vals ethlance-config-values)
               {:gas max-gas
                :from active-address}
               :contract/transaction-sent
               :contract/transaction-error
               [:contract/transaction-receipt :set-configs max-gas :generate-db false]]]}})))

(reg-event-fx
  :contract.db/add-allowed-contracts
  interceptors
  (fn [{:keys [db]} [contract-keys]]
    (let [contract-keys (if-not contract-keys (keys (medley/filter-vals :setter? (:contracts db)))
                                              contract-keys)]
      (let [{:keys [web3 active-address contracts]} db]
        {:web3-fx.contract/state-fns
         {:web3 web3
          :db-path [:contract.db/add-allowed-contracts]
          :fns [[(get-instance db :ethlance-db)
                 :add-allowed-contracts
                 (map :address (vals (select-keys contracts contract-keys)))
                 {:gas max-gas
                  :from active-address}
                 :contract/transaction-sent
                 :contract/transaction-error
                 (if (contains? (set contract-keys) :ethlance-config)
                   :contract.config/set-configs
                   :do-nothing)
                 ;[:contract/transaction-receipt max-gas false false]
                 ]]}}))))

(reg-event-fx
  :contract.user/register-freelancer
  interceptors
  (fn [{:keys [db]} [values]]
    (let [{:keys [web3 active-address contracts]} db
          args ((apply juxt ethlance-db/register-freelancer-args) values)]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract.user/register-freelancer]
        :fns [(concat
                [(get-instance db :ethlance-user)
                 :register-freelancer]
                (remove nil? args)
                [{:gas max-gas
                  :from active-address}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :register-freelancer max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.user/register-employer
  interceptors
  (fn [{:keys [db]} [values update?]]
    (let [{:keys [web3 active-address contracts]} db
          args ((apply juxt ethlance-db/register-employer-args) values)
          method (if update? :set-employer :register-employer)]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract.user/register-employer]
        :fns [(concat
                [(get-instance db :ethlance-user)
                 method]
                (remove nil? args)
                [{:gas max-gas
                  :from active-address}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt method max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.user/search-freelancers
  interceptors
  (fn [{:keys [db]} [values]]
    (let [{:keys [web3 active-address contracts]} db
          args ((apply juxt ethlance-db/search-freelancers-args) values)]
      {:web3-fx.contract/constant-fns
       {:fns [(concat
                [(get-instance db :ethlance-search)
                 :search-freelancers]
                args
                [:log :log-error])]}})))

(reg-event-fx
  :contract.user/search-jobs
  interceptors
  (fn [{:keys [db]} [values]]
    (let [{:keys [web3 active-address contracts]} db
          args ((apply juxt ethlance-db/search-jobs-args) values)]
      {:web3-fx.contract/constant-fns
       {:fns [(concat
                [(get-instance db :ethlance-search)
                 :search-jobs]
                args
                [:log :log-error])]}})))

(reg-event-fx
  :contract.job/add
  interceptors
  (fn [{:keys [db]} [values]]
    (let [{:keys [web3 active-address contracts]} db
          args (into [] ((apply juxt ethlance-db/add-job-args) values))
          nested-args ((apply juxt ethlance-db/add-job-nested-args) values)]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract.job/add]
        :fns [(concat
                [(get-instance db :ethlance-job)
                 :add-job]
                (conj args nested-args)
                [{:gas max-gas
                  :from active-address}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :add-job max-gas false :log-error]])]}})))

(reg-event-fx
  :contract/call
  interceptors
  (fn [{:keys [db]} [contract-key & args]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat [(get-instance db contract-key)] args [:log :log-error])]}}))

(reg-event-fx
  :contract/state-call
  interceptors
  (fn [{:keys [db]} [contract-key method & args]]
    {:web3-fx.contract/state-fns
     {:web3 (:web3 db)
      :db-path [:contract/state-call]
      :fns [(concat [(get-instance db contract-key)
                     method]
                    args
                    [{:gas max-gas
                      :from (:active-address db)}
                     :contract/transaction-sent
                     :contract/transaction-error
                     [:contract/transaction-receipt method max-gas nil nil]])]}}))

(reg-event-fx
  :contract/transaction-sent
  interceptors
  (fn [_ [tx-hash]]
    #_ (console :log tx-hash)))

(reg-event-fx
  :contract/transaction-error
  interceptors
  (fn [_ [error]]
    (console :error error)))

(reg-event-fx
  :contract/transaction-receipt
  interceptors
  (fn [_ [event-name gas-limit on-success on-out-of-gas {:keys [gas-used] :as receipt}]]
    (let [gas-used-percent (* (/ gas-used gas-limit) 100)]
      (console :log event-name (gstring/format "%.2f%" gas-used-percent) "gas used:" gas-used)
      (if (<= gas-limit gas-used)
        (when on-out-of-gas
          {:dispatch [on-out-of-gas receipt]})
        (when on-success
          {:dispatch [on-success receipt]})))))

(reg-event-fx
  :clean-localstorage
  interceptors
  (fn [_ [initialize?]]
    (merge
      {:localstorage nil}
      (when initialize?
        {:dispatch [:initialize]}))))

(reg-event-db
  :print-db
  interceptors
  (fn [db]
    (print.foo/look db)
    db))

(reg-event-fx
  :print-localstorage
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]}]
    (print.foo/look localstorage)
    nil))

(reg-event-fx
  :log
  interceptors
  (fn [db result]
    (apply console :log (map #(if (aget % "toNumber") (.toNumber %) %) result))))

(reg-event-fx
  :do-nothing
  interceptors
  (fn [db result]
    ))

(reg-event-db
  :log-error
  interceptors
  (fn [db errors]
    (apply console :error errors)
    db))
