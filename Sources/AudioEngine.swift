import AVFoundation
import MediaPlayer
import Darwin

final class AudioEngine: ObservableObject {
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let eq: AVAudioUnitEQ

    @Published var isPlaying = false
    @Published var hasFile = false
    @Published var gains: [Float] = Array(repeating: 0, count: 11)
    @Published var frequencies: [Float] = [20, 40, 80, 160, 320, 640, 1280, 2560, 5120, 10240, 20000]
    @Published var currentPresetId: String?
    @Published var currentFileName: String = ""

    private var currentFileURL: URL?

    init() {
        eq = AVAudioUnitEQ(numberOfBands: 11)
        configureSession()
        configureEQ()
        buildGraph()
        startEngine()
        setupRemoteCommands()
    }

    // MARK: - Setup

    private func configureSession() {
        do {
            let s = AVAudioSession.sharedInstance()
            // .playback enables background audio automatically
            try s.setCategory(.playback, mode: .default, options: [])
            try s.setActive(true)
        } catch {
            print("AVAudioSession: \(error)")
        }
    }

    private func configureEQ() {
        for (i, band) in eq.bands.enumerated() {
            band.filterType = i == 0 ? .lowShelf : (i == 10 ? .highShelf : .parametric)
            band.frequency = frequencies[i]
            band.gain = 0
            band.bandwidth = 1.9
            band.bypass = false
        }
    }

    private func buildGraph() {
        engine.attach(player)
        engine.attach(eq)
        engine.connect(player, to: eq, format: nil)
        engine.connect(eq, to: engine.mainMixerNode, format: nil)
    }

    private func startEngine() {
        guard !engine.isRunning else { return }
        do { try engine.start() } catch { print("Engine start: \(error)") }
    }

    // MARK: - Remote Command Center (Lock Screen / Control Center)

    private func setupRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()

        center.playCommand.addTarget { [weak self] _ in
            guard let self, self.hasFile else { return .noSuchContent }
            DispatchQueue.main.async {
                self.player.play()
                self.isPlaying = true
                self.updateNowPlaying()
            }
            return .success
        }

        center.pauseCommand.addTarget { [weak self] _ in
            guard let self else { return .commandFailed }
            DispatchQueue.main.async {
                self.player.pause()
                self.isPlaying = false
                self.updateNowPlaying()
            }
            return .success
        }

        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            guard let self else { return .commandFailed }
            DispatchQueue.main.async { self.togglePlay() }
            return .success
        }
    }

    private func updateNowPlaying() {
        var info = [String: Any]()
        info[MPMediaItemPropertyTitle] = currentFileName.isEmpty ? "EQ Player" : currentFileName
        info[MPMediaItemPropertyArtist] = currentPresetId.map { "Preset: \($0)" } ?? "EQ App"
        info[MPNowPlayingInfoPropertyPlaybackRate] = isPlaying ? 1.0 : 0.0
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
    }

    // MARK: - EQ

    func apply(_ preset: Preset) {
        currentPresetId = preset.id
        frequencies = preset.frequencies
        gains = preset.gains

        for (i, band) in eq.bands.enumerated() {
            guard i < preset.frequencies.count else { break }
            band.frequency = preset.frequencies[i]
            band.gain = preset.gains[i]
            let q = Double(preset.qs[i])
            let bw = Float(2.0 * Darwin.asinh(1.0 / (2.0 * q)) / Darwin.log(2.0))
            band.bandwidth = max(0.1, min(bw, 5.0))
        }
        updateNowPlaying()
    }

    func resetEQ() {
        currentPresetId = nil
        for i in 0..<eq.bands.count {
            eq.bands[i].gain = 0
            if i < gains.count { gains[i] = 0 }
        }
        updateNowPlaying()
    }

    func setBand(_ index: Int, gain: Float) {
        guard index < eq.bands.count, index < gains.count else { return }
        gains[index] = gain
        eq.bands[index].gain = gain
        currentPresetId = nil
    }

    // MARK: - Playback

    func load(_ url: URL) {
        let accessed = url.startAccessingSecurityScopedResource()

        let tmpURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString + "_" + url.lastPathComponent)

        do {
            try FileManager.default.copyItem(at: url, to: tmpURL)
        } catch {
            print("File copy: \(error)")
            if accessed { url.stopAccessingSecurityScopedResource() }
            return
        }
        if accessed { url.stopAccessingSecurityScopedResource() }

        player.stop()

        do {
            let file = try AVAudioFile(forReading: tmpURL)
            currentFileURL = tmpURL
            player.scheduleFile(file, at: nil) { [weak self] in
                DispatchQueue.main.async {
                    self?.isPlaying = false
                    self?.updateNowPlaying()
                }
            }
            if !engine.isRunning { try engine.start() }
            player.play()
            DispatchQueue.main.async {
                let name = url.deletingPathExtension().lastPathComponent
                self.currentFileName = name
                self.hasFile = true
                self.isPlaying = true
                self.updateNowPlaying()
            }
        } catch {
            print("Playback: \(error)")
        }
    }

    func togglePlay() {
        if isPlaying {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
        updateNowPlaying()
    }
}
