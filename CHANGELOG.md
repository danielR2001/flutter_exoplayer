# Changelog

## 0.5.2

- Fixed minor bugs (thanks, @btanarola)

## 0.5.1

- Fixed hiding notification when player stopped.
- Updated notification managment code efficiency.

## 0.5.0

- Added two customizable actions with callback in addition to the default ones.
- Changed NotificationActionMode enum to NotificationDefaultActions.
- Updated example app.

## 0.4.0

- Added protection from notification errors.
- Fixed seek position when paused (it would set the state to playing).
- Fixed getCurrentPosition.

## 0.3.5

- Added getVolume feature.
- Fixed some bugs.

## 0.3.4

- Changed error handling to act as dispose method.
- Fixed some bugs.

## 0.3.3

- Fixed getCurrentPlayingAudioIndex.

## 0.3.2

- Fixed next and previous actions not working when paused.

## 0.3.1

- Fixed seekPosition and seekIndex errors in foregroundPlayer.

## 0.3.0

- Renamed seek to seekPosition.
- Added seekIndex feature that lets you seek to a specific index in playlist (available only when playing playlist).
- Added index parameter to playAll, that indicates from what index to start playing.

## 0.2.1

- Updated code efficiency.
- Fixed some minor bugs.

## 0.2.0

- Fixed player state handling completly.
- Changed the behavior of the `COMPLETED` state to act similarly as `PAUSED` state.

## 0.1.0

- Fixed player state handling.
- Added SetRepeatMode feature.

## 0.0.2

- Changed the class name, Exoplayer => Audioplayer.
- Added custom notification callback via stream (Dart side) in addition to the default (only Java side). 

## 0.0.1

- Initial Open Source release.


