package hieroglyph

import probably.*
import rudiments.*
import larceny.*
// FIXME: resolution of overloaded `displayWidth` does not work
import gossamer.{displayWidth as _, *}

import textWidthCalculation.eastAsianScripts

object Tests extends Suite(t"Hieroglyph tests"):
  def run(): Unit =
    val japanese = t"平ぱ記動テ使村方島おゃぎむ万離ワ学つス携"
    val japaneseBytes = japanese.s.getBytes("UTF-8").nn.immutable(using Unsafe)
    
    suite(t"Character widths"):
      test(t"Check narrow character width"):
        'a'.displayWidth
      .assert(_ == 1)

      test(t"Check Japanese character width"):
        '身'.displayWidth
      .assert(_ == 2)

      test(t"Check displayWidth of string of Japanese text: \"平ぱ記...つス携\""):
        import gossamer.displayWidth
        japanese.displayWidth
      .assert(_ == 40)

    suite(t"Roundtrip decoding"):

      test(t"Decode Japanese from UTF-8"):
        import badEncodingHandlers.skip
        charDecoders.utf8.decode(japaneseBytes)
      .assert(_ == japanese)
    
      for chunk <- 1 to 25 do
        test(t"Decode Japanese text in chunks of size $chunk"):
          import badEncodingHandlers.skip
          charDecoders.utf8.decode(japaneseBytes.grouped(chunk).to(LazyList)).join
        .assert(_ == japanese)
      
      val badUtf8 = Bytes(45, -62, 49, 48)

      test(t"Decode invalid UTF-8 sequence, skipping errors"):
        import badEncodingHandlers.skip
        charDecoders.utf8.decode(badUtf8)
      .assert(_ == t"-10")
      
      test(t"Decode invalid UTF-8 sequence, substituting for a question mark"):
        import badEncodingHandlers.substitute
        charDecoders.utf8.decode(badUtf8)
      .assert(_ == t"-?10")
      
      test(t"Decode invalid UTF-8 sequence, throwing exception"):
        import unsafeExceptions.canThrowAny
        import badEncodingHandlers.strict
        capture[UndecodableCharError, Text](charDecoders.utf8.decode(badUtf8))
      .assert(_ == UndecodableCharError(1, enc"UTF-8"))
    
    suite(t"Compile-time tests"):
      test(t"Check that an invalid encoding produces an error"):
        captureCompileErrors(enc"ABCDEF").map(_.message)
      .assert(_ == List(t"hieroglyph: the encoding ABCDEF was not available"))
      
      test(t"Check that a non-encoding encoding does have a `decoder` method"):
        import badEncodingHandlers.skip
        captureCompileErrors(enc"ISO-2022-CN".decoder)
      .assert(_ == List())
      
      test(t"Check that a non-encoding encoding has no encoder method"):
        captureCompileErrors(enc"ISO-2022-CN".encoder)
      .assert(_.length == 1)
      
      test(t"Check that an encoding which can encode has an encoder method"):
        captureCompileErrors(enc"ISO-8859-1".encoder)
      .assert(_ == List())


