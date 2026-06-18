import Foundation

final class DummyUtterance {}

func expectEqual<T: Equatable>(_ actual: T, _ expected: T, _ message: String) {
  if actual != expected {
    fatalError("\(message): expected \(expected), got \(actual)")
  }
}

func expectNil<T>(_ actual: T?, _ message: String) {
  if actual != nil {
    fatalError("\(message): expected nil, got \(String(describing: actual))")
  }
}

func testRestartIgnoresStaleFinishAndRestartsSamePosition() {
  let session = MealSpeechPlaybackSession()
  let oldUtterance = DummyUtterance()

  session.replace(position: 0)
  session.beginUtterance(oldUtterance, position: 0)
  session.requestRestartAfterStop()

  expectEqual(
    session.handleFinish(oldUtterance, stepCount: 3),
    .restart(position: 0),
    "stale finish after rate restart should restart the same step"
  )

  let restartedUtterance = DummyUtterance()
  session.beginUtterance(restartedUtterance, position: 0)

  expectNil(session.stepPositionForStart(oldUtterance), "old utterance start must not update the UI anchor")
  expectEqual(
    session.stepPositionForStart(restartedUtterance),
    0,
    "restarted utterance should keep the original step anchor"
  )
}

func testRepeatedStaleEventsDoNotLoopAfterRestartIsConsumed() {
  let session = MealSpeechPlaybackSession()
  let oldUtterance = DummyUtterance()

  session.replace(position: 1)
  session.beginUtterance(oldUtterance, position: 1)
  session.requestRestartAfterStop()

  expectEqual(
    session.handleCancel(oldUtterance),
    .restart(position: 1),
    "first stale cancel should trigger the pending restart"
  )
  expectEqual(
    session.handleFinish(oldUtterance, stepCount: 4),
    .ignore,
    "later stale finish should be ignored after restart was consumed"
  )
}

func testAcceptedFinishAdvancesFromAcceptedUtteranceOnly() {
  let session = MealSpeechPlaybackSession()
  let firstUtterance = DummyUtterance()
  let secondUtterance = DummyUtterance()

  session.replace(position: 0)
  session.beginUtterance(firstUtterance, position: 0)
  session.beginUtterance(secondUtterance, position: 1)

  expectEqual(
    session.handleFinish(firstUtterance, stepCount: 3),
    .ignore,
    "an older utterance must not advance the current step"
  )
  expectEqual(
    session.handleFinish(secondUtterance, stepCount: 3),
    .speakNext(position: 2),
    "the accepted utterance should advance by one step"
  )
}

@main
struct MealSpeechSessionTestRunner {
  static func main() {
    testRestartIgnoresStaleFinishAndRestartsSamePosition()
    testRepeatedStaleEventsDoNotLoopAfterRestartIsConsumed()
    testAcceptedFinishAdvancesFromAcceptedUtteranceOnly()
    print("meal speech session tests passed")
  }
}
