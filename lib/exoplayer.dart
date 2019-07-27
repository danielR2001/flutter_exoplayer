import 'dart:async';
import 'package:exoplayer/audio_object.dart';
import 'package:flutter/services.dart';

enum PlayerState {
  STOPPED,
  PLAYING,
  PAUSED,
  COMPLETED,
}
enum PlayerMode { FOREGROUND, BACKGROUND }

class ExoPlayer {
  static final MethodChannel _channel =
      const MethodChannel('com.daniel/exoplayer')
        ..setMethodCallHandler(platformCallHandler);
  static bool logEnabled = false;

  PlayerState _audioPlayerState;

  PlayerState get state => _audioPlayerState;

  set state(PlayerState state) {
    _playerStateController.add(state);
    _audioPlayerState = state;
  }

  final StreamController<PlayerState> _playerStateController =
      StreamController<PlayerState>.broadcast();

  final StreamController<Duration> _positionController =
      StreamController<Duration>.broadcast();

  final StreamController<void> _completionController =
      StreamController<void>.broadcast();

  final StreamController<String> _errorController =
      StreamController<String>.broadcast();

  /// Initializes ExoPlayer
  ///
  ExoPlayer() {
    state = PlayerState.STOPPED;
  }

  /// Plays an audio.
  ///
  /// If [exoPlayerMode] is set to [ExoPlayerMode.FOREGROUND], then you also need to pass:
  /// [audioObject] for providing the foreground notification.
  Future<int> play(
    String url, {
    double volume = 1.0,
    bool repeatMode = false,
    PlayerMode playerMode = PlayerMode.BACKGROUND,
    AudioObject audioObject,
  }) async {
    volume ??= 1.0;
    playerMode ??= PlayerMode.BACKGROUND;
    repeatMode ??= false;

    bool isBackground = true;
    int smallIcon;
    String title;
    String subTitle;
    String largeIconUrl;
    bool isLocal;
    if (playerMode == PlayerMode.FOREGROUND) {
      smallIcon = audioObject.getSmallIcon();
      title = audioObject.getTitle();
      subTitle = audioObject.getSubTitle();
      largeIconUrl = audioObject.getLargeIconUrl();
      isLocal = audioObject.getIsLocal();

      isBackground = false;
    }

    final int result = await _invokeMethod('play', {
      'url': url,
      'volume': volume,
      'repeatMode': repeatMode,
      'isBackground': isBackground,
      'smallIcon': smallIcon,
      'title': title,
      'subTitle': subTitle,
      'largeIconUrl': largeIconUrl,
      'isLocal': isLocal,
    });

    if (result == 1) {
      state = PlayerState.PLAYING;
    }

    return result;
  }

  /// Plays your playlist.
  ///
  /// If [exoPlayerMode] is set to [ExoPlayerMode.FOREGROUND], then you also need to pass:
  /// [audioObjects] for providing the foreground notification.
  Future<int> playAll(
    //! maybe raname to playPlaylist
    List<String> urls, {
    double volume = 1.0,
    bool repeatMode = false,
    PlayerMode playerMode = PlayerMode.BACKGROUND,
    List<AudioObject> audioObjects,
  }) async {
    volume ??= 1.0;
    playerMode ??= PlayerMode.BACKGROUND;
    repeatMode ??= false;

    bool isBackground = true;
    final List<int> smallIcons = List();
    final List<String> titles = List();
    final List<String> subTitles = List();
    final List<String> largeIconUrls = List();
    final List<bool> isLocals = List();
    if (playerMode == PlayerMode.FOREGROUND) {
      for (AudioObject audioObject in audioObjects) {
        smallIcons.add(audioObject.getSmallIcon());
        titles.add(audioObject.getTitle());
        subTitles.add(audioObject.getSubTitle());
        largeIconUrls.add(audioObject.getLargeIconUrl());
        isLocals.add(audioObject.getIsLocal());
      }

      isBackground = false;
    }

    final int result = await _invokeMethod('playAll', {
      'urls': urls,
      'volume': volume,
      'repeatMode': repeatMode,
      'isBackground': isBackground,
      'smallIcons': smallIcons,
      'titles': titles,
      'subTitles': subTitles,
      'largeIconUrls': largeIconUrls,
      'isLocals': isLocals,
    });

    if (result == 1) {
      state = PlayerState.PLAYING;
    }

    return result;
  }

