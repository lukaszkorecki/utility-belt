(ns utility-belt.compile
  "Utilities for conditional code evaluation/loading")

(defmacro compile-if
  "Evaluate `exp` and if it returns logical true and doesn't error, expand to
  `then`.  Else expand to `else`.

  ```clojure
  (compile-if (Class/forName \"java.util.concurrent.ForkJoinTask\")
    (do-cool-stuff-with-fork-join)
    (fall-back-to-executor-services))
  ```
  "
  [exp then else]
  (if (try (eval exp)
           (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))

(defmacro compile-when
  "Evaluate `exp` and if it returns logical true and doesn't error, expand to `then`
  Otherwise evaluates to `nil`

  ```clojure
  (compile-when (require '[java-time.api :as jt])
    (do-cool-stuff-with-java-time))
  ```
  "
  [exp & then]
  `(compile-if ~exp (do ~@then) nil))
