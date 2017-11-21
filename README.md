# promenade

Take program design oddities in stride in Clojure/ClojureScript.

## Rationale

Non-trivial Clojure applications often need to deal with deeply nested conditions, complex error handling and
potentially missing data. Naively applying Clojure's facilities to such problems may lead to brittle code that
is hard to reason about. This library provides few basic mechanisms to decouple the tangled concerns.

### Goals

- Provide facilities to take apart code units and put back together in a cleaner way
- Provide simple & effective API without the awkwardness of using monads in Clojure
- Keep the surface area of this library's API small, fun and easy to work with

### Non goals

- Be faithful implementation of monads
- Adapt all known monads (completeness)

### Implemented features

- Decouple condition checks from conditional action by representing success and failure (called _Either_)
- Represent and deal with available and missing values (called _Maybe_)
- Avoid imperativeness and coupling of throwing and catching of exceptions (called _Trial_)

### Other work

You may find similarities to this library in the following work:

- [Failjure](https://github.com/adambard/failjure)
- [SynThread](https://github.com/LonoCloud/synthread)
- https://youtu.be/3y7xzH8jB8A?t=1390
- [Various](https://github.com/funcool/cats) [Clojure](https://github.com/clojure/algo.monads) [monad](http://fluokitten.uncomplicate.org/) [implementations](https://github.com/blancas/morph)

## Usage

Clojars coordinates: `[promenade "0.2.0"]`

See [Documentation](doc/intro.md)

## Development

Running tests:

```bash
$ lein do clean, test       # run tests in lowest supported Clojure version
$ lein do clean, clj-test   # run tests in all supported versions of Clojure
$ lein do clean, cljs-test  # run tests in ClojureScript using NodeJS
```

## License

Copyright © 2017 Shantanu Kumar (kumar.shantanu@gmail.com, shantanu.kumar@concur.com)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