  /// Pauses the audio that is currently playing.
  ///
  /// If you call [resume] later, the audio will resume from the point that it
  /// has been paused.
  Future<int> pause() async {
    final int result = await _invokeMethod('pause');

    if (result == 1) {
      state = PlayerState.PAUSED;
    }

    return result;
  }

  /// Stops the audio that is currently playing.
  ///
  /// The position is going to be reset and you will no longer be able to resume
  /// from the last point.
  Future<int> stop() async {
    final int result = await _invokeMethod('stop');

    if (result == 1) {
      state = PlayerState.STOPPED;
    }

    return result;
  }

  /// Resumes the audio that has been paused.
  Future<int> resume() async {
    final int result = await _invokeMethod('resume');

    if (result == 1) {
      state = PlayerState.PLAYING;
    }

    return result;
  }

  /// Releases the resources associated with this audio player.
  ///
  Future<int> release() async {
    final int result = await _invokeMethod('release');

    if (result == 1) {
      state = PlayerState.STOPPED;
    }

    return result;
  }

  /// Moves the cursor to the desired position.
  Future<int> seek(Duration position) {
    return _invokeMethod('seek', {'position': position.inMilliseconds});
  }

  /// Sets the volume (amplitude).
  ///
  /// 0 is mute and 1 is the max volume. The values between 0 and 1 are linearly
  /// interpolated.
  Future<int> setVolume(double volume) {
    return _invokeMethod('setVolume', {'volume': volume});
  }

  /// Get audio duration after setting url.
  ///
  /// It will be available as soon as the audio duration is available
  /// (it might take a while to download or buffer it if file is not local).
  Future<int> getDuration() {
    return _invokeMethod('getDuration');
  }

  /// Gets audio current playing position
  Future<int> getCurrentPosition() async {
    return _invokeMethod('getCurrentPosition');
  }

  static Future<void> platformCallHandler(MethodCall call) async {
    try {
      _doHandlePlatformCall(call);
    } catch (ex) {
      _log('Unexpected error: $ex');
    }
  }

  Future<int> _invokeMethod(
    String method, [
    Map<String, dynamic> arguments,
  ]) {
    arguments ??= const {};

    return _channel
        .invokeMethod(method, arguments)
        .then((result) => (result as int));
  }

  static Future<void> _doHandlePlatformCall(MethodCall call) async {
    final Map<dynamic, dynamic> callArgs = call.arguments as Map;
    _log('_platformCallHandler call ${call.method} $callArgs');

    switch (call.method) {
      // case 'audio.onCurrentPosition':
      //   Duration newDuration = Duration(milliseconds: value);
      //   player._positionController.add(newDuration);
      //   // ignore: deprecated_member_use_from_same_package
      //   player.positionHandler?.call(newDuration);
      //   break;
      // case 'audio.onComplete':
      //   exoPlayerState = ExoPlayerState.COMPLETED;
      //   player._completionController.add(null);
      //   // ignore: deprecated_member_use_from_same_package
      //   player.completionHandler?.call();
      //   break;
      // case 'audio.onError':
      //   exoPlayerState = ExoPlayerState.STOPPED;
      //   player._errorController.add(value);
      //   // ignore: deprecated_member_use_from_same_package
      //   player.errorHandler?.call(value);
      //   break;
      default:
        _log('Unknown method ${call.method} ');
    }
  }

  static void _log(String param) {
    if (logEnabled) {
      print(param);
    }
  }

  Future<void> dispose() async {
    List<Future> futures = [];

    if (!_playerStateController.isClosed) {
      futures.add(_playerStateController.close());
    }
    if (!_positionController.isClosed) {
      futures.add(_positionController.close());
    }
    if (!_completionController.isClosed) {
      futures.add(_completionController.close());
    }
    if (!_errorController.isClosed) {
      futures.add(_errorController.close());
    }
    await Future.wait(futures);
  }
}
