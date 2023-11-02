[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/exoskeleton/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/exoskeleton/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/7b6mpF6Qcf)
<img src="/doc/images/github.png" valign="middle">

# Exoskeleton

__Interfaces for interacting with the shell__

Exoskeleton is a Scala library for to make it easy to write command-line applications which
interact through tab-completion in
[`bash`](https://www.gnu.org/software/bash/),
[`zsh`](http://zsh.sourceforge.net/) and [`fish`](https://fishshell.com/) by
implementing a simple,
[functional](https://en.wikipedia.org/wiki/Functional_programming) API to
define the execution of parameters.

## Features

- unified programmatic tab-completions for `bash`, `zsh` and `fish`
- automatic installation of completion scripts for each shell
- tab-completions can be generated programmatically on-demand
- high-level APIs for programs following common tab-completion patterns
- low-level APIs for more fine-grained control over completions
- fully functional API
- can be used with [GraalVM](https://www.graalvm.org/) to eliminate JVM
  startup time


## Availability

Exoskeleton has not yet been published as a binary.

## Getting Started

Exoskeleton provides the `Application` trait, akin to `App` in the Scala
Standard Library. In addition to a `main` method, the `Application` trait
defines a `complete` method, which should be implemented to return a sequence
of tab-completions based on the current state of the command-line, which is
passed in as an input parameter.

### Explanation

For a shell to offer tab-completions for a command to the user, it needs to
find definitions for the possible ways of completing the arguments to that
command. In general, this is done by searching a sequence of possible
locations for a file with a predictable name based on the command name (e.g.
for `cmd`, this file will be called `_cmd` or `cmd.fish`). These locations
are usually defined in an environment variable, containing at least some
default initial values, but potentially with additional paths added to it
during the shell startup script, e.g. `.bashrc` for `bash`.

The way a completion script works will be different for every shell, because
their tab-completion systems all work differently, but each shell typically
provides a command for specifying command completions which will be called
from the completion script. Completion scripts for most commands define a
static set of completions (which may be dependent on the state of the
command-line being completed).

Exoskeleton, however, defines very general completion scripts for each shell
it supports, which all run the command whose parameters are being completed
in “completion mode”, effectively calling the Exoskeleton application’s
`complete` method. The completion scripts receive a response from the
Exoskeleton application, in aformat that is specific to each shell, which is
transformed into calls to the completion command for that particular shell.

### Generation of completion scripts

Source files may be generated by running,
```shell
java -cp exoskeleton.jar exoskeleton.Generate <shell> <command> <dir>
```
where _shell_ is one of `bash`, `zsh` and `fish`, _command_ is the name of
the command that completions are being provided for, and _dir_ is the
directory in which the script should be written.



## Status

Exoskeleton is classified as __fledgling__. For reference, Scala One projects are
categorized into one of the following five stability levels:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Projects at any stability level, even _embryonic_ projects, are still ready to
be used, but caution should be taken if there is a mismatch between the
project's stability level and the importance of your own project.

Exoskeleton is designed to be _small_. Its entire source code currently consists
of 586 lines of code.

## Building

Exoskeleton can be built on Linux or Mac OS with [Fury](/propensive/fury), however
the approach to building is currently in a state of flux, and is likely to
change.

## Contributing

Contributors to Exoskeleton are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/exoskeleton/labels/beginner">beginner</a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Exoskeleton easier.

Please __do not__ contact project maintainers privately with questions unless
there is a good reason to keep them private. While it can be tempting to
repsond to such questions, private answers cannot be shared with a wider
audience, and it can result in duplication of effort.

## Author

Exoskeleton was designed and developed by Jon Pretty, and commercial support and training is available from
[Propensive O&Uuml;](https://propensive.com/).



## Name

Exoskeleton is a library for interacting with shells, which are their exterior skeletons—or Exoskeletons.

In general, Scala One project names are always chosen with some rationale, however it is usually
frivolous. Each name is chosen for more for its _uniqueness_ and _intrigue_ than its concision or
catchiness, and there is no bias towards names with positive or "nice" meanings—since many of the
libraries perform some quite unpleasant tasks.

Names should be English words, though many are obscure or archaic, and it should be noted how
willingly English adopts foreign words. Names are generally of Greek or Latin origin, and have
often arrived in English via a romance language.

## Logo

The logo shows a simplistic and imaginary arthropod, with a pair of wings on each side; and an exoskeleton.

## License

Exoskeleton is copyright &copy; 2023 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
