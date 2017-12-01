# promenade Todo and Change Log

## Todo

- [Todo] Adaptation of state monad


## [WIP] 0.3.1 / 2017-December-??

- Fix issue where `mlet` does not consider a context value to be a non-match


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
