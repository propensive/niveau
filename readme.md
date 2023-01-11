[<img alt="GitHub Workflow" src="https://img.shields.io/github/actions/workflow/status/propensive/polyvinyl/main.yml?style=for-the-badge" height="24">](https://github.com/propensive/polyvinyl/actions)
[<img src="https://img.shields.io/discord/633198088311537684?color=8899f7&label=DISCORD&style=for-the-badge" height="24">](https://discord.gg/7b6mpF6Qcf)
<img src="/doc/images/github.png" valign="middle">

# Polyvinyl

_Polyvinyl_ makes it easy to define schemas for and use record types, originating from a
variety of possible sources. These could originate from a database, a file or some other
source, loaded at compiletime, and utilized in a later phase of compilation.

## Features

- provides support for record types in Scala
- allows the implementation of F#-style type providers
- enforces namespace-safety on field access
- record schemas may be defined programmatically, without writing explicit case classes
- schemas can be defined dynamically, taking strings as field names
- schemas may be defined in the same module, provided they are not used in the same file
- uses Scala 3's `Selectable` trait internally


## Availability

Polyvinyl has not yet been published as a binary, though work is ongoing to fix this.

## Getting Started

### Untyped Schemas

A schema may be implemented as a singleton object extending `SimpleSchema[T]`, where `T` is the
fixed type that each field will return. Additionally, the object should also define the method
`fields`, returning a `List[String]` of the valid field names for record instances of that schema.
Whichever string values this method returns when the code to construct a record is invoked (at
compiletime) will be considered valid field names for that record.

Accordingly, different schemas must be implemented as different singletons.

Furthermore, an `record` method should be implemented as a macro on the singleton object, exactly
as follows:
```scala
transparent inline def record(inline fn: String => T): SimpleRecord[T] = ${build('fn)}
```

Invoking this method will construct a new record, an instance of `SimpleRecord[T]`, whose field
values will be obtained by calling the `fn` function with the field's name, as a `String`.

Here is a full, albeit uninteresting, example:
```scala
object Numbers extends SimpleSchema[Int]:
  def fields = List("one", "two", "three", "four")
  transparent inline def record(inline fn: String => Int): SimpleRecord[Int] =
    ${build('fn)}
```

The field names are defined in the source code, but they could be obtained from anywhere
(provided the code will produce the desired output when it is invoked inside the compiler at
compiletime).

In a different file, this `Numbers` schema object may be used to construct new `SimpleRecord[Int]`
objects. These instances must be backed by some means of obtaining the field values, given a
field name; this is just a lambda, so the implementation is up to the programmer.

Here, we implement a record using a `Map[String, Int]`:
```scala
val numberMap = Map(
  "one"   -> 1,
  "two"   -> 2,
  "three" -> 3,
  "four"  -> 4,
  "five"  -> 5
)

val rec = Numbers.record(numberMap(_))
```

Given the value, `rec`, any fields defined in the `fields` method may be invoked on it, and will
return appropriate values from the map.

For example, `rec.one` will return the `Int`, `1`. But `rec.six` will be a compile error,
as will `rec.five`: even though the runtime `numberMap` object contains a value for the
`String` `"five"`, the schema does not define it.

### Typed Schemas

A schema whose fields may return different types requires a bit more work to implement. Its records
will be instances of `Record` (rather than `SimpleRecord[T]`) and its schemas will have the type,
`Schema[E]`, where `E` must be an enumeration type (a subtype of `reflect.Enum`), consisting only
of unparameterized values. Instead of the value, `fields`, a `Schema[E]` has a `Map[String, E]`
called `types`.

The purpose of `E` is to facilitate a correspondence between a set of runtime values, and a set of
compiletime types. This is necessary because some _runtime_ representation of a field's return type
is needed to inform the compiler about the structure of records.

So first, an `enum` should be written with cases for each possible return type, for example:
```scala
enum FieldType:
  case Number, Bool, Varchar
```

Each enumeration case has a corresponding singleton type, and the schema—in this example,
`Schema[FieldType]`—needs a _match type_ to specify how the singleton type of each `FieldType` case
maps to a return type. This type constructor, and member of `Schema[E]`, is called `Result` and
could be implemented as follows:
```scala
type Result[T <: FieldType] = T match
  case FieldType.Number.type  => Double
  case FieldType.Bool.type    => Boolean
  case FieldType.Varchar.type => String
```

Then, the schema requires an implementation of `types`, which should map from a `String` key to
the enum type representing its return type. For example,
```scala
lazy val types: Map[String, FieldType] = Map(
  "age"      -> FieldType.Number,
  "name"     -> FieldType.Varchar,
  "employed" -> FieldType.Bool
)
```

A more typical implementation of `types` may have much more complex logic, reading an external
schema definition, and marshalling it into the `Map[String, E]` instance.

As with `SimpleSchema`, a `Schema` must be a singleton object, and requires an implementation of
`record`. Collecting these parts together, we have:
```scala

enum FieldType:
  case Number, Bool, Varchar

object MySchema extends Schema[FieldType]:
  import FieldType.*
  
  lazy val types: Map[String, FieldType] =
    Map("age" -> Number, "name" -> Varchar, "employed" -> Bool)
  
  type Result[T <: FieldType] = T match
    case Number  => Double
    case Bool    => Boolean
    case Varchar => String
  
  transparent inline def record(inline fn: String => Any): Record =
    ${build('fn)}
```

As before, new `Record` instances may be constructed by calling `MySchema.record(fn)` with an
appropriate function, `fn`. This should be implemented carefully: `fn`'s type is
`String => Any`, but its return value will be cast to the the declared type whenever the field is
accessed. So the runtime type returned by this method _must_ be consistent with the field types
declared in `types`.

Commonly, this method would refer to `types` to work out the type of the value to return, for
example, backed by a `Map[String, String]`, called `data`:
```scala
MySchema.record { key =>
  types(key) match
    case FieldType.Number => data(key)
    case FieldType.Bool   => data(key) == "true"
    case FieldType.Int    => data(key).toDouble
}
```

This logic may be inconvenient to include at every record creation site, so it would be convenient
to include in the `MySchema` object itself, for example, as an `apply` method which takes the
`Map[String, String]` directly, for example,
```scala
transparent inline def apply(value: Map[String, String]): Record =
  record { key => ... }
```
remembering to make it transparent and inline to ensure that the precise type of the returned
`Record` is preserved—after all, that is the whole point!

### Structural Types

Polyvinyl uses Scala 3 macros to return structural refinements of `Record` and `SimpleRecord` types.

For example, given the definition of `MySchema`, above, the return type of the method, `record`,
would be:
```scala
Record { def age: Double; def name: String; def employed: Boolean }
```

This means that, not only is the return value a `Record`, but it additionally has fields called,
`age`, `name` and `employed` returning a `Double`, a `String` and a `Boolean` respectively.

## Compilation Order

It is crucial that the schema singleton is defined in a different file from the invocation. This
is to guarantee that the object (and hence the fields it defines) is available as a runtime object
during later compilations which use it. Scala is able to compile a macro definition and its usage
in the correct order so long as they are defined in separate files (and no cyclic dependencies
exist between those files).

## Limitations

`SimpleSchema` and `Schema` do not currently make their precise record types available except as the
return type of calling `record`. However the type is statically known, and could potentially be made
available as a type member.

## Related Projects

The following _Scala One_ libraries are dependencies of _Polyvinyl_:

[![Rudiments](https://github.com/propensive/rudiments/raw/main/doc/images/128x128.png)](https://github.com/propensive/rudiments/) &nbsp;

The following _Scala One_ libraries are dependents of _Polyvinyl_:

[![Cellulose](https://github.com/propensive/cellulose/raw/main/doc/images/128x128.png)](https://github.com/propensive/cellulose/) &nbsp;

## Status

Polyvinyl is classified as __fledgling__. For reference, Scala One projects are
categorized into one of the following five stability levels:

- _embryonic_: for experimental or demonstrative purposes only, without any guarantees of longevity
- _fledgling_: of proven utility, seeking contributions, but liable to significant redesigns
- _maturescent_: major design decisions broady settled, seeking probatory adoption and refinement
- _dependable_: production-ready, subject to controlled ongoing maintenance and enhancement; tagged as version `1.0` or later
- _adamantine_: proven, reliable and production-ready, with no further breaking changes ever anticipated

Projects at any stability level, even _embryonic_ projects, are still ready to
be used, but caution should be taken if there is a mismatch between the
project's stability level and the importance of your own project.

Polyvinyl is designed to be _small_. Its entire source code currently consists
of 34 lines of code.

## Building

Polyvinyl can be built on Linux or Mac OS with [Fury](/propensive/fury), however
the approach to building is currently in a state of flux, and is likely to
change.

## Contributing

Contributors to Polyvinyl are welcome and encouraged. New contributors may like to look for issues marked
<a href="https://github.com/propensive/polyvinyl/labels/beginner">beginner</a>.

We suggest that all contributors read the [Contributing Guide](/contributing.md) to make the process of
contributing to Polyvinyl easier.

Please __do not__ contact project maintainers privately with questions unless
there is a good reason to keep them private. While it can be tempting to
repsond to such questions, private answers cannot be shared with a wider
audience, and it can result in duplication of effort.

## Author

Polyvinyl was designed and developed by Jon Pretty, and commercial support and training is available from
[Propensive O&Uuml;](https://propensive.com/).



## Name

Polyvinyl is the substance from which records (LPs) are made; the purpose of this library is to produce record types.

## License

Polyvinyl is copyright &copy; 2021-23 Jon Pretty & Propensive O&Uuml;, and is made available under the
[Apache 2.0 License](/license.md).
