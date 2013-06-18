(ns mondrian.anim
  (:use [dommy.core :only [set-html!]]
        [monet.core :only [animation-frame]]))

;; ---------------------------------------------------------------------
;; Animation functions
;;

(defn start
  "Starts a new animation given the initial state and a pair of
  functions that drive the the animation.  Multiple animations can run
  concurrently on the same page, although they may be scheduled
  independently of each other.

  update-fn will be called every frame with a single argument -- the
  final state of the previous frame.  Included in that state object is a
  key, :delta-t-ms, that contains the time in milliseconds since the
  last call to update.  update-fn should return the new state.

  render-fn will be called after update-fn and takes a single argument:
  the new state returned by update-fn.

  error-fn, if provided, will be called with an error string if either
  update-fn or render-fn fails.

  Note that update-fn and render-fn are called by the browser, which may
  target a frame rate other than 60FPS, or may not call the functions at
  all if the page is not visible or the user is interacting with the
  browser.  :delta-t-ms is specifically designed to address that
  situation and allows animations to depend on the time that has passed
  rather than the frequency at which update-fn is being called."
  [init-state update-fn render-fn & [error-fn]]
  (letfn [(animation-loop [prev-ms prev-state]
            (let [now-ms (.now js/Date)
                  delta-t-ms (- now-ms prev-ms)
                  prev-state (assoc prev-state :delta-t-ms delta-t-ms)
                  next-state (atom prev-state)]
              (try
                (if-let [new-state (update-fn prev-state)]
                  (try
                    (reset! next-state new-state)
                    (render-fn @next-state)
                    (catch js/Object e
                      (if error-fn
                        (error-fn (str "render-fn failed -- " e))
                        (throw e))))
                  (throw (js/Error. "returned nil state")))
                (catch js/Object e
                  (if error-fn
                    (error-fn (str "update-fn failed -- " e))
                    (throw e)))
                (finally
                  (animation-frame #(animation-loop now-ms @next-state))))))]
    (animation-loop (.now js/Date) init-state)))


;; ---------------------------------------------------------------------
;; FPS counter
;;
;; Update pipeline: update-frame-counter -> snapshot-fps
;;
;; Render stack:    draw-fps
;;
;; State:
;;    :label -- DOM element (usually a span) whose inner HTML will be
;;      replaced on every animation frame with the current contents of
;;      the FPS value.
;;    :fps -- Snapshot of the last FPS value (updated by snapshot-fps
;;      every 1000ms).
;;    :elapsed-ms-this-sec -- Number of milliseconds that have elapsed
;;      since the frame counter was reset.
;;    :frames-this-sec -- Number of frames since the last time that
;;      :elapsed-ms-this-sec was reset.

(defn- update-frame-counter
  "Updates the frame counter and elapsed time since the last FPS
  snapshot."
  [{:keys [delta-t-ms] :as state}]
  (-> state
      (update-in [:elapsed-ms-this-sec] + delta-t-ms)
      (update-in [:frames-this-sec] inc)))

(defn- snapshot-fps
  "Copies the frame counter to the FPS value and resets the counters if
  at least 1000ms have elapsed since the last FPS snapshot.  Passes the
  state straight through otherwise."
  [{:keys [elapsed-ms-this-sec frames-this-sec] :as state}]
  (if (> elapsed-ms-this-sec 1000)
    (assoc state
           :fps (- frames-this-sec 1)
           :elapsed-ms-this-sec 0
           :frames-this-sec 0)
    state))

(defn- draw-fps
  "Updates the contents of the FPS element with the current FPS value."
  [{:keys [label fps]}]
  (set-html! label fps))

(defn ^:export add-fps
  "Given a DOM element, starts an animation loop that periodically
  updates the contents of that element with the current FPS."
  [label]
  (start {:label label :fps 0 :elapsed-ms-this-sec 0 :frames-this-sec 0}
         #(-> % update-frame-counter snapshot-fps)
         draw-fps
         #(set-html! label %)))
