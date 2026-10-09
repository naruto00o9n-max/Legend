import Foundation
@main struct TypesetterChecks {
  static func main() {
    let engine = Typesetter(measure: { Double($0.utf16.count) })
    precondition(engine.box("  one\n two\tthree  ", width: 30, tatweel: false) == "one two three")
    precondition(engine.box("one two three four", width: 13, tatweel: false) == "one two\nthree four")
    precondition(engine.box("empty", width: 0, tatweel: true).isEmpty)
    precondition(engine.circle("one", width: 30, fontSize: 1) == "one")
    precondition(engine.normalized("\u{00A0}a\u{00A0}b\u{00A0}") == "a\u{00A0}b")
    let arabic = "هذا حوار عربي داخل فقاعة طويلة للتحقق من توزيع الكلمات"
    for text in [arabic, "one two three four five six seven eight nine ten", "براءة وتآزر", "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيمِ", "حوار\u{00A0}متصل", ""] {
      for width in [5.0, 12.0, 30.0, 800.0] {
        for result in [engine.box(text,width:width,tatweel:false), engine.box(text,width:width,tatweel:true), engine.circle(text,width:width,fontSize:1)] {
          // Compare scalar sequence: a Tatweel can carry a combining mark, so
          // deleting whole Swift grapheme clusters would also delete the mark.
          let strip: (String) -> [UInt32] = { value in
            value.unicodeScalars.filter { $0.value != 0x640 && !$0.properties.isWhitespace }.map(\.value)
          }
          precondition(strip(result)==strip(text), "Typesetting lost or reordered source characters")
        }
      }
    }
    print("PASS: whitespace normalization, box/circle balancing, no orphan line, no source character loss.")
  }
}
