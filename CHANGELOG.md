# promenade Todo and Change Log

## Todo

- [Todo] Adaptation of state monad
- [Todo] Support for extensible, pattern-matching bind on constrained context
  - Enumeration (pattern match on individual values)
  - Range (pattern match on range groups)
- [Todo] Utility abstractions (as value or context)
  - Enum


## [WIP] 0.6.0 / 2018-July-??

- [Todo] Doumentation


## 0.6.0-alpha2 / 2018-July-26

- Add `!wrap` macro to wrap functions to return thrown context on exception
- Drop `!refn` macro in favour of `!wrap` and `refn`
- Drop `!rewrap` macro in favour of `!wrap` and `rewrap`
- Turn `rewrap` from a macro into a function


## 0.6.0-alpha1 / 2018-July-25

- Add support for reducing functions (that work with clojure.core/reduce, transducers)
  - `refn`    - construct a reducing function that terminates on encountering a context
  - `!refn`   - construct a reducing function that terminates on encountering a context or exception
  - `rewrap`  - wrap a reducing function such that it terminates on encountering a context
  - `!rewrap` - wrap a reducing function such that it terminates on encountering a context or exception


## 0.5.2 / 2018-June-05

- Do not need to do `require-macros` at usage site (@awkay)
- Improved documentation (@awkay)


## 0.5.1 / 2018-April-11

- Factor out `doo` related namespaces into a separate directory
- Make `Failure`, `Nothing` and `Thrown` instances printable at the REPL
  - Default printing: Multi method `print-method`
    - https://github.com/kumarshantanu/promenade/issues/5
  - Pretty printing using `clojure.pprint/pprint`: Multi method `simple-dispatch`


## 0.5.0 / 2018-April-06

- Utility functions
  - Predicate: `free?` (to imply "context free" or "not context")
  - Branching: `branch`
- [BREAKING CHANGE] Do not implicitly return `nothing` (or any context, unless the user asks for it)
  - `mdo` (empty body)
  - `mlet` (empty body)
  - `when-mlet` (test fail, empty body)
- Doc: Improve section `Dealing with success and failure with Either`
  - https://github.com/kumarshantanu/promenade/issues/3
- Test with muliple CLJS versions


## 0.4.0 / 2018-February-09

- Add `mdo` as an equivalent of `do` form
  - Context values are returned as soon as encountered
- Retrofit other forms using `mdo`
  - `mlet`
  - `when-mlet`
- Add arity to specify 'default'
  - `mfailure`
  - `mthrown`


## 0.3.2 / 2017-December-29

- Fix issue where a context cannot be printed due to clash of types: `IPersistentMap` and `IDeref`


## 0.3.1 / 2017-December-01

- Fix issue where matching macros do not consider a context value to be a non-match
  - `mlet`
  - `if-mlet`
  - `when-mlet`
  - `cond-mlet`


## 0.3.0 / 2017-November-23

- Add context pattern matching support
  - Matcher fns: `mfailure`, `mnothing`, `mthrown`
  - Bind macros: `mlet`, `if-mlet`, `when-mlet`, `cond-mlet`
- Add utility fn `thrown` to wrap as thrown
- Add predicate fns for context types


## 0.2.0 / 2017-November-21

- Add ClojureScript support
- Make context types extensible
  - Failure (Either): Protocol `promenade.type.IFailure`
  - Nothing (Maybe):  Protocol `promenade.type.INothing`
  - Thrown  (Trial):  Protocol `promenade.type.IThrown`


## 0.1.0 / 2017-November-19

### Added
- Context support
  - Contract:  `promenade.type.IContext`
  - Utilities: `deref-context`
- Either (Success/Failure)
  - Utilities: `failure` (constant), `nil->failure`, `fail` and `ex-fail`
  - Bind impl: `bind-either`
  - Threading: `either->`, `either->>` and `either-as->`
- Maybe (Just/Nothing)
  - Utilities: `nothing` (constant), `nil->nothing` and `void`
  - Bind impl: `bind-maybe`
  - Threading: `maybe->`, `maybe->>` and `maybe-as->`
- Trial (Result/Exception)
  - Utilities: `!`
  - Bind impl: `bind-trial`
  - Threading: `trial->`, `trial->>` and `trial-as->`
