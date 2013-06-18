(ns mondrian.ui
  (:require [mondrian.dom :as dom])
  (:use [dommy.attrs :only [attr has-class? px set-attr! set-px!]]
        [dommy.core :only [add-class! append! closest hide! set-html!]])
  (:use-macros [dommy.macros :only [by-id node sel sel1]]))

;; ---------------------------------------------------------------------
;; Canvas functions
;;

(defn setup-canvas
  "Adjusts the canvas's height to ensure that the drawing surface
  matches the given aspect-ratio.  Also applies scaling as necessary to
  ensure 1:1 drawing on HiDPI screens.  Returns the final width and
  height of the canvas."
  [canvas ctx aspect-ratio]
  (let [canvas-width (px canvas :width)
        new-canvas-height (quot canvas-width aspect-ratio)
        dpr (or (.-devicePixelRatio js/window) 1)]

    ;; Adjust the canvas height based on the desired aspect ratio.  All
    ;; browsers will properly set the width and many will also preserve
    ;; the canvas's original aspect ratio by setting the element height.
    ;; Not all browsers will do so however, and in any event the caller
    ;; might want to use a different aspect ratio than was configured by
    ;; the HTML/CSS.
    (set-px! canvas :height new-canvas-height)

    ;; Set the canvas's width and height in order to lock in the new
    ;; aspect ratio.  We always have to do this, even for browsers that
    ;; properly scale the canvas element, because at best the browser
    ;; only set the element size and not the size of the drawing
    ;; surface.  We also need to take the device pixel ratio into
    ;; account here in order to properly support HiDPI screens.
    (set-attr! canvas :width (* dpr canvas-width))
    (set-attr! canvas :height (* dpr new-canvas-height))

    ;; The final task in supporting HiDPI screens is to scale the canvas
    ;; according to the device pixel ratio.
    (.scale ctx dpr dpr)

    ;; Return the width and height of the drawing surface.
    [canvas-width new-canvas-height]))


;; ---------------------------------------------------------------------
;; Control functions
;;

(defn data-values
  "Returns a map containing all of the HTML5 data attributes for the
  given element that begin with value-*; the values are identified by
  Clojure-style hyphenated keyword names, not JavaScript-style
  camelCase."
  [elem]
  (into {} (for [[k v] (dom/data elem)
                 :when (re-matches #"^value-.*" (name k))]
             [(-> k name (subs 6) keyword) v])))

(defn- get-checkbox-values
  "Returns a map containing all of the checkbox values found under the
  given DOM element."
  [elem]
  (let [checkboxes (sel elem "input[type=checkbox]")
        checkbox-kvs (map #(vector (-> % .-name keyword)
                                   (-> % .-checked))
                          checkboxes)]
    (into {} checkbox-kvs)))

(defn- get-radio-values
  "Returns a map containing all of the radio button values found under
  the given DOM element."
  [elem]
  (let [checked-buttons (sel elem ".radio.control.mondrian-ready input:checked")
        checked-button-kvs (map #(vector (-> % .-name keyword)
                                         (-> % dom/data :value dom/jsval->clj))
                                checked-buttons)]
    (into {} checked-button-kvs)))

(defn- get-slider-values
  "Returns a map containing all of the slider values found under the
  given DOM element."
  [elem]
  (let [sliders (sel elem :.slider.control.mondrian-ready)
        slider-data (map dom/data sliders)
        slider-kvs (map #(vector (-> % :name keyword)
                                 (-> % :value))
                        slider-data)]
    (into {} slider-kvs)))

(defn get-control-values
  "Returns a map containing all of the control values found under the
  given DOM element."
  [elem]
  (merge (get-checkbox-values elem)
         (get-radio-values elem)
         (get-slider-values elem)))

(defn- choices
  "Returns a dommy body vector containing all of the DOM elements for
  the given radio button config.  default-value contains the default
  value for the radio button group and is used to enable the default
  choice."
  [config default-value]
  (apply concat (for [i (iterate inc 1)
                      :let [id (str "choice_" (gensym))
                            label (-> (str "choice" i "-label") keyword config)
                            value (-> (str "choice" i "-value") keyword config)
                            checked? (= value default-value)]
                      :while (not= nil label)]
                  [[:input (merge {:type "radio" :id id :name (config :name)
                                   :data-value (str value)}
                                  (if checked? {:checked "checked"}))]
                   [:label {:for id} label]])))

