import Foundation

struct Preset: Identifiable, Hashable {
    let id: String
    var frequencies: [Float]
    var gains: [Float]
    var qs: [Float]

    static let flat = Preset(
        id: "__flat__",
        frequencies: [20, 40, 80, 160, 320, 640, 1280, 2560, 5120, 10240, 20000],
        gains: Array(repeating: 0, count: 11),
        qs: Array(repeating: 0.7071, count: 11)
    )
}

final class PresetStore: ObservableObject {
    @Published var presets: [Preset] = []

    init() { load() }

    private struct RawPreset: Decodable {
        let frequencies: [Float]
        let gains: [Float]
        let qs: [Float]
    }

    private func load() {
        guard
            let url = Bundle.main.url(forResource: "EarsAudioToolkitPresets", withExtension: "json"),
            let data = try? Data(contentsOf: url),
            let raw = try? JSONDecoder().decode([String: RawPreset].self, from: data)
        else { return }

        presets = raw.map { name, p in
            Preset(id: name, frequencies: p.frequencies, gains: p.gains, qs: p.qs)
        }
        .sorted { $0.id.lowercased() < $1.id.lowercased() }
    }
}
