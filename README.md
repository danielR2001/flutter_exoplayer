# ExoPlayer

A Flutter plugin that let's you play multiple audio files simultaneously with an option to choose if to play in background or as a forground service,for now works only for Android.

## Install

just add this dependency in your pubsec.yaml file:

```yaml
dependencies:
  exoplayer: ^0.0.1
```

## Usage

An `ExoPlayer` instance can play a single audio at a time. To create it, simply call the constructor:

```dart
    ExoPlayer exoPlayer = ExoPlayer();
```

You can create multiple instances to play audio simultaneously, but only if you choose `playerMode: PlayerMode.BACKGROUND`, because android can't run two similar services.

For all methods that return a `Future<int>`: that's the status of the operation. If `1`, the operation was successful. Otherwise it's the platform native error code.

Logs are disable by default! To debug, run:

```dart
    ExoPlayer.logEnabled = true;
```

### Playing Audio

TODO: Playing Audio

### Controlling

TODO: Controlling

### Notification Customization

TODO: Notification Customization

### Streams

The AudioPlayer supports subscribing to events like so:

#### Duration Event

This event returns the duration of the file, when it's available (it might take a while because it's being downloaded or buffered).

```dart
  exoPlayer.onDurationChanged.listen((Duration d) {
    print('Max duration: $d');
    setState(() => duration = d);
  });
```

#### Position Event

This Event updates the current position of the audio. You can use it to make a progress bar, for instance.

```dart
  exoPlayer.onAudioPositionChanged.listen((Duration  p) => {
    print('Current position: $p');
    setState(() => position = p);
  });
```

#### State Event

This Event returns the current player state. You can use it to show if player playing, or stopped, or paused.

```dart
  exoPlayer.onPlayerStateChanged.listen((PlayerState s) => {
    print('Current player state: $s');
    setState(() => palyerState = s);
  });
```

#### Completion Event

This Event is called when the audio finishes playing; it's used in the loop method, for instance.

It does not fire when you interrupt the audio with pause or stop.

```dart
  exoPlayer.onPlayerCompletion.listen((event) {
    onComplete();
    setState(() {
      position = duration;
    });
  });
```

#### Error Event

This is called when an unexpected error is thrown in the native code.

```dart
  exoPlayer.onPlayerError.listen((msg) {
    print('audioPlayer error : $msg');
    setState(() {
      playerState = PlayerState.stopped;
      duration = Duration(seconds: 0);
      position = Duration(seconds: 0);
    });
  });
```

## Supported Formats

You can check a list of supported formats below:

 - [Android](https://exoplayer.dev/supported-formats.html)

## Credits

This project was originally a fork of [luanpotter's audioplayers](https://github.com/luanpotter/audioplayers) that was also originally a fork of [rxlabz's audioplayer](https://github.com/rxlabz/audioplayer), but since we have diverged and added more features.

Thanks for @rxlabz and @luanpotter for the amazing work!
