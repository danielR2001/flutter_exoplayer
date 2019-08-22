# Changelog

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


