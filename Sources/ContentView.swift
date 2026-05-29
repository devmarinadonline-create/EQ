import SwiftUI

struct ContentView: View {
    @StateObject private var store = PresetStore()
    @StateObject private var audio = AudioEngine()
    @State private var tab = 0
    @State private var showPicker = false

    var body: some View {
        ZStack {
            Color(white: 0.05).ignoresSafeArea()

            VStack(spacing: 0) {
                // Header
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("EQ")
                            .font(.system(size: 34, weight: .black))
                            .foregroundColor(.white)
                        if let id = audio.currentPresetId {
                            Text(id)
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.green)
                                .lineLimit(1)
                        }
                        if !audio.currentFileName.isEmpty {
                            Text(audio.currentFileName)
                                .font(.system(size: 10))
                                .foregroundColor(.gray)
                                .lineLimit(1)
                        }
                    }
                    Spacer()
                    Button(action: audio.resetEQ) {
                        Label("Reset", systemImage: "arrow.counterclockwise")
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(.gray)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)
                .padding(.bottom, 12)

                // EQ Curve
                EQCurveView(frequencies: audio.frequencies, gains: audio.gains)
                    .frame(height: 160)
                    .padding(.horizontal, 16)
                    .animation(.easeInOut(duration: 0.3), value: audio.gains)

                // Tab picker
                Picker("View", selection: $tab) {
                    Text("Presets").tag(0)
                    Text("Bands").tag(1)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)

                // Content
                if tab == 0 {
                    PresetListView(
                        presets: store.presets,
                        selectedId: audio.currentPresetId,
                        onSelect: { audio.apply($0) }
                    )
                    .transition(.opacity)
                } else {
                    BandControlsView(
                        frequencies: audio.frequencies,
                        gains: $audio.gains,
                        onChange: audio.setBand
                    )
                    .transition(.opacity)
                }

                Spacer(minLength: 0)

                // Player
                PlayerBar(
                    isPlaying: audio.isPlaying,
                    hasFile: audio.hasFile,
                    onToggle: audio.togglePlay,
                    onOpen: { showPicker = true }
                )
                .padding(.bottom, 20)
            }
        }
        .preferredColorScheme(.dark)
        .animation(.easeInOut(duration: 0.2), value: tab)
        .sheet(isPresented: $showPicker) {
            DocumentPicker { url in audio.load(url) }
        }
    }
}

// MARK: - Player Bar

struct PlayerBar: View {
    let isPlaying: Bool
    let hasFile: Bool
    let onToggle: () -> Void
    let onOpen: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            Button(action: onOpen) {
                Label(
                    hasFile ? "Change File" : "Open Audio File",
                    systemImage: "music.note"
                )
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(.white)
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
                .background(Color(white: 0.16))
                .clipShape(Capsule())
            }

            if hasFile {
                Button(action: onToggle) {
                    Image(systemName: isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .font(.system(size: 52))
                        .foregroundColor(.green)
                        .shadow(color: .green.opacity(0.4), radius: 8)
                }
                .transition(.scale.combined(with: .opacity))
            }
        }
        .animation(.spring(response: 0.3), value: hasFile)
    }
}
