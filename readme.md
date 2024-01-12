[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/capricious/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/capricious/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/7b6mpF6Qcf)
<img src="/doc/images/github.png" valign="middle">

# Capricious

__Generation of random values__

Being able to calculate random values is useful for a variety of tasks, not least as test data.
Capricious provides a more convenient interface to the standard pseudorandom number generators 
available on the JVM.

## Features

- generalized abstraction of random value generation
- special support for generating random `Double`s in a user-specified distribution
- parameterized uniform, Normal (Gaussian) and Gamma distributions are available
- offers a choice of random sources: "default", secure and strongly secure, with optional seed values
- generic derivation of product and sum types

## Availability Plan

Capricious has not yet been published. The medium-term plan is to build Capricious
with [Fury](/propensive/fury) and to publish it as a source build on
[Vent](/propensive/vent). This will enable ordinary users to write and build
software which depends on Capricious.

Subsequently, Capricious will also be made available as a binary in the Maven
Central repository. This will enable users of other build tools to use it.

For the overeager, curious and impatient, see [building](#building).

## Getting Started

All terms and types are defined in the `capricious` package:
```scala
import capricious.*
```

### Generating random values

A random instance of some type, `Type` can be constructed by calling,
```scala
val randomInt = random[Int]()
val randomBoolean = random[Boolean]()
```
or, if the type can be inferred from the context, just,
```scala
val randomChar: Char = random()
```

Random values can only be generated for certain types, but this includes most
primitive types, and any type for which an `Arbitrary` typeclass instance
exists.

A value generated with `random` should be unpredictable, since it will be
determined from a random 64-bit value provided by the JVM's default random
number generator. This is, however, only a pseudorandom number generator, and
the sequence it produces will be deterministic, albeit difficult to predict.

### Repeatability

Randomness is useful, but it can undermine repeatability. So a more
fine-grained mechanism is available for generating random values with the same
probability distribution, but using different random number generators (RNGs),
including seeded RNGs which will produce a repeatable sequence of values each
time.

The code which will generate random values of this form must be delimited in a
`stochastic` block. Within this block, new random values can be generated by
calling `arbitrary` in much the same way as we called `random` before.

But in order to construct a new `stochastic` block, a random number generator
should be specified, with a seed value if necessary. For now, we will used the
default random number generator, with a specified seed value.

For example,
```scala
given Seed = Seed(12L)
import randomNumberGenerators.seeded

def main(): Unit = stochastic:
  println(arbitrary[Int]())
  println(arbitrary[Char]())
```


Note that the sequence of random values generated within a stochastic block
will be deterministic, so long as the code is deterministic. This is generally
true for single-threaded code, but concurrency can introduce nondeterminism,
since multiple threads could cause random values to be generated in a different
order across threads each time the code is run. Therefore, it is important to
initiate a new `stochastic` block for each thread, using a seed generated from
the parent thread, like so:
```scala
import parasite.*

given Seed = Seed(42L)
import randomNumberGenerators.seeded

def main(): Unit =
  stochastic:
    val seed1 = arbitrary[Seed]()
    val seed2 = arbitrary[Seed]()
  
    val async1 = Async:
      stochastic(using seed1):
        println(arbitrary[Int])

    val async2 = Async:
      stochastic(using seed2):
        println(arbitrary[Int])

    async1.await()
    async2.await()
``

### Generating random `Double`s

Random `Double`s can be generated only if a probability distribution is
specified. Since `Double`s are a 64-bit approximation of the set of real
numbers, which is an infinite set, there is no clear answer for what
probability each possible `Double` value should have of being chosen randomly.
Hence, several options are provided, which can be selected by importing them as
contextual values:

- `import randomDistributions.gaussian` - the Gaussian distribution with mean,
  `0`, and variance, `1`
- `import randomDistributions.uniformUnitInterval` - uniform across the
  interval `[0, 1]`
- `import randomDistributions.uniformSymmetricUnitInterval` - uniform across
  the interval `[-1, 1]`
- `import randomDistributions.binary` - uniform across the 64-bit binary
  representations of IEEE 754
  double-precision values
- `given Distribution = Gamma(shape, scale)` - a Gamma distribution with a
  specified shape (k) and
  scale (θ)
- `given Distribution = Gaussian(mean, standardDeviation)` - a Gaussian
  (normal) distribution with specified mean (x̄) and standard deviation (σ)
- `given Distribution = UniformDistribution(start, end)` - a uniform
  distribution in the range `[start, end]`

### Random sources

Several (pseudo-)random number generators are available, sometimes in seeded and unseeded variants:

- `import randomNumberGenerators.unseeded` - a "standard" generator, with no seed
- `import randomNumberGenerators.seeded` - a "standard" generator, requiring a contextual `Seed` instance
- `import randomNumberGenerators.secureUnseeded` - a "secure" generator, with no seed
- `import randomNumberGenerators.secureSeeded` - a "secure" generator, requiring a contextual `Seed` instance
- `import randomNumberGenerators.stronglySecure` - a "strongly secure" generator, which cannot be seeded

Those generators which require a seed value can define it, as a `Long` value, with:
```scala
given Seed = Seed(23956242374982L)
```
or as a byte array of arbitrary length, for example,
```scala
given Seed = Seed(Bytes(78, 124, 19, 3, 52, 99, 112, 89, 8, 7, 12))
```
though different random number generators may only use as much of the seed value as they need.

### The `Arbitrary` Typeclass

The typeclass, `Arbitrary`, will produce random instances of its type parameter. Given instances are
predefined for a few basic types, but custom instances can be constructed by implementing the trait:
```scala
trait Arbitrary[ValueType]:
  def from(gen: => Long): ValueType
```

An implementation of `from` should call `gen` as many times as necessary to construct a new, random instance
of `ValueType`. Although random, the instance of `ValueType` should depend deterministically on the values
produced by `gen`.

### Product and Sum types

Capricious can construct random instances of product types such as case classes and enumeration cases, and
sum types like `enum`s and sealed traits, as long as each field of the product and variant of the sum has
a valid `Arbitrary` instance.

## Status

Capricious is classified as __fledgling__. For reference, Scala One projects are
categorized into one of the following five stability levels:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Projects at any stability level, even _embryonic_ projects, can still be used,
as long as caution is taken to avoid a mismatch between the project's stability
level and the required stability and maintainability of your own project.

Capricious is designed to be _small_. Its entire source code currently consists
of 95 lines of code.

## Building

Capricious will ultimately be built by Fury, when it is published. In the
meantime, two possibilities are offered, however they are acknowledged to be
fragile, inadequately tested, and unsuitable for anything more than
experimentation. They are provided only for the necessity of providing _some_
answer to the question, "how can I try Capricious?".

1. *Copy the sources into your own project*
   
   Read the `fury` file in the repository root to understand Capricious's build
   structure, dependencies and source location; the file format should be short
   and quite intuitive. Copy the sources into a source directory in your own
   project, then repeat (recursively) for each of the dependencies.

   The sources are compiled against the latest nightly release of Scala 3.
   There should be no problem to compile the project together with all of its
   dependencies in a single compilation.

2. *Build with [Wrath](https://github.com/propensive/wrath/)*

   Wrath is a bootstrapping script for building Capricious and other projects in
   the absence of a fully-featured build tool. It is designed to read the `fury`
   file in the project directory, and produce a collection of JAR files which can
   be added to a classpath, by compiling the project and all of its dependencies,
   including the Scala compiler itself.
   
   Download the latest version of
   [`wrath`](https://github.com/propensive/wrath/releases/latest), make it
   executable, and add it to your path, for example by copying it to
   `/usr/local/bin/`.

   Clone this repository inside an empty directory, so that the build can
   safely make clones of repositories it depends on as _peers_ of `capricious`.
   Run `wrath -F` in the repository root. This will download and compile the
   latest version of Scala, as well as all of Capricious's dependencies.

   If the build was successful, the compiled JAR files can be found in the
   `.wrath/dist` directory.

## Contributing

Contributors to Capricious are welcome and encouraged. New contributors may like
to look for issues marked
[beginner](https://github.com/propensive/capricious/labels/beginner).

We suggest that all contributors read the [Contributing
Guide](/contributing.md) to make the process of contributing to Capricious
easier.

Please __do not__ contact project maintainers privately with questions unless
there is a good reason to keep them private. While it can be tempting to
repsond to such questions, private answers cannot be shared with a wider
audience, and it can result in duplication of effort.

## Author

Capricious was designed and developed by Jon Pretty, and commercial support and
training on all aspects of Scala 3 is available from [Propensive
O&Uuml;](https://propensive.com/).



## Name

Something which is _capricious_ is determined by chance, whimsy or impulse.

In general, Scala One project names are always chosen with some rationale,
however it is usually frivolous. Each name is chosen for more for its
_uniqueness_ and _intrigue_ than its concision or catchiness, and there is no
bias towards names with positive or "nice" meanings—since many of the libraries
perform some quite unpleasant tasks.

Names should be English words, though many are obscure or archaic, and it
should be noted how willingly English adopts foreign words. Names are generally
of Greek or Latin origin, and have often arrived in English via a romance
language.

## Logo

The logo shows a stylized pollen cell, the canonical body used to illustrate Brownian (i.e. random) motion.

## License

Capricious is copyright &copy; 2024 Jon Pretty & Propensive O&Uuml;, and
is made available under the [Apache 2.0 License](/license.md).

