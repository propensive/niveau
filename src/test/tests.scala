/*
    Adversaria, version 0.4.0. Copyright 2019-23 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package adversaria

import probably.*
import rudiments.*
import gossamer.*
import annotation.StaticAnnotation

import unsafeExceptions.canThrowAny

final case class id() extends StaticAnnotation
final case class count(number: Int) extends StaticAnnotation
final case class ref(x: Int) extends StaticAnnotation

case class Person(name: Text, @id email: Text)

@count(10)
case class Company(name: Text)

case class Employee(person: Person, @id code: Long)

case class Letters(@ref(1) alpha: Int, @ref(2) @ref(3) beta: Int, gamma: Int, @ref(4) delta: Int)

object Tests extends Suite(t"Adversaria tests"):

  def run(): Unit =

    test(t"first field") {
      val letters = Letters(5, 6, 7, 8)
      Annotations.firstField[Letters, ref](letters)
    }.assert(_ == 5)

    // test("access field annotations") {
    //   Annotations.field[Employee](_.code)
    // }.assert(_ == List(id()))
    
    test(t"check nonexistant annotations") {
      Annotations.field[Employee](_.person)
    }.assert(_ == Nil)

    // test("get field values") {
    //   val letters = Letters(5, 6, 7, 8)
    //   Annotations.fields[Letters, ref].map(_(letters))
    // }.assert(_ == List(5, 6, 6, 8))
    
    // test("get field annotations") {
    //   val letters = Letters(5, 6, 7, 8)
    //   Annotations.fields[Letters, ref].map(_.annotation)
    // }.assert(_ == List(ref(1), ref(2), ref(3), ref(4)))

    // test("get field names") {
    //   val letters = Letters(5, 6, 7, 8)
    //   Annotations.fields[Letters, ref].map(_.name)
    // }.assert(_ == List("alpha", "beta", "beta", "delta"))

    // test("get annotations on type") {
    //   implicitly[TypeMetadata[Company]].annotations
    // }.assert(_ == List(count(10)))

    // test("get the short name of the type") {
    //   implicitly[TypeMetadata[Person]].typeName
    // }.assert(_ == "Person")
    
    // test("get the full name of the type") {
    //   implicitly[TypeMetadata[Person]].fullTypeName
    // }.assert(_ == "adversaria.tests.Person")
    
    // test("find the field with a particular annotation") {
    //   val ann = implicitly[FindMetadata[id, Person]]
    //   val person = Person("John Smith", "test@example.com")
    //   ann.get(person)
    // }.assert(_ == "test@example.com")
    
    // test("check the name of the field found by an annotation") {
    //   implicitly[FindMetadata[id, Person]].parameter.fieldName
    // }.assert(_ == "email")
    
    // test("check that implicit for missing annotation is not resolved") {
    //   scalac"implicitly[FindMetadata[id, Company]]"
    // }.assert(_ == TypecheckError("adversaria: could not find matching annotation"))

    // test("extract annotation value generically") {
    //   def getId[T](value: T)(implicit anns: FindMetadata[id, T]): Text =
    //     anns.get(value).toString

    //   getId(Employee(Person("John Smith", "test@example.com"), 3141592))
    // }.assert(_ == "3141592")
