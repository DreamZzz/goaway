import Foundation
import AVFoundation
import React

private struct MealSpeechStep {
  let index: Int
  let content: String
}

@objc(MealSpeechSynthesizer)
final class MealSpeechSynthesizer: RCTEventEmitter, AVSpeechSynthesizerDelegate {
  private let synthesizer = AVSpeechSynthesizer()
  private let playbackSession = MealSpeechPlaybackSession()
  private var recipeId: String?
  private var steps: [MealSpeechStep] = []
  private var locale = "zh-CN"
  private var displayRate: Float = 1.0
  private var hasListeners = false

  override init() {
    super.init()
    synthesizer.delegate = self
  }

  @objc
  override static func requiresMainQueueSetup() -> Bool {
    true
  }

  override func supportedEvents() -> [String]! {
    ["mealSpeechStateChanged"]
  }

  override func startObserving() {
    hasListeners = true
  }

  override func stopObserving() {
    hasListeners = false
  }

  @objc(start:resolver:rejecter:)
  func start(_ payload: NSDictionary,
             resolver resolve: @escaping RCTPromiseResolveBlock,
             rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      do {
        try self.configureAudioSession()
        let wasActive = self.synthesizer.isSpeaking || self.synthesizer.isPaused
        try self.prepareSpeech(from: payload)
        if wasActive {
          self.playbackSession.requestRestartAfterStop()
          let stopped = self.synthesizer.stopSpeaking(at: .immediate)
          if stopped {
            self.schedulePendingRestartFallback()
            resolve(["ok": true])
            return
          }
        }
        self.speakCurrentStep()
        resolve(["ok": true])
      } catch {
        self.emit(state: "error", error: error.localizedDescription)
        reject("meal_speech_start_failed", error.localizedDescription, error)
      }
    }
  }

  @objc(pause:rejecter:)
  func pause(_ resolve: @escaping RCTPromiseResolveBlock,
             rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      if self.synthesizer.isSpeaking {
        _ = self.synthesizer.pauseSpeaking(at: .word)
      }
      self.emit(state: "paused", stepIndex: self.currentStepIndex())
      resolve(["ok": true])
    }
  }

  @objc(resume:rejecter:)
  func resume(_ resolve: @escaping RCTPromiseResolveBlock,
              rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      if self.synthesizer.isPaused {
        _ = self.synthesizer.continueSpeaking()
      } else if !self.synthesizer.isSpeaking, !self.steps.isEmpty {
        self.speakCurrentStep()
      }
      self.emit(state: "speaking", stepIndex: self.currentStepIndex())
      resolve(["ok": true])
    }
  }

  @objc(stop:rejecter:)
  func stop(_ resolve: @escaping RCTPromiseResolveBlock,
            rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      if self.synthesizer.isSpeaking || self.synthesizer.isPaused {
        self.playbackSession.requestStop(reportCancel: true)
        _ = self.synthesizer.stopSpeaking(at: .immediate)
      } else {
        self.playbackSession.requestStop(reportCancel: false)
        self.emit(state: "idle")
      }
      self.deactivateAudioSession()
      resolve(["ok": true])
    }
  }

  @objc(seekToStep:resolver:rejecter:)
  func seekToStep(_ stepIndex: NSNumber,
                  resolver resolve: @escaping RCTPromiseResolveBlock,
                  rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      guard let position = self.position(for: stepIndex.intValue) else {
        reject("meal_speech_step_not_found", "Step not found", nil)
        return
      }
      self.playbackSession.move(to: position)
      self.restartFromCurrentStep()
      resolve(["ok": true])
    }
  }

  @objc(setRate:resolver:rejecter:)
  func setRate(_ rate: NSNumber,
               resolver resolve: @escaping RCTPromiseResolveBlock,
               rejecter reject: @escaping RCTPromiseRejectBlock) {
    DispatchQueue.main.async {
      self.displayRate = self.normalizedDisplayRate(rate.floatValue)
      if self.synthesizer.isSpeaking || self.synthesizer.isPaused {
        self.restartFromCurrentStep()
      } else {
        self.emit(state: "idle", stepIndex: self.currentStepIndex())
      }
      resolve(["ok": true])
    }
  }

  private func prepareSpeech(from payload: NSDictionary) throws {
    guard let rawRecipeId = payload["recipeId"] else {
      throw NSError(domain: "MealSpeechSynthesizer", code: 1, userInfo: [
        NSLocalizedDescriptionKey: "Missing recipeId",
      ])
    }
    guard let rawSteps = payload["steps"] as? [[String: Any]] else {
      throw NSError(domain: "MealSpeechSynthesizer", code: 2, userInfo: [
        NSLocalizedDescriptionKey: "Missing steps",
      ])
    }

    let parsedSteps = rawSteps.enumerated().compactMap { offset, item -> MealSpeechStep? in
      let content = (item["content"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
      guard !content.isEmpty else {
        return nil
      }
      let index = (item["index"] as? NSNumber)?.intValue ?? offset + 1
      return MealSpeechStep(index: index, content: content)
    }
    guard !parsedSteps.isEmpty else {
      throw NSError(domain: "MealSpeechSynthesizer", code: 3, userInfo: [
        NSLocalizedDescriptionKey: "No readable steps",
      ])
    }

    recipeId = String(describing: rawRecipeId)
    steps = parsedSteps
    locale = (payload["locale"] as? String) ?? "zh-CN"
    displayRate = normalizedDisplayRate((payload["rate"] as? NSNumber)?.floatValue ?? 1.0)
    let requestedStartIndex = (payload["startIndex"] as? NSNumber)?.intValue ?? parsedSteps[0].index
    playbackSession.replace(position: position(for: requestedStartIndex) ?? 0)
  }

  private func speakCurrentStep() {
    speakStep(at: playbackSession.currentPosition)
  }

  private func speakStep(at position: Int) {
    guard position >= 0, position < steps.count else {
      emit(state: "finished")
      deactivateAudioSession()
      return
    }

    playbackSession.move(to: position)
    let step = steps[position]
    let utterance = AVSpeechUtterance(string: "第\(step.index)步。\(MealSpeechSynthesizer.speechFriendlyText(step.content))")
    utterance.voice = AVSpeechSynthesisVoice(language: locale)
    utterance.rate = nativeRate(for: displayRate)
    utterance.preUtteranceDelay = 0
    utterance.postUtteranceDelay = 0.15
    playbackSession.beginUtterance(utterance, position: position)
    synthesizer.speak(utterance)
    emit(state: "speaking", stepIndex: step.index)
  }

  private func restartFromCurrentStep() {
    if synthesizer.isSpeaking || synthesizer.isPaused {
      playbackSession.requestRestartAfterStop()
      let stopped = synthesizer.stopSpeaking(at: .immediate)
      if stopped {
        schedulePendingRestartFallback()
        return
      }
    }
    speakCurrentStep()
  }

  private func currentStepIndex() -> Int? {
    let currentPosition = playbackSession.currentPosition
    guard currentPosition >= 0, currentPosition < steps.count else {
      return nil
    }
    return steps[currentPosition].index
  }

  private func position(for stepIndex: Int) -> Int? {
    steps.firstIndex { $0.index == stepIndex }
  }

  private func normalizedDisplayRate(_ value: Float) -> Float {
    min(1.2, max(0.8, value))
  }

  // 烹饪场景里 "5g" "100ml" 这类单位被 AVSpeechSynthesizer 默认按英文字母朗读
  // （"五-G"、"一百-M-L"），听感不友好。这里把数字后紧跟的常见英文/符号单位
  // 替换为中文等价表达，仅影响朗读，UI 上的文本 / 复制 / 分享数据完全不动。
  //
  // 边界用 `(?![a-zA-Z])` 而不是 `\b`：NSRegularExpression（ICU）按 UAX#29 把
  // CJK 字符判为 word，所以 `g` + `搅` 之间没有 \b 边界，导致 "2g搅拌" 这种
  // CJK 紧贴单位的情形匹配失败（"2g 搅拌" 有空格则成功）。改用 lookahead 后
  // 只要单位后不是英文字母就视为合法边界，避免误伤 "2gan"（拼音）类输入。
  //
  // 顺序很重要：长前缀（kg/mg/mL/cm/mm）必须在短前缀（g/L）之前匹配。
  static func speechFriendlyText(_ text: String) -> String {
    var result = text
    let rules: [(String, String)] = [
      (#"(\d+(?:\.\d+)?)\s*kg(?![a-zA-Z])"#, "$1千克"),
      (#"(\d+(?:\.\d+)?)\s*mg(?![a-zA-Z])"#, "$1毫克"),
      (#"(\d+(?:\.\d+)?)\s*g(?![a-zA-Z])"#,  "$1克"),
      (#"(\d+(?:\.\d+)?)\s*mL(?![a-zA-Z])"#, "$1毫升"),
      (#"(\d+(?:\.\d+)?)\s*L(?![a-zA-Z])"#,  "$1升"),
      (#"(\d+(?:\.\d+)?)\s*°C(?![a-zA-Z])"#, "$1摄氏度"),
      (#"(\d+(?:\.\d+)?)\s*℃"#,              "$1摄氏度"),
      (#"(\d+)\s*min(?![a-zA-Z])"#,          "$1分钟"),
      (#"(\d+(?:\.\d+)?)\s*cm(?![a-zA-Z])"#, "$1厘米"),
      (#"(\d+(?:\.\d+)?)\s*mm(?![a-zA-Z])"#, "$1毫米"),
      // "长" 多音字消歧：烹饪场景里跟在长度单位前后的"长"几乎一定是 cháng
      // （长度），但 iOS 内置中文 TTS 默认读 zhǎng（生长）。把单字"长"替换为
      // 双字词"长度"消歧，TTS 读"长度"时一定是 cháng dù，不会再猜。
      // skip 复合词"长度/长大/长成"避免重复替换。
      // 长 multi-pronunciation 消歧仅覆盖最高频的"长 + 长度单位"形式；
      // 其他多音字（重/调/没/着/干/还）等真实 badcase 累积后再统一上 IPA
      // 注释方案（见 tech-debt P1-9）。
      (#"(\d+(?:\.\d+)?\s*(?:厘米|毫米))\s*长(?![度大成])"#, "$1 长度"),
      (#"长(?![度大成])\s*(\d+(?:\.\d+)?\s*(?:厘米|毫米))"#, "长度 $1"),
    ]
    for (pattern, repl) in rules {
      guard let re = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else { continue }
      let range = NSRange(result.startIndex..., in: result)
      result = re.stringByReplacingMatches(in: result, options: [], range: range, withTemplate: repl)
    }
    return result
  }

  private func nativeRate(for rate: Float) -> Float {
    let native = AVSpeechUtteranceDefaultSpeechRate * rate
    return min(AVSpeechUtteranceMaximumSpeechRate, max(AVSpeechUtteranceMinimumSpeechRate, native))
  }

  private func configureAudioSession() throws {
    let session = AVAudioSession.sharedInstance()
    try session.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
    try session.setActive(true, options: [])
  }

  private func deactivateAudioSession() {
    try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
  }

  private func schedulePendingRestartFallback() {
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) {
      guard self.playbackSession.hasPendingRestart,
            !self.synthesizer.isSpeaking,
            !self.synthesizer.isPaused,
            let position = self.playbackSession.consumePendingRestart() else {
        return
      }
      self.speakStep(at: position)
    }
  }

  private func emit(state: String, stepIndex: Int? = nil, error: String? = nil) {
    guard hasListeners else {
      return
    }
    var body: [String: Any] = [
      "state": state,
      "rate": displayRate,
    ]
    if let recipeId = recipeId {
      body["recipeId"] = recipeId
    }
    if let stepIndex = stepIndex {
      body["stepIndex"] = stepIndex
    }
    if let error = error {
      body["error"] = error
    }
    sendEvent(withName: "mealSpeechStateChanged", body: body)
  }

  func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didStart utterance: AVSpeechUtterance) {
    guard let position = playbackSession.stepPositionForStart(utterance),
          position >= 0,
          position < steps.count else {
      return
    }
    emit(state: "speaking", stepIndex: steps[position].index)
  }

  func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
    switch playbackSession.handleFinish(utterance, stepCount: steps.count) {
    case .speakNext(let position), .restart(let position):
      speakStep(at: position)
    case .finished(let position):
      let stepIndex = position >= 0 && position < steps.count ? steps[position].index : nil
      emit(state: "finished", stepIndex: stepIndex)
      deactivateAudioSession()
    case .ignore:
      break
    }
  }

  func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
    switch playbackSession.handleCancel(utterance) {
    case .restart(let position):
      speakStep(at: position)
    case .cancelled(let position):
      let stepIndex = position.flatMap { $0 >= 0 && $0 < steps.count ? steps[$0].index : nil }
      emit(state: "cancelled", stepIndex: stepIndex)
      deactivateAudioSession()
    case .ignore:
      break
    }
  }
}
