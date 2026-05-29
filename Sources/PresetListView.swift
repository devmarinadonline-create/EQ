import SwiftUI

struct PresetListView: View {
    let presets: [Preset]
    let selectedId: String?
    let onSelect: (Preset) -> Void

    private let columns = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(presets) { preset in
                    PresetCard(
                        preset: preset,
                        isSelected: preset.id == selectedId,
                        onTap: { onSelect(preset) }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 20)
        }
    }
}

private struct PresetCard: View {
    let preset: Preset
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 6) {
                Text(preset.id)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(isSelected ? .black : .white)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)
                    .frame(minHeight: 28, alignment: .topLeading)

                MiniEQBar(gains: preset.gains, inverted: isSelected)
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isSelected ? Color.green : Color(white: 0.13))
            .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
}

private struct MiniEQBar: View {
    let gains: [Float]
    let inverted: Bool

    var body: some View {
        Canvas { ctx, size in
            guard !gains.isEmpty else { return }
            let bw = size.width / CGFloat(gains.count)
            let mid = size.height / 2

            for (i, g) in gains.enumerated() {
                let h = CGFloat(abs(g) / 30) * mid
                let x = CGFloat(i) * bw
                let y = g >= 0 ? mid - h : mid
                let rect = CGRect(x: x + 0.5, y: y, width: max(bw - 1, 1), height: max(h, 1))
                let c: Color = inverted
                    ? (g >= 0 ? .black.opacity(0.55) : .black.opacity(0.35))
                    : (g >= 0 ? .green.opacity(0.85) : .orange.opacity(0.75))
                ctx.fill(Path(rect), with: .color(c))
            }

            var midLine = Path()
            midLine.move(to: CGPoint(x: 0, y: mid))
            midLine.addLine(to: CGPoint(x: size.width, y: mid))
            ctx.stroke(midLine, with: .color(inverted ? .black.opacity(0.3) : .white.opacity(0.2)),
                       lineWidth: 0.5)
        }
        .frame(height: 22)
    }
}
