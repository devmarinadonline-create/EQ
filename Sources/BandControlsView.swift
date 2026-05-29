import SwiftUI

struct BandControlsView: View {
    let frequencies: [Float]
    @Binding var gains: [Float]
    let onChange: (Int, Float) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(alignment: .center, spacing: 6) {
                ForEach(Array(frequencies.enumerated()), id: \.offset) { i, freq in
                    VerticalBandSlider(
                        label: freqLabel(freq),
                        gain: Binding(
                            get: { i < gains.count ? gains[i] : 0 },
                            set: { onChange(i, $0) }
                        )
                    )
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
        }
        .frame(height: 220)
    }

    private func freqLabel(_ f: Float) -> String {
        f >= 1000
            ? String(format: f.truncatingRemainder(dividingBy: 1000) == 0 ? "%.0fk" : "%.1fk", f / 1000)
            : String(format: "%.0f", f)
    }
}

private struct VerticalBandSlider: View {
    let label: String
    @Binding var gain: Float
    @State private var dragStart: Float = 0

    var body: some View {
        VStack(spacing: 4) {
            Text(gain >= 0 ? "+\(Int(gain.rounded()))" : "\(Int(gain.rounded()))")
                .font(.system(size: 10, weight: .semibold, design: .monospaced))
                .foregroundColor(gain == 0 ? .gray : (gain > 0 ? .green : .orange))
                .frame(width: 36)

            ZStack {
                // Track background
                Capsule()
                    .fill(Color(white: 0.18))
                    .frame(width: 4, height: 140)

                // Zero tick
                Rectangle()
                    .fill(Color.white.opacity(0.5))
                    .frame(width: 12, height: 1)

                // Fill bar
                let fillH = CGFloat(abs(gain) / 30) * 70
                Capsule()
                    .fill(gain >= 0 ? Color.green : Color.orange)
                    .frame(width: 4, height: max(fillH, 1))
                    .offset(y: gain >= 0 ? -fillH / 2 : fillH / 2)

                // Thumb
                Circle()
                    .fill(Color.white)
                    .frame(width: 18, height: 18)
                    .shadow(color: .black.opacity(0.4), radius: 2, y: 1)
                    .offset(y: -CGFloat(gain / 30) * 70)
            }
            .frame(width: 36, height: 140)
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { drag in
                        let delta = Float(-drag.translation.height / 140) * 30
                        gain = max(-30, min(30, dragStart + delta))
                    }
                    .onEnded { _ in dragStart = gain }
            )
            .onTapGesture(count: 2) {
                gain = 0
                dragStart = 0
            }

            Text(label)
                .font(.system(size: 10))
                .foregroundColor(.gray)
                .frame(width: 36)
        }
        .frame(width: 36)
    }
}
