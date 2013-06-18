(ns mondrian.color
  (:require [mondrian.math :as math]))

(defn hsl-str
  "Returns the CSS/HTML-style color string for the given HSL color."
  [h s l]
  (str "hsl(" h "," s "%," l "%)"))

(def colors
  "A vector containing 360 HSL colors with a fixed saturation of 100%
  and a lightness of 50%."
  (let [hsls (map #(vector % 100 50) (range 0 361))
        hsl-strs (map (fn [[h s l]] (hsl-str h s l)) hsls)]
    (vec hsl-strs)))

(defn hsl-by-hue-deg
  "Returns the HSL color whose hue is at location theta (in degrees),
  with a fixed saturation of 100% and a lightness of 50%."
  [theta]
  (-> theta int colors))

(defn hsl-by-hue-rad
  "Returns the HSL color whose hue is at location theta (in radians),
  with a fixed saturation of 100% and a lightness of 50%."
  [theta]
  (-> theta math/degrees hsl-by-hue-deg))
