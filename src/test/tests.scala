/*
    Telekinesis, version 0.4.0. Copyright 2022-23 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package telekinesis

import rudiments.*
import probably.*
import serpentine.*
import eucalyptus.*, logging.stdout
import gossamer.*, stdouts.stdout
import turbulence.*
import rudiments.*

import scala.collection.immutable.ListMap

import unsafeExceptions.canThrowAny


case class Address(house: Int, street: Text, city: Text, country: Text)
case class Person(name: Text, address: Address)

object Tests extends Suite(t"Telekinesis tests"):
  def run(using Runner): Unit =
    suite(t"Parsing tests"):
      test(t"parse Authority with username and password"):
        Authority.parse(t"username:password@example.com")
      .assert(_ == Authority(Host(t"example", t"com"), t"username:password"))
      
      test(t"parse Authority with username but not password"):
        Authority.parse(t"username@example.com")
      .assert(_ == Authority(Host(t"example", t"com"), t"username"))
      
      test(t"parse Authority with username, password and port"):
        Authority.parse(t"username:password@example.com:8080")
      .assert(_ == Authority(Host(t"example", t"com"), t"username:password", 8080))
      
      test(t"parse Authority with username and port"):
        Authority.parse(t"username@example.com:8080")
      .assert(_ == Authority(Host(t"example", t"com"), t"username", 8080))
      
      test(t"parse Authority with username, numerical password and port"):
        Authority.parse(t"username:1234@example.com:8080")
      .assert(_ == Authority(Host(t"example", t"com"), t"username:1234", 8080))

      test(t"Authority with invalid port fails"):
        capture(Authority.parse(t"username@example.com:no"))
      .matches:
        case InvalidUrlError(_, 21, InvalidUrlError.Expectation.Number) =>
      
      test(t"Parse full URI"):
        Url.parse(t"http://user:pw@example.com:8080/path/to/location?query=1#ref")
      .assert(_ == Url(Scheme.Http, Authority(Host(t"example", t"com"), t"user:pw", 8080),
          t"/path/to/location", t"query=1", t"ref"))
      
      test(t"Parse simple URI"):
        Url.parse(t"https://example.com/foo")
      .assert(_ == Url(Scheme.Https, Authority(Host(t"example", t"com")), t"/foo"))
      
      test(t"Show simple URI"):
        Url.parse(t"http://example.com/foo").show
      .assert(_ == t"http://example.com/foo")
      
      test(t"Parse full URL at compiletime"):
        url"http://user:pw@example.com:8080/path/to/location?query=1#ref"
      .assert(_ == Url(Scheme.Http, Authority(Host(t"example", t"com"), t"user:pw", 8080),
          t"/path/to/location", t"query=1", t"ref"))
      
      test(t"Parse FTP URL at compiletime"):
        url"ftp://user:pw@example.com:8080/path/to/location"
      .assert(_ == Url(Scheme(t"ftp"), Authority(Host(t"example", t"com"), t"user:pw", 8080),
          t"/path/to/location"))
      
      test(t"Parse URL at compiletime with substitution"):
        val port = 1234
        url"http://user:pw@example.com:$port/path/to/location"
      .assert(_ == Url(Scheme(t"http"), Authority(Host(t"example", t"com"), t"user:pw", 1234),
          t"/path/to/location"))
      
      test(t"Parse URL at compiletime with escaped substitution"):
        val message: Text = t"Hello world!"
        url"http://user:pw@example.com/$message"
      .assert(_ == Url(Scheme(t"http"), Authority(Host(t"example", t"com"), t"user:pw"), t"/Hello+world%21"))
      
      test(t"Parse URL at compiletime with unescaped substitution"):
        val message = Raw(t"Hello world!")
        url"http://user:pw@example.com/$message"
      .assert(_ == Url(Scheme(t"http"), Authority(Host(t"example", t"com"), t"user:pw"), t"/Hello world!"))
      
      test(t"Relative path is unescaped"):
        val message: Text = t"Hello world!"
        url"http://user:pw@example.com/$message/foo".path
      .assert(_ == Relative(0, List(t"Hello world!", t"foo")))
      
      test(t"Relative path with raw substitution is unescaped"):
        val message: Raw = Raw(t"Hello+world%21")
        url"http://user:pw@example.com/$message/foo".path
      .assert(_ == Relative(0, List(t"Hello world!", t"foo")))