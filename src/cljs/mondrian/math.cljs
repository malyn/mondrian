(ns mondrian.math)

;; ---------------------------------------------------------------------
;; Basic functions
;;

(defn degrees
  "Returns the angle rad in degrees."
  [rad]
  (* rad (/ 180 Math/PI)))

(defn radians
  "Returns the angle deg in radians."
  [deg]
  (* (/ deg 180) Math/PI))


;; ---------------------------------------------------------------------
;; Parametric equations
;;

(defn circle-x
  "Returns the x location at angle t (in radians) on the circumference
  of the circle with radius r."
  [r t]
  (* r (Math/cos t)))

(defn circle-y
  "Returns the y location at angle t (in radians) on the circumference
  of the circle with radius r."
  [r t]
  (* r (Math/sin t)))

(defn epitrochoid-x
  "Returns the x location at angle t (in radians) of the epitrochoid
  with fixed circle radius R (in radians), rolling circle radius r (in
  radians), and length of radial d."
  [R r d t]
  (- (* (+ R r) (Math/cos t))
     (* d (Math/cos (* (/ (+ R r)
                         r)
                       t)))))

(defn epitrochoid-y
  "Returns the y location at angle t (in radians) of the epitrochoid
  with fixed circle radius R (in radians), rolling circle radius r (in
  radians), and length of radial d."
  [R r d t]
  (- (* (+ R r) (Math/sin t))
     (* d (Math/sin (* (/ (+ R r)
                         r)
                       t)))))
