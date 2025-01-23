/*
    Feudalism, version 0.26.0. Copyright 2025 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package feudalism

import gossamer.*
import probably.*

import language.experimental.captureChecking

object Tests extends Suite(t"Feudalism tests"):
  def run(): Unit =
    test(t"read mutex"):
      val mutex = Mutex[String]("Hello")

      val result = mutex.read: ref =>
        println(ref())
        ref.snapshot()

      result

    .assert(_ == "Hello")
