# promenade

[![Build Status](https://travis-ci.org/kumarshantanu/promenade.svg)](https://travis-ci.org/kumarshantanu/promenade)

Take program design oddities in stride with Clojure/ClojureScript.

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
- Be limited to monadic ideas

### Implemented features

- Decouple condition checks from conditional action by representing success and failure (called _Either_)
- Represent and deal with available and missing values (called _Maybe_)
- Avoid imperativeness and coupling of throwing and catching of exceptions (called _Trial_)

### Other work

You may find similarities to this library in the following work:

- [Failjure](https://github.com/adambard/failjure)
- [SynThread](https://github.com/LonoCloud/synthread)
- [Either-monad adaptation](https://youtu.be/3y7xzH8jB8A?t=1390)
- [Various](https://github.com/funcool/cats) [Clojure](https://github.com/clojure/algo.monads) [monad](http://fluokitten.uncomplicate.org/) [implementations](https://github.com/blancas/morph)

### External references

- IN/Clojure 2018 talk: [Elegant error-handling for a more civilized age](https://www.youtube.com/watch?v=FsyPQG_IuaY) and [Slides](https://speakerdeck.com/varunited/elegant-error-handling-for-a-more-civilized-age) by Varun Sharma
- Blog post: [Using Clojure macros for nicer error handling](https://jakemccrary.com/blog/2018/02/18/using-clojure-macros-for-nicer-error-handling/) by Jake McCrary

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/promenade.svg)](https://clojars.org/promenade)

See [Documentation](doc/intro.md)

## Discuss

Slack channel: [#promenade](https://clojurians.slack.com/messages/CAK3M4A65/) (you need an invitation from
http://clojurians.net/ to join the Clojurian Slack team)

## Development

Running tests:

```bash
$ lein do clean, test       # run tests in lowest supported Clojure version
$ lein do clean, clj-test   # run tests in all supported versions of Clojure
$ lein do clean, cljs-test  # run tests in ClojureScript using NodeJS
```

## License

Copyright © 2017-2021 Shantanu Kumar

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
