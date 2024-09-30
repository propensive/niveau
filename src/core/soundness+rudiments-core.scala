/*
    Rudiments, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package soundness

export rudiments.{ByteSize, bin, hex, Hex, b, kb, mb, gb, tb, byteSize, sift, has, //where,
    interleave, each, all, sumBy, bi, tri, indexBy, longestTrain, mutable, immutable, snapshot,
    place, upsert, collate, establish, plus, runs, runsBy, Cursor, cursor, precursor, postcursor,
    cursorIndex, cursorOffset, curse, ult, create, javaInputStream, DecimalConverter, as, Irrefutable,
    Unapply, As, ClassTag, Typeable, Set, List, ListMap, Map, TreeSet, TreeMap, TrieMap, NonFatal,
    boundary, break, IteratorHasAsScala, ListHasAsScala, MapHasAsScala, SeqHasAsJava, MapHasAsJava,
    EnumerationHasAsScala, tailrec, missingContext, targetName, switch, StaticAnnotation,
    uncheckedVariance, uncheckedCaptures, uncheckedStable, ###, map, contramap, Functor, Cofunctor,
    ExitStatus, Pid, Quickstart, Nat, Label, unit, waive, twin, triple, is, matchable, give, pipe,
    tap, also, Counter, loop, Loop, &, tuple, to, WorkingDirectoryError, HomeDirectoryError,
    WorkingDirectory, HomeDirectory, workingDirectory, homeDirectory, Bond, binds, bound, prim, sec,
    ter, unwind, at, Indexable, yet, Capability}

package quickstart:
  export rudiments.quickstart.defaults as defaults

package workingDirectories:
  export rudiments.workingDirectories.default as default

package homeDirectories:
  export rudiments.homeDirectories.default as default
