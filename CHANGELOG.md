# promenade Todo and Change Log

## Todo

- [Todo] Adaptation of state monad


## [WIP] 0.2.0 / 2017-November-??

- Add ClojureScript support
- Make context types extensible
  - Either (Success/Failure): Protocol `promenade.type.IFailure` (impl `promenade.type.Failure`)
  - Maybe (Just/Nothing):     Protocol `promenade.type.INothing` (impl `promenade.type.Nothing`)
  - Trial (Result/Exception): Protocol `promenade.type.IThrown`  (impl `promenade.type.Thrown`)
- [Todo] Context pattern matching
  - [Todo] `if-mlet`, `when-mlet`, `cond-mlet`
  - [Todo] Matchers: `mval`, `mfailure`, `mnothing`, `mthrown`


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
