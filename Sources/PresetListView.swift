import SwiftUI

struct PresetListView: View {
    let presets: [Preset]
    let selectedId: String?
    let onSelect: (Preset) -> Void

    var body: some View {
        ScrollView {
            FlowLayout(spacing: 7) {
                ForEach(presets) { preset in
                    let selected = preset.id == selectedId
                    Button(action: { onSelect(preset) }) {
                        Text(preset.id)
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(selected ? .black : .white)
                            .lineLimit(1)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 7)
                            .background(selected ? Color.green : Color(white: 0.15))
                            .cornerRadius(7)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
    }
}

// MARK: - Flow Layout (wrapping, like HTML flex-wrap)

struct FlowLayout: Layout {
    var spacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 0
        var y: CGFloat = 0
        var x: CGFloat = 0
        var rowH: CGFloat = 0

        for v in subviews {
            let s = v.sizeThatFits(.unspecified)
            if x + s.width > width, x > 0 {
                y += rowH + spacing
                x = 0; rowH = 0
            }
            x += s.width + spacing
            rowH = max(rowH, s.height)
        }
        return CGSize(width: width, height: y + rowH)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowH: CGFloat = 0

        for v in subviews {
            let s = v.sizeThatFits(.unspecified)
            if x + s.width > bounds.maxX, x > bounds.minX {
                y += rowH + spacing
                x = bounds.minX; rowH = 0
            }
            v.place(at: CGPoint(x: x, y: y), proposal: .unspecified)
            x += s.width + spacing
            rowH = max(rowH, s.height)
        }
    }
}
