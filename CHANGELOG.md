# Changelog

## 0.6.0

- **[Add]** Added Support to Null Safety.

## 0.5.5

- **[Add]** + **[Fix]** Added start from certain position (In Dart side was already implemented but not in Java side).

## 0.5.4

- **[Feature]** control of playback speed.

## 0.5.3

- **[Feature]** updating Notification metadata dynamically.

## 0.5.2

- **[Fix]** minor bugs (thanks, @btanarola)

## 0.5.1

- **[Fix]** hiding notification when player stopped.
- **[Update]** notification managment code efficiency.

## 0.5.0

- **[Add]** two customizable actions with callback in addition to the default ones.
- **[Change]** NotificationActionMode enum to NotificationDefaultActions.
- **[Update]** example app.

## 0.4.0

- **[Add]** protection from notification errors.
- **[Fix]** seek position when paused (it would set the state to playing).
- **[Fix]** getCurrentPosition.

## 0.3.5

- **[Feature]** getVolume.
- **[Fix]** some bugs.

## 0.3.4

- **[Change]** error handling to act as dispose method.
- **[Fix]** some bugs.

## 0.3.3

- **[Fix]** getCurrentPlayingAudioIndex.

## 0.3.2

- **[Fix]** next and previous actions not working when paused.

## 0.3.1

- **[Fix]** seekPosition and seekIndex errors in foregroundPlayer.

## 0.3.0

- **[Change]** Renamed seek to seekPosition.
- **[Feature]** seekIndex that lets you seek to a specific index in playlist (available only when playing playlist).
- **[Add]** index parameter to playAll, that indicates from what index to start playing.

## 0.2.1

- **[Update]** code efficiency.
- **[Fix]** some minor bugs.

## 0.2.0

- **[Fix]** player state handling completly.
- **[Change]** the behavior of the `COMPLETED` state to act similarly as `PAUSED` state.

## 0.1.0

- **[Fix]** player state handling.
- **[Feature]** SetRepeatMode.

## 0.0.2

- **[Change]** class name, Exoplayer => Audioplayer.
- **[Feature]** custom notification callback via stream (Dart side) in addition to the default (only Java side). 

## 0.0.1

- Initial Open Source release.


