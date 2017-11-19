# promenade Todo and Change Log

## Todo

- [Todo] ClojureScript support
- [Todo] Adaptation of state monad
- [Todo] Context pattern matching
  - [Todo] `if-mlet`, `when-mlet`, `cond-mlet`
  - [Todo] Matchers: `mval`, `mfailure`, `mnothing`, `mthrown`
- [Todo] Extensibility
  - Either (Success/Failure)
    - [TODO] Extension: Via protocol promenade.type.IFailure (impl promenade.type.Failure)
  - Maybe (Just/Nothing)
    - [TODO] Extension: Via protocol promenade.type.INothing (impl promenade.type.Nothing)
  - Trial (Result/Exception)
    - [TODO] Extension: Via protocol promenade.type.IThrown (impl promenade.type.Thrown)


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
