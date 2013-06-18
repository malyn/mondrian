(ns mondrian.canvas
  (:use [monet.canvas :only [begin-path move-to line-to stroke translate]]))

(defn translate-to-center
  "Moves the drawing origin to the center of the canvas."
  [ctx]
  (let [dpr (or (.-devicePixelRatio js/window) 1)
        w (-> ctx .-canvas .-width (/ dpr))
        h (-> ctx .-canvas .-height (/ dpr))
        cent-x (quot w 2)
        cent-y (quot h 2)]
    (translate ctx cent-x cent-y)))

(defn stroke-circle
  "Draws the outline of a circle centered at (x,y) with radius r."
  [ctx {:keys [x y r]}]
  (begin-path ctx)
  (. ctx (arc x y r 0 (* 2 Math/PI) true))
  (stroke ctx)
  ctx)

(defn stroke-line
  "Draws a line from (x1,y1) to (x2,y2)"
  [ctx x1 y1 x2 y2]
  (-> ctx
      begin-path
      (move-to x1 y1)
      (line-to x2 y2)
      stroke))
