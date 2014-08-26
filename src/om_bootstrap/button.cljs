(ns om-bootstrap.button
  "Bootstrap buttons!"
  (:require [om.core :as om]
            [om-bootstrap.mixins :as m]
            [om-bootstrap.types :as t]
            [om-bootstrap.util :as u]
            [om-tools.core :refer-macros [defcomponentk]]
            [om-tools.dom :as d :include-macros true]
            [om-tools.mixin :refer-macros [defmixin]]
            [schema.core :as s])
  (:require-macros [schema.macros :as sm]))

;; ## Basic Button

(def Button
  (t/bootstrap
   {:active? s/Bool
    :disabled? s/Bool
    :block? s/Bool
    :nav-item? s/Bool
    :nav-dropdown? s/Bool}))

(def ButtonGroup
  (t/bootstrap
   {:vertical? s/Bool
    :justified? s/Bool}))

;; ## Code

(sm/defn render-anchor
  [opts :- {:classes {s/Any s/Any}
            :disabled? s/Bool
            :props {s/Any s/Any}}
   children]
  (let [props {:href (-> opts :props (:href "#"))
               :class (d/class-set (assoc (:classes opts)
                                     :disabled (:disabled? opts)))
               :role "button"}]
    (d/a (u/merge-props props (:props opts))
         children)))

(sm/defn button :- t/Component
  "Renders a button."
  [props :- Button & children]
  (let [[bs props] (t/separate Button props {:bs-class "button"
                                             :bs-style "default"
                                             :type "button"})
        klasses (if (:nav-dropdown? bs)
                  {}
                  (t/bs-class-set bs))
        klasses (merge klasses
                       {:active (:active? bs)
                        :btn-block (:block? bs)})]
    (cond
     (:nav-item? bs) (d/li {:class (d/class-set {:active (:active? bs)})}
                           (render-anchor {:props props
                                           :disabled? (:disabled? bs)
                                           :classes klasses}
                                          children))
     (or (:href props)
         (:nav-dropdown? bs))
     (render-anchor {:props props
                     :disabled? (:disabled? bs)
                     :classes klasses}
                    children)
     :else (d/button (u/merge-props props {:class (d/class-set klasses)
                                           :disabled (:disabled? bs)})
                     children))))

;; ## Button Toolbar

(sm/defn toolbar :- t/Component
  "Renders a button toolbar."
  [opts & children]
  (let [[bs props] (t/separate {} opts {:bs-class "button-toolbar"})]
    (d/div {:role "toolbar"
            :class (d/class-set (t/bs-class-set bs))}
           children)))

;; ## Button Group

(sm/defn button-group :- t/Component
  "Renders the supplied children in a wrapping button-group div."
  [opts :- ButtonGroup & children]
  (let [[bs props] (t/separate ButtonGroup opts {:bs-class "button-group"})
        classes (merge (t/bs-class-set bs)
                       {:btn-group (not (:vertical? bs))
                        :btn-group-vertical (:vertical? bs)
                        :btn-group-justified (:justified? bs)})]
    (d/div (u/merge-props props {:class (d/class-set classes)})
           children)))

;; ## Dropdown Button

(def DropdownButton
  (t/bootstrap
   {(s/optional-key :title) t/Renderable
    (s/optional-key :href) s/Str
    (s/optional-key :on-click) (sm/=> s/Any s/Any)
    (s/optional-key :on-select) (sm/=> s/Any s/Any)
    (s/optional-key :pull-right?) s/Bool
    (s/optional-key :dropup?) s/Bool
    (s/optional-key :nav-item?) s/Bool}))

(defn render-nav-item [props open? children]
  (let [classes {:dropdown true
                 :open open?
                 :dropup (:dropup? props)}]
    (d/li {:class (d/class-set classes)}
          children)))

(defn render-button-group [props open? children]
  (let [group-classes {:open open?
                       :dropup (:dropup? props)}]
    (button-group {:bs-size (:bs-size props)
                   :class (d/class-set group-classes)}
                  children)))

;; ## Dropdown Button

(def MenuItem
  (t/bootstrap
   {:key s/Str
    (s/optional-key :header?) s/Bool
    (s/optional-key :divider?) s/Bool
    (s/optional-key :href) s/Str
    (s/optional-key :title) s/Str
    (s/optional-key :on-select) (sm/=> s/Any s/Any)}))

(sm/defn menu-item :- t/Component
  [opts :- MenuItem & children]
  (let [[bs props] (t/separate MenuItem opts {:href "#"})
        classes {:dropdown-header (:header? bs)
                 :divider (:divider? bs)}
        handle-click (fn [e]
                       (when-let [on-select (:on-select bs)]
                         (.preventDefault e)
                         (on-select (:key bs))))
        children (if (:header? bs)
                   children
                   (d/a {:on-click handle-click
                         :href (:href bs)
                         :title (:title bs)
                         :tab-index "-1"}
                        children))]
    (d/li (u/merge-props props {:role "presentation"
                                :key (:key bs)
                                :class (d/class-set classes)})
          children)))

(def DropdownMenu
  (t/bootstrap
   {(s/optional-key :pull-right?) s/Bool
    (s/optional-key :on-select) (sm/=> s/Any s/Any)}))

(sm/defn dropdown-menu
  [opts :- DropdownMenu & children]
  (let [[bs props] (t/separate DropdownMenu opts)
        classes {:dropdown-menu true
                 :dropdown-menu-right (:pull-right? bs)}]
    (d/ul (u/merge-props props {:class (d/class-set classes)
                                :role "menu"})
          (if-let [on-select (:on-select bs)]
            (map #(u/clone-with-props % {:on-select on-select}) children)
            children))))

(defcomponentk dropdown* [owner state]
  (:mixins m/dropdown-mixin)
  (render
   [_]
   (let [open? ((aget owner "isDropdownOpen"))
         {:keys [opts children]} (om/get-props owner)
         [bs props] (t/separate DropdownButton opts {:href "#"})
         set-dropdown (aget owner "setDropdownState")
         render-fn (partial (if (:nav-item? bs)
                              render-nav-item
                              render-button-group)
                            bs open?)]
     (render-fn
      [(button
        (u/merge-props (dissoc opts :nav-item? :title :pull-right? :dropup?)
                       {:ref "dropdownButton"
                        :class "dropdown-toggle"
                        :key 0
                        :nav-dropdown? (:nav-item? bs)
                        :on-click (fn [e]
                                    (.preventDefault e)
                                    (set-dropdown (not open?)))})
        (:title bs) " " (d/span {:class "caret"}))
       (dropdown-menu
        {:ref "menu"
         :aria-labelledby (:id props)
         :pull-right? (:pull-right? bs)
         :key 1}
        (map
         #(u/clone-with-props
           % (fn [props]
               (let [handle (when (or (:on-select (:opts props))
                                      (:on-select bs))
                              (fn [key]
                                (if-let [os (:on-select bs)]
                                  (os key)
                                  (set-dropdown false))))]
                 (update-in props [:opts]
                            u/merge-props
                            {:on-select handle}))))
         children))]))))

(sm/defn dropdown
  [opts :- DropdownButton & children]
  (->dropdown* {:opts opts
                :children children}))
