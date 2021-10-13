[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/v7CjtbnwDq)
[<img src="https://vent.dev/badge/propensive/gossamer" height="24">](https://vent.dev/)
<img src="/doc/images/github.png" valign="middle">

# Gossamer

Gossamer provides the `Text` type, a typesafe opaque type alias of `String` and a `Show` typeclass
instance.

## Features

- provides a `Show` typeclass with instances for common types, including product types
- reimplements common methods on `String` with more typesafe variants
- implementation of the Minimum Edit Distance algorithm
- convenient converters to common encodings like URL encodings and Punycode
- implements a stricter `str""` interpolator for strings
- implements the `txt""` interpolator to ignore spurious whitespace in strings which flow onto multiple lines


## Getting Started

TBC


## Status

Gossamer is classified as __fledgling__. Propensive defines the following five stability levels for open-source projects:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

## Availability

Gossamer&rsquo;s source is available on GitHub, and may be built with [Fury](https://github.com/propensive/fury) by
cloning the layer `propensive/gossamer`.
```
fury layer clone -i propensive/gossamer
```
or imported into an existing layer with,
```
fury layer import -i propensive/gossamer
```

## Contributing

Contributors to Gossamer are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/gossamer/labels/good%20first%20issue"><img alt="label: good first issue"
src="https://img.shields.io/badge/-good%20first%20issue-67b6d0.svg" valign="middle"></a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Gossamer easier.

Please __do not__ contact project maintainers privately with questions, as other users cannot then benefit from
answers given in private.

## Author

Gossamer was designed and developed by Jon Pretty, and commercial support and training is available from
[Propensive O&Uuml;](https://propensive.com/).



## Name

Gossamer is lightweight and stringlike.

## License

Gossamer is copyright &copy; 2020-21 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
