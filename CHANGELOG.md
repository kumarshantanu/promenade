# promenade Todo and Change Log

## Todo

- [Todo] Adaptation of state monad
- [Todo] Support for extensible, pattern-matching bind on constrained context
  - Enumeration (pattern match on individual values)
  - Range (pattern match on range groups)
- [Todo] Utility abstractions (as value or context)
  - Enum


## 0.8.0-alpha1 / 2021-January-09

- [BREAKING CHANGE] Drop support for ClojureScript 1.9
  - Due to `javax.xml.bind.DatatypeConverter` exceptions with Java 9+
- [BREAKING CHANGE] Contexts holding value (e.g. failure and thrown) must implement `promenade.type/IHolder`
  - New protocol `promenade.type/IHolder` is introduced
    - To hold a value, as a substitute for `IDeref`
    - Because `clojure.lang.IDeref` is a Java interface - cannot be extended to `java.lang.Throwable`
  - Built-in failure and thrown implementations are updated to implement `promenade.type/IHolder`
- Optimize exception handling by avoiding unwanted wrapping/unwrapping
  - Overload `Throwable` (CLJ) and `js/Error` (CLJS) with `IThrown` and `IHolder`
  - Do not wrap exception as `Thrown` if already a `Thrown` (true in most cases)
- Add fast, stackless alternative to `ex-info`
  - Add Java class and CLJS type `promenade.util.StacklessExceptionInfo`
  - `promenade.util/se-info`
  - `promenade.util/se-info?`
  - `promenade.util/!se-info`
  - `promenade.util/!wrap-se-info`
- Add custom entity and failure definition support
  - `promenade.util/defentity`
  - `promenade.util/defailure`
- [Todo] Documentation
  - [Todo] Formatting for Cljdoc
  - [Todo] Add Cljdoc badge


## 0.7.2 / 2019-April-25

- Fix buggy handling of context as initial expression in `*->`, `*->>`, `*as->` macros


## 0.7.1 / 2019-February-20

- Allow `recur` at the tail position in code body (impacting the following macros)
  - `promenade.core/mdo`
  - `promenade.core/mlet`
  - `promenade.core/when-mlet`


## 0.7.0 / 2018-October-30

- Add reducing thread macros `reduce->`, `reduce->>`, `reduce-as->`
  - Support for early termination via `clojure.core/reduced`
  - Support for custom bind functions
- [BREAKING CHANGE] Accept `nil` as argument in `bind-maybe` nothing-handler
  - Consistent with other alt-handlers
- Base bind thread macros on reducing thread macros
  - `either->`, `either->>`, `either-as->`
  - `maybe->`, `maybe->>`, `maybe-as->`
    - [BREAKING CHANGE] Nothing handler expression must handle the argument
  - `trial->`, `trial->>`, `trial-as->`


## 0.6.0 / 2018-August-01

- Add `!wrap` macro to wrap functions to return thrown context on exception
- Add support for reducing functions (that work with clojure.core/reduce, transducers)
  - `refn`   - construct a reducing function that terminates on encountering a context
  - `rewrap` - wrap a reducing function such that it terminates on encountering a context


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