(defn make-radiogroup!
  "Turns the given element into a radio button group, creating radio
  buttons for all of the choices found under the element.  init-values
  contains the initial values for all of the controls and is used to set
  the default choice."
  [elem init-values]
  (let [config (dom/data elem)
        radiogroup-value-kw (-> config :name keyword)
        radiogroup-value (init-values radiogroup-value-kw)]
    (-> elem
        (append! [:span.label (config :label)])
        (append! [:form.wrapper (choices config radiogroup-value)])
        (append! [:span.value])))
  (-> (sel1 elem :.wrapper) js/jQuery .buttonset)
  (add-class! elem "mondrian-ready"))

(defn- to-jquery-slider!
  "Turns DOM element slider into a jQuery UI slider with the given min,
  max, current value, and optional step (which defaults to 1 if not
  provided).  on-value-change will be called whenever the value of the
  slider changes."
  [slider {:keys [min max step] :or {:step 1}} value on-value-change]
  (let [jq-slider (js/jQuery slider)]
    (.slider jq-slider (js-obj "value" value
                               "min" min
                               "max" max
                               "step" step
                               "slide" #(on-value-change (.-value %2))))))

(defn make-slider!
  "Turns the given element into a slider."
  [elem init-values]
  (let [config (dom/data elem)
        slider-value-kw (keyword (config :name))
        slider-value (init-values slider-value-kw)]
    (-> elem
        (append! [:span.label (config :label)])
        (append! [:.wrapper :div])
        (append! [:span.value]))
    (let [slider-elem (sel1 elem ".wrapper div")
          value-elem (sel1 elem :.value)
          on-value-change (fn [value]
                            (set-attr! elem :data-value (str value))
                            (set-html! value-elem value))]
      (to-jquery-slider! slider-elem config slider-value on-value-change)
      (on-value-change slider-value)))
  (add-class! elem "mondrian-ready"))

(defn- toggles
  "Returns a dommy body vector containing all of the DOM elements for
  the given toggle group config.  init-values contains the initial
  values for all of the controls and is used to set the enabled state
  for the toggle(s)."
  [config init-values]
  (apply concat (for [i (iterate inc 1)
                      :let [id (str "button_" (gensym))
                            name (-> (str "button" i "-name") keyword config)
                            label (-> (str "button" i "-label") keyword config)
                            checked? (get init-values (keyword name))]
                      :while (not= nil name)]
                  [[:input (merge {:type "checkbox" :id id :name name}
                                  (if checked? {:checked "checked"}))]
                   [:label {:for id} label]])))

(defn make-togglegroup!
  "Turns the given element into a toggle button group."
  [elem init-values]
  (let [config (dom/data elem)]
    (-> elem
        (append! [:span.label (config :label)])
        (append! [:.wrapper (toggles config init-values)])
        (append! [:span.value])))
  (doseq [button (sel elem ".wrapper input")]
    (.button (js/jQuery button)))
  (add-class! elem "mondrian-ready"))

(defn make-control!
  "Adds inner elements to control in order to turn it into a jQuery UI
  control.  The control type is taken from the control's class and the
  configuration values for the control from the data-* elements on the
  element.  init-values contains the initial values for all of the
  controls and is used to enable/provide default values for/etc. the
  controls."
  [elem init-values]
  (cond
    (has-class? elem "radio") (make-radiogroup! elem init-values)
    (has-class? elem "slider") (make-slider! elem init-values)
    (has-class? elem "togglegroup") (make-togglegroup! elem init-values)))

(defn update-controls
  "Finds all controls in the given mondrian DOM element, generates DOM
  elements for any newly-found controls, and returns the value of all of
  the controls."
  [elem]
  (let [init-values (data-values elem)]
    (doseq [control (sel elem ".control:not(.mondrian-ready)")]
      (make-control! control init-values))
    (merge init-values (get-control-values elem))))
