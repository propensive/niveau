[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/vacuous/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/vacuous/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.com/invite/MBUrkTgMnA)
<img src="/doc/images/github.png" valign="middle">

# Vacuous

__A simple representation of optional values__

Scala's `Option` is the traditional way to represent values which may be either
_present_ (in which case a value is specified) or _absent_. `Option` is a
simple ADT, but union types, along with some helper methods, can provide much
better ergonomics in most circumstances. __Vacuous__ offers an `Optional` type
which provides this functionality.

## Features

- Provides an implementation of optional values using union types
- No need to wrap _present_ values in `Some`
- Flattened representation, prohibiting ambiguous `Some(None)` representation
- Lightweight `let`, `lay` and `or` methods cover `map`, `flatMap`, `fold`, `orElse` and `getOrElse`
- Performant inlined implementations of all critical methods
- Especially convenient use case for default parameters


## Availability

Vacuous has not yet been published. The medium-term plan is to build it with
[Fury](https://github.com/propensive/fury) and to publish it as a source build
on [Vent](https://github.com/propensive/vent). This will enable ordinary users
to write and build software which depends on Vacuous.

Subsequently, Vacuous will also be made available as a binary in the Maven
Central repository. This will enable users of other build tools to use it.

For the overeager, curious and impatient, see [building](#building).




## Getting Started

### The `Optional` type

An optional value, which might be an instance of `ValueType`, or may be
_absent_, may be given the type `Optional[ValueType]`. If it is absent, then it
has the value, `Unset`, which is a singleton object. `Optional[ValueType]` is
an alias for the union type, `ValueType | Unset.type`.

Note that the declarations,
```scala
val value: Text = t"Hello world"
```
and,
```scala
val value: Optional[Text] = t"Hello world"
```
differ only in their types; the syntax of the expression is identical, and does
not need to be wrapped with another factory method, like `Some`.

Since union types are unordered sets of types, nesting two `Optional`s, for
example in `Optional[Optional[Int]]` expands to
`Int | Unset.type | Unset.type`, which is identical to `Int | Unset.type`. And
this is the same as `Optional[Int]`. While there is nothing to prevent nesting
one `Optional` within another `Optional`, it's impossible to distinguish
between the types, and impossible for an `Unset` value to be considered
_present_ rather than _absent_; it is the definition of absence.

### `or`, `let` and `lay`

An `Optional[Text]` value may seem very similar to a `Text` value, but the
possibility that it might be `Unset` makes it impossible to use any methods
defined on `Text` on an `Optional[Text]`, since those methods are not
applicable to just one of the possible values of the type, `Unset`. So several
convenience methods are provided to make `Optional`s easy to work with.

The method `or` replaces the `Unset` value with another value, eliminating the
optionality from the type. This is equivalent to _both_ `getOrElse` and
`orElse` on `Option`. This equivalence comes from the lack of nesting of
`Optional` values.

Similarly, `let` applies a lambda to the _present_ values, and leaves the
_absent_ value unchanged. It is equivalent to both `map` and `flatMap` on
`Option`s.

Finally, `lay` combines `or` and `let` in a single, two-parameter method: the
alternative value for `Unset` is specified first, followed by the lambda
mapping the _present_ values. This is equivalent to `fold` on an `Option`.

These method names were deliberately chosen to be short, as they are intended
to be used frequently and are rarely the most interesting part of an
expression.



## Status

Vacuous is classified as __maturescent__. For reference, Soundness projects are
categorized into one of the following five stability levels:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Projects at any stability level, even _embryonic_ projects, can still be used,
as long as caution is taken to avoid a mismatch between the project's stability
level and the required stability and maintainability of your own project.

Vacuous is designed to be _small_. Its entire source code currently consists
of 116 lines of code.

## Building

Vacuous will ultimately be built by Fury, when it is published. In the
meantime, two possibilities are offered, however they are acknowledged to be
fragile, inadequately tested, and unsuitable for anything more than
experimentation. They are provided only for the necessity of providing _some_
answer to the question, "how can I try Vacuous?".

1. *Copy the sources into your own project*
   
   Read the `fury` file in the repository root to understand Vacuous's build
   structure, dependencies and source location; the file format should be short
   and quite intuitive. Copy the sources into a source directory in your own
   project, then repeat (recursively) for each of the dependencies.

   The sources are compiled against the latest nightly release of Scala 3.
   There should be no problem to compile the project together with all of its
   dependencies in a single compilation.

2. *Build with [Wrath](https://github.com/propensive/wrath/)*

   Wrath is a bootstrapping script for building Vacuous and other projects in
   the absence of a fully-featured build tool. It is designed to read the `fury`
   file in the project directory, and produce a collection of JAR files which can
   be added to a classpath, by compiling the project and all of its dependencies,
   including the Scala compiler itself.
   
   Download the latest version of
   [`wrath`](https://github.com/propensive/wrath/releases/latest), make it
   executable, and add it to your path, for example by copying it to
   `/usr/local/bin/`.

   Clone this repository inside an empty directory, so that the build can
   safely make clones of repositories it depends on as _peers_ of `vacuous`.
   Run `wrath -F` in the repository root. This will download and compile the
   latest version of Scala, as well as all of Vacuous's dependencies.

   If the build was successful, the compiled JAR files can be found in the
   `.wrath/dist` directory.

## Contributing

Contributors to Vacuous are welcome and encouraged. New contributors may like
to look for issues marked
[beginner](https://github.com/propensive/vacuous/labels/beginner).

We suggest that all contributors read the [Contributing
Guide](/contributing.md) to make the process of contributing to Vacuous
easier.

Please __do not__ contact project maintainers privately with questions unless
there is a good reason to keep them private. While it can be tempting to
repsond to such questions, private answers cannot be shared with a wider
audience, and it can result in duplication of effort.

## Author

Vacuous was designed and developed by Jon Pretty, and commercial support and
training on all aspects of Scala 3 is available from [Propensive
O&Uuml;](https://propensive.com/).



## Name

Something which is _vacuous_ is empty; devoid of content. _Vacuous_ provides representation for values which may be thus.

In general, Soundness project names are always chosen with some rationale,
however it is usually frivolous. Each name is chosen for more for its
_uniqueness_ and _intrigue_ than its concision or catchiness, and there is no
bias towards names with positive or "nice" meanings—since many of the libraries
perform some quite unpleasant tasks.

Names should be English words, though many are obscure or archaic, and it
should be noted how willingly English adopts foreign words. Names are generally
of Greek or Latin origin, and have often arrived in English via a romance
language.

## Logo

The logo shows a stylized pressure gauge, reading zero.

## License

Vacuous is copyright &copy; 2025 Jon Pretty & Propensive O&Uuml;, and
is made available under the [Apache 2.0 License](/license.md).

