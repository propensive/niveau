/*
    Eucalyptus, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package eucalyptus

import escapade.*
import iridescence.*, webColors.*
import anticipation.*
import gossamer.*
import symbolism.*
import fulminate.*
import spectacular.*
import hieroglyph.*
import turbulence.*
import hieroglyph.textMetrics.uniform

import java.text as jt

//import scala.language.experimental.captureChecking

package logFormats:
  given Level is Displayable = level =>
    val color = level match
      case Level.Fine => LightSeaGreen
      case Level.Info => YellowGreen
      case Level.Warn => Gold
      case Level.Fail => Tomato

    e"${Bg(color)}[$Black($Bold( ${level.show} ))]"

  given Message is Inscribable in Display as ansiStandard = (level, realm, timestamp, message) =>
    import textMetrics.uniform

    e"$Gray(${dateFormat.format(timestamp)}) $level $Chocolate(${realm.name.fit(10)}) > $message\n"
