# with-dynamic-redefs

Use `with-redefs` on parallel tests.

Useful when you have a lot of tests that use `with-redefs` to [spy](https://github.com/alexanderjamesking/spy) on functions, and you need to run them in [parallel](https://github.com/metabase/hawk).

Only works with functions. 
Inspired by https://github.com/mourjo/dynamic-redef and https://gist.github.com/mourjo/c7fc03e59eb96f8a342dfcabd350a927, but preserves function metadata needed for spies.

`deps.edn` dependency:

```clojure
io.github.filipesilva/with-dynamic-redefs {:git/tag "v1.0.0"
                                           :git/sha "123abcd"}
```

Usage:

```clojure
(ns your-ns
  (:require
   [with-dynamic-redefs :as wdr]))

(defn foo []
  :none)

(wdr/with-dynamic-redefs [foo (constantly 1)]
  (foo)) 
;; => 1

```


Developed at [Metabase](https://github.com/metabase), but since we stopped needing it there I've preserved it in my github account.

PRs welcome, especially with test cases for non-trivial circumstances.
