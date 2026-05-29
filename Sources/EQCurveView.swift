import SwiftUI

struct EQCurveView: View {
    let frequencies: [Float]
    let gains: [Float]

    private static let minF: Float = 20
    private static let maxF: Float = 20000
    private static let minG: Float = -32
    private static let maxG: Float = 32

    private func xRatio(_ f: Float) -> Float {
        (log10(f) - log10(Self.minF)) / (log10(Self.maxF) - log10(Self.minF))
    }

    private func yRatio(_ g: Float) -> Float {
        1 - (g - Self.minG) / (Self.maxG - Self.minG)
    }

    private func responseAt(_ f: Float) -> Float {
        var sum: Float = 0
        let sigma: Float = 0.85
        for i in 0..<min(frequencies.count, gains.count) {
            let f0 = max(frequencies[i], 1)
            let d = log2(f / f0) / sigma
            sum += gains[i] * exp(-0.5 * d * d)
        }
        return max(Self.minG, min(Self.maxG, sum))
    }

    private func curvePoints(_ size: CGSize, steps: Int = 300) -> [CGPoint] {
        (0...steps).map { i in
            let t = Float(i) / Float(steps)
            let logF = log10(Self.minF) + t * (log10(Self.maxF) - log10(Self.minF))
            let g = responseAt(pow(10, logF))
            return CGPoint(x: Double(t) * size.width, y: Double(yRatio(g)) * size.height)
        }
    }

    var body: some View {
        Canvas { ctx, size in
            // Gain grid lines
            let gainLines: [Float] = [-20, -10, 0, 10, 20]
            for g in gainLines {
                let y = CGFloat(yRatio(g)) * size.height
                var p = Path()
                p.move(to: CGPoint(x: 0, y: y))
                p.addLine(to: CGPoint(x: size.width, y: y))
                ctx.stroke(p, with: .color(.white.opacity(g == 0 ? 0.3 : 0.1)),
                           lineWidth: g == 0 ? 1 : 0.5)
            }

            // Freq grid lines
            for f: Float in [100, 1000, 10000] {
                let x = CGFloat(xRatio(f)) * size.width
                var p = Path()
                p.move(to: CGPoint(x: x, y: 0))
                p.addLine(to: CGPoint(x: x, y: size.height))
                ctx.stroke(p, with: .color(.white.opacity(0.1)), lineWidth: 0.5)
            }

            let pts = curvePoints(size)
            guard pts.count > 1 else { return }

            // Fill under curve
            let zeroY = CGFloat(yRatio(0)) * size.height
            var fill = Path()
            fill.move(to: CGPoint(x: pts[0].x, y: zeroY))
            pts.forEach { fill.addLine(to: $0) }
            fill.addLine(to: CGPoint(x: pts.last!.x, y: zeroY))
            fill.closeSubpath()
            ctx.fill(fill, with: .linearGradient(
                Gradient(colors: [Color.green.opacity(0.35), Color.green.opacity(0.0)]),
                startPoint: CGPoint(x: 0, y: 0), endPoint: CGPoint(x: 0, y: size.height)
            ))

            // Curve line
            var line = Path()
            line.move(to: pts[0])
            pts.dropFirst().forEach { line.addLine(to: $0) }
            ctx.stroke(line, with: .color(.green), lineWidth: 2.5)

            // Band dots
            for i in 0..<min(frequencies.count, gains.count) {
                guard frequencies[i] >= Self.minF else { continue }
                let x = CGFloat(xRatio(frequencies[i])) * size.width
                let y = CGFloat(yRatio(gains[i])) * size.height
                let r = CGRect(x: x - 4, y: y - 4, width: 8, height: 8)
                ctx.fill(Path(ellipseIn: r), with: .color(.white))
                ctx.stroke(Path(ellipseIn: r), with: .color(.green), lineWidth: 1.5)
            }
        }
        .background(Color(white: 0.09))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
        .overlay(alignment: .bottom) {
            // Freq labels
            GeometryReader { geo in
                let labels: [(Float, String)] = [
                    (20,"20"), (100,"100"), (500,"500"),
                    (1000,"1k"), (5000,"5k"), (20000,"20k")
                ]
                ForEach(labels, id: \.0) { f, label in
                    Text(label)
                        .font(.system(size: 8))
                        .foregroundColor(.gray)
                        .position(x: CGFloat(xRatio(f)) * geo.size.width, y: 7)
                }
            }
            .frame(height: 14)
        }
    }
}
