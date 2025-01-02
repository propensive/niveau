[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/phoenicia/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/phoenicia/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.com/invite/MBUrkTgMnA)
<img src="/doc/images/github.png" valign="middle">

# Phoenicia

__Read TrueType and OpenType fonts in Scala__

TrueType and OpenType fonts have become a _de facto_ standard for describing
how character data, or _text_, should be visually rendered. As binary data,
font files are not as straightforward to read as many other files. This is
particularly true of `TrueType` and `OpenType` fonts, which can include a
variety of font-related data, not all of which is needed for all applications:
_most_ of the data in a font file is exists to describe the shapes of each
glyph, which is not easy to work with. So usually, reading font files is left
to graphics software, web browsers or the operating system.

But there remains useful data (or _metadata_) in a font file which many
applications can take advantage of. In particular this includes font metrics
data, essentially the widths of glyphs, and information about the font's weight
or slant. _Phoenicia_ provides read-only access to this data.

## Features

- reads TrueType and OpenType font files
- can read from any [Turbulence](https://github.com/propensive/turbulence/) source
- access font metrics and calculate widths of individual glyphs or strings of text


## Availability






## Getting Started

All terms and types are defined in the `phoenicia` package:
```scala
import phoenicia.*
```

### Reading a TTF font

A TrueType font, which usually has the `.ttf` extension, can be loaded from any
value [Turbulence](https://github.com/propensive/turbulence/) source that can
be read as `Bytes`, and includes files on disk (using
[Galilei](https://github.com/propensive/galilei/)), classpath resources (using
[Hellenism](https://github.com/propensive/hellenism/)) or URLs (using
[Telekinesis](https://github.com/propensive/telekinesis/)).

A new `Ttf` object can be created using the `Ttf` factory, like so:
```scala
val font = Ttf(url"https://example.com/typography.ttf")
```

This will capture the binary data of the font, but will not interpret it; that
will be performed lazily on different parts of the, only when it is needed.

#### Reading an OTF font

Currently, _Phoenicia_ does not provide any features that are unique to
OpenType fonts; every feature is supported by both TrueType and OpenType fonts.
So it is sufficient to use the `Ttf` factory to load OTF files too. At a later
time, it may be necessary to provide an `Otf` factory as well.

### Operations on `Ttf`s

The only useful operation currently implemented on `Ttf` instances is the
`advanceWidth` method, which takes a `Char` value and returns its width as a
number of font design units. This width is specifically its _advance width_,
the amount of horizontal distance traveled to accommodate the glyph before the
next glyph; typically a lower value for a narrow character like `l` than for a
wide character like `w`.

A related method, width, takes a `Text` value, and returns the sum of the
advance widths of its characters, when rendered at normal spacing. The result
is specified in `ems`, with the type `Quantity[Ems[1]]`.



## Status

Phoenicia is classified as __embryotic__. For reference, Soundness projects are
categorized into one of the following five stability levels:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Projects at any stability level, even _embryonic_ projects, can still be used,
as long as caution is taken to avoid a mismatch between the project's stability
level and the required stability and maintainability of your own project.

Phoenicia is designed to be _small_. Its entire source code currently consists
of 240 lines of code.

## Building

Phoenicia will ultimately be built by Fury, when it is published. In the
meantime, two possibilities are offered, however they are acknowledged to be
fragile, inadequately tested, and unsuitable for anything more than
experimentation. They are provided only for the necessity of providing _some_
answer to the question, "how can I try Phoenicia?".

1. *Copy the sources into your own project*
   
   Read the `fury` file in the repository root to understand Phoenicia's build
   structure, dependencies and source location; the file format should be short
   and quite intuitive. Copy the sources into a source directory in your own
   project, then repeat (recursively) for each of the dependencies.

   The sources are compiled against the latest nightly release of Scala 3.
   There should be no problem to compile the project together with all of its
   dependencies in a single compilation.

2. *Build with [Wrath](https://github.com/propensive/wrath/)*

   Wrath is a bootstrapping script for building Phoenicia and other projects in
   the absence of a fully-featured build tool. It is designed to read the `fury`
   file in the project directory, and produce a collection of JAR files which can
   be added to a classpath, by compiling the project and all of its dependencies,
   including the Scala compiler itself.
   
   Download the latest version of
   [`wrath`](https://github.com/propensive/wrath/releases/latest), make it
   executable, and add it to your path, for example by copying it to
   `/usr/local/bin/`.

   Clone this repository inside an empty directory, so that the build can
   safely make clones of repositories it depends on as _peers_ of `phoenicia`.
   Run `wrath -F` in the repository root. This will download and compile the
   latest version of Scala, as well as all of Phoenicia's dependencies.

   If the build was successful, the compiled JAR files can be found in the
   `.wrath/dist` directory.

## Contributing

Contributors to Phoenicia are welcome and encouraged. New contributors may like
to look for issues marked
[beginner](https://github.com/propensive/phoenicia/labels/beginner).

We suggest that all contributors read the [Contributing
Guide](/contributing.md) to make the process of contributing to Phoenicia
easier.

Please __do not__ contact project maintainers privately with questions unless
there is a good reason to keep them private. While it can be tempting to
repsond to such questions, private answers cannot be shared with a wider
audience, and it can result in duplication of effort.

## Author

Phoenicia was designed and developed by Jon Pretty, and commercial support and
training on all aspects of Scala 3 is available from [Propensive
O&Uuml;](https://propensive.com/).



## Name

The Phoenicians were responsible for creating the first recorded alphabet.
Their land, mostly modern-day Lebanon, was _Phoenicia_.

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

The logo shows a close-up view of a character in a _serif_ font.

## License

Phoenicia is copyright &copy; 2025 Jon Pretty & Propensive O&Uuml;, and
is made available under the [Apache 2.0 License](/license.md).

