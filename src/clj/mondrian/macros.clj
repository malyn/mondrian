(ns mondrian.macros
  (use [dommy.macros :only [sel1]]))

(defmacro defmondrian
  "Defines a mondrian animation function given the initial state and a
  pair of functions that drive the the animation.  The resulting
  function takes a reference to a mondrian DOM element, which must
  include a canvas element upon which the animation will be rendered."
  [fname init-state update-pipeline render-stack]
  `(def ~(with-meta fname {:export true})
     (fn [drawing#]
       (let [canvas# (sel1 drawing# :canvas)
             ctx# (monet.canvas/get-context canvas# "2d")
             [w# h#] (mondrian.ui/setup-canvas canvas# ctx# 1.0)
             fixed-state# {:drawing drawing# :ctx ctx# :w w# :h h#}]
         (anim/start ~init-state
                     #(-> (merge % fixed-state#) ~update-pipeline)
                     #(~render-stack %)
                     #(-> ctx#
                          (monet.canvas/fill-style "red")
                          (monet.canvas/font-style "sans-serif")
                          (monet.canvas/text {:text % :x 0 :y 20})))))))
