import Foundation

final class MealSpeechPlaybackSession {
  enum FinishAction: Equatable {
    case speakNext(position: Int)
    case finished(position: Int)
    case restart(position: Int)
    case ignore
  }

  enum CancelAction: Equatable {
    case restart(position: Int)
    case cancelled(position: Int?)
    case ignore
  }

  private struct Token {
    let id: ObjectIdentifier
    let generation: Int
    let position: Int
  }

  private(set) var currentPosition = 0
  private var generation = 0
  private var activeToken: Token?
  private var pendingRestartPosition: Int?
  private var shouldReportCancel = false

  var hasPendingRestart: Bool {
    pendingRestartPosition != nil
  }

  func replace(position: Int) {
    generation += 1
    currentPosition = position
    activeToken = nil
    pendingRestartPosition = nil
    shouldReportCancel = false
  }

  func move(to position: Int) {
    currentPosition = position
  }

  func beginUtterance(_ utterance: AnyObject, position: Int) {
    currentPosition = position
    activeToken = Token(
      id: ObjectIdentifier(utterance),
      generation: generation,
      position: position
    )
  }

  func stepPositionForStart(_ utterance: AnyObject) -> Int? {
    guard let token = activeToken,
          token.id == ObjectIdentifier(utterance),
          token.generation == generation else {
      return nil
    }
    return token.position
  }

  func requestRestartAfterStop() {
    generation += 1
    pendingRestartPosition = currentPosition
    activeToken = nil
    shouldReportCancel = false
  }

  func requestStop(reportCancel: Bool) {
    generation += 1
    pendingRestartPosition = nil
    activeToken = nil
    shouldReportCancel = reportCancel
  }

  func consumePendingRestart() -> Int? {
    guard let position = pendingRestartPosition else {
      return nil
    }
    pendingRestartPosition = nil
    return position
  }

  func handleFinish(_ utterance: AnyObject, stepCount: Int) -> FinishAction {
    if let position = consumePendingRestart() {
      return .restart(position: position)
    }

    guard let token = activeToken,
          token.id == ObjectIdentifier(utterance),
          token.generation == generation else {
      return .ignore
    }

    activeToken = nil
    currentPosition = token.position

    if token.position + 1 < stepCount {
      currentPosition = token.position + 1
      return .speakNext(position: currentPosition)
    }

    return .finished(position: token.position)
  }

  func handleCancel(_ utterance: AnyObject) -> CancelAction {
    if let position = consumePendingRestart() {
      return .restart(position: position)
    }

    if shouldReportCancel {
      shouldReportCancel = false
      activeToken = nil
      return .cancelled(position: currentPosition)
    }

    guard let token = activeToken,
          token.id == ObjectIdentifier(utterance),
          token.generation == generation else {
      return .ignore
    }

    activeToken = nil
    return .cancelled(position: token.position)
  }
}
