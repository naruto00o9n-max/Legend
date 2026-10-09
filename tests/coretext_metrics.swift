import Foundation
import CoreText
import CoreGraphics

private struct AndroidFixture: Decodable {
    var font: String, text: String, box: String, boxTatweel: String, circle: String
    var fontSize: Double, width: Double, androidMeasuredWidth: Double
}
@main struct FontParityAudit {
    static func main() throws {
        guard CommandLine.arguments.count == 4 else { fatalError("Usage: font-audit AndroidFixtures FontDirectory Report") }
        let records = try JSONDecoder().decode([AndroidFixture].self, from: Data(contentsOf: URL(fileURLWithPath: CommandLine.arguments[1])))
        let directory = URL(fileURLWithPath: CommandLine.arguments[2])
        var loaded: [String: CGFont] = [:], results: [[String: Any]] = [], mismatches = 0, fractionalMismatches = 0
        for row in records {
            let graphicsFont: CGFont
            if let cached = loaded[row.font] { graphicsFont = cached }
            else {
                guard let provider = CGDataProvider(url: directory.appendingPathComponent(row.font) as CFURL), let face = CGFont(provider) else { throw NSError(domain: "FontParity",code:1,userInfo:[NSLocalizedDescriptionKey:"Cannot open original font \(row.font)"]) }
                graphicsFont = face; loaded[row.font] = face
                CTFontManagerRegisterFontsForURL(directory.appendingPathComponent(row.font) as CFURL, .process, nil)
            }
            let font = CTFontCreateWithGraphicsFont(graphicsFont, CGFloat(row.fontSize), nil, nil)
            let metrics = CoreTextMeasure(font: font)
            let measure: (String) -> Double = { metrics.width($0) }
            let engine = Typesetter(measure: measure)
            let box = engine.box(row.text,width:row.width,tatweel:false)
            let tatweel = engine.box(row.text,width:row.width,tatweel:true)
            let circle = engine.circle(row.text,width:row.width,fontSize:row.fontSize)
            let matches = box == row.box && tatweel == row.boxTatweel && circle == row.circle
            if !matches { mismatches += 1 }
            let fractional = Typesetter(measure: { metrics.width($0, pixelAligned: false) })
            let fractionalMatches = fractional.box(row.text,width:row.width,tatweel:false) == row.box
                && fractional.box(row.text,width:row.width,tatweel:true) == row.boxTatweel
                && fractional.circle(row.text,width:row.width,fontSize:row.fontSize) == row.circle
            if !fractionalMatches { fractionalMismatches += 1 }
            results.append(["font":row.font,"text":row.text,"boxMatches":box == row.box,"kashidaMatches":tatweel == row.boxTatweel,"circleMatches":circle == row.circle,"coreTextBox":box,"coreTextKashida":tatweel,"coreTextCircle":circle,"androidBox":row.box,"androidKashida":row.boxTatweel,"androidCircle":row.circle,"widthDelta":measure(row.text)-row.androidMeasuredWidth,"fractionalWidthDelta":metrics.width(row.text,pixelAligned:false)-row.androidMeasuredWidth,"fractionalFormattingMatches":fractionalMatches])
        }
        let report: [String: Any] = ["cases":records.count,"mismatches":mismatches,"fractionalMismatches":fractionalMismatches,"measurement":"whole-pixel glyph advances; raw CoreText retained as baseline","status":mismatches == 0 ? "text-formatting-match" : "port-needs-correction","renderPixelParity":"not covered by font metrics","details":results]
        let data = try JSONSerialization.data(withJSONObject:report,options:[.prettyPrinted,.sortedKeys])
        try data.write(to:URL(fileURLWithPath:CommandLine.arguments[3]))
        print("Font parity: \(records.count-mismatches)/\(records.count) matching formatting cases; inspect report before claiming parity.")
        if mismatches > 0 { exit(1) }
    }
}
