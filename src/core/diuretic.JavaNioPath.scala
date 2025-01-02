/*
    Diuretic, version [unreleased]. Copyright 2025 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package diuretic

import anticipation.*

import java.nio.file as jnf

import language.experimental.captureChecking

object JavaNioPath
extends SpecificFile, SpecificDirectory, SpecificPath, GenericPath, GenericDirectory, GenericFile:

  type Self = jnf.Path

  def path(path: Text): jnf.Path = jnf.Paths.get(path.s).nn
  def file(file: Text): jnf.Path = path(file)
  def directory(directory: Text): jnf.Path = path(directory)

  def pathText(value: jnf.Path): Text = value.toAbsolutePath.nn.toString.tt
  def fileText(value: jnf.Path): Text = pathText(value)
  def directoryText(value: jnf.Path): Text = pathText(value)
