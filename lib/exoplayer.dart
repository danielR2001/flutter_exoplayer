import 'dart:async';
import 'package:exoplayer/audio_object.dart';
import 'package:flutter/services.dart';
import 'package:uuid/uuid.dart';

enum PlayerState {
  STOPPED,
  PLAYING,
  PAUSED,
  COMPLETED,
  RELEASED,
  BUFFERING,
}
enum PlayerMode {
  FOREGROUND,
  BACKGROUND,
}

class ExoPlayer {
  static MethodChannel _channel = const MethodChannel('danielr2001/exoplayer')
    ..setMethodCallHandler(platformCallHandler);
  static final _uuid = Uuid();
  static bool logEnabled = false;
  static final players = Map<String, ExoPlayer>();

  String playerId;
  PlayerState playerState;

  final StreamController<PlayerState> _playerStateController =
      StreamController<PlayerState>.broadcast();

  final StreamController<Duration> _positionController =
      StreamController<Duration>.broadcast();

  final StreamController<Duration> _durationController =
      StreamController<Duration>.broadcast();

  final StreamController<void> _completionController =
      StreamController<void>.broadcast();

  final StreamController<String> _errorController =
      StreamController<String>.broadcast();

  /// Stream of changes on player playerState.
  Stream<PlayerState> get onPlayerStateChanged => _playerStateController.stream;

  /// Stream of changes on audio position.
  ///
  /// Roughly fires every 200 milliseconds. Will continuously update the
  /// position of the playback if the status is [AudioPlayerState.PLAYING].
  ///
  /// You can use it on a progress bar, for instance.
  Stream get onAudioPositionChanged => _positionController.stream;

  /// Stream of changes on audio duration.
  ///
  /// An event is going to be sent as soon as the audio duration is available
  /// (it might take a while to download or buffer it).
  Stream<Duration> get onDurationChanged => _durationController.stream;

  /// Stream of player completions.
  ///
  /// Events are sent every time an audio is finished, therefore no event is
  /// sent when an audio is paused or stopped.
  ///
  /// [ReleaseMode.LOOP] also sends events to this stream.
  Stream<void> get onPlayerCompletion => _completionController.stream;

  /// Stream of player errors.
  ///
  /// Events are sent when an unexpected error is thrown in the native code.
  Stream<String> get onPlayerError => _errorController.stream;

  PlayerState _audioPlayerState;

  PlayerState get state => _audioPlayerState;

  /// Initializes ExoPlayer
  ///
  ExoPlayer() {
    playerState = PlayerState.RELEASED;
    playerId = _uuid.v4();
    players[playerId] = this;
  }

  /// Plays an audio.
  ///
  /// If [exoPlayerMode] is set to [ExoPlayerMode.FOREGROUND], then you also need to pass:
  /// [audioObject] for providing the foreground notification.
  Future<int> play(
    String url, {
    double volume = 1.0,
    bool repeatMode = false,
    bool respectAudioFocus = false,
    PlayerMode playerMode = PlayerMode.BACKGROUND,
    AudioObject audioObject,
  }) {
    volume ??= 1.0;
    playerMode ??= PlayerMode.BACKGROUND;
    repeatMode ??= false;
    respectAudioFocus ??= false;

    bool isBackground = true;
    String smallIconFileName;
    String title;
    String subTitle;
    String largeIconUrl;
    bool isLocal;
    int notificationMode;
    if (playerMode == PlayerMode.FOREGROUND) {
      smallIconFileName = audioObject.getSmallIconFileName();
      title = audioObject.getTitle();
      subTitle = audioObject.getSubTitle();
      largeIconUrl = audioObject.getLargeIconUrl();
      isLocal = audioObject.getIsLocal();
      if (audioObject.getNotificationMode() == NotificationMode.NONE) {
        notificationMode = 0;
      } else if (audioObject.getNotificationMode() == NotificationMode.NEXT) {
        notificationMode = 1;
      } else if (audioObject.getNotificationMode() ==
          NotificationMode.PREVIOUS) {
        notificationMode = 2;
      } else {
        notificationMode = 3;
      }

      isBackground = false;
    }

    return _invokeMethod('play', {
      'url': url,
      'volume': volume,
      'repeatMode': repeatMode,
      'isBackground': isBackground,
      'respectAudioFocus': respectAudioFocus,
      'smallIconFileName': smallIconFileName,
      'title': title,
      'subTitle': subTitle,
      'largeIconUrl': largeIconUrl,
      'isLocal': isLocal,
      'notificationMode': notificationMode,
    });
  }

  /// Plays your playlist.
  ///
  /// If [exoPlayerMode] is set to [ExoPlayerMode.FOREGROUND], then you also need to pass:
  /// [audioObjects] for providing the foreground notification.
  Future<int> playAll(
    List<String> urls, {
    double volume = 1.0,
    bool repeatMode = false,
    bool respectAudioFocus = false,
    PlayerMode playerMode = PlayerMode.BACKGROUND,
    List<AudioObject> audioObjects,
  }) {
    volume ??= 1.0;
    playerMode ??= PlayerMode.BACKGROUND;
    repeatMode ??= false;
    respectAudioFocus ??= false;

    bool isBackground = true;
    final List<String> smallIconFileNames = List();
    final List<String> titles = List();
    final List<String> subTitles = List();
    final List<String> largeIconUrls = List();
    final List<bool> isLocals = List();
    final List<int> notificationModes = List();
    if (playerMode == PlayerMode.FOREGROUND) {
      for (AudioObject audioObject in audioObjects) {
        smallIconFileNames.add(audioObject.getSmallIconFileName());
        titles.add(audioObject.getTitle());
        subTitles.add(audioObject.getSubTitle());
        largeIconUrls.add(audioObject.getLargeIconUrl());
        isLocals.add(audioObject.getIsLocal());

        if (audioObject.getNotificationMode() == NotificationMode.NONE) {
          notificationModes.add(0);
        } else if (audioObject.getNotificationMode() == NotificationMode.NEXT) {
          notificationModes.add(1);
        } else if (audioObject.getNotificationMode() ==
            NotificationMode.PREVIOUS) {
          notificationModes.add(2);
        } else {
          notificationModes.add(3);
        }
      }

      isBackground = false;
    }

    return _invokeMethod('playAll', {
      'urls': urls,
      'volume': volume,
      'repeatMode': repeatMode,
      'isBackground': isBackground,
      'respectAudioFocus': respectAudioFocus,
      'smallIconFileNames': smallIconFileNames,
      'titles': titles,
      'subTitles': subTitles,
      'largeIconUrls': largeIconUrls,
      'isLocals': isLocals,
      'notificationModes': notificationModes,
    });
  }

  /// Pauses the audio that is currently playing.
  ///
  /// If you call [resume] later, the audio will resume from the point that it
  /// has been paused.
  Future<int> pause() {
    return _invokeMethod('pause');
  }

  /// Stops the audio that is currently playing.
  ///
  /// The position is going to be reset and you will no longer be able to resume
  /// from the last point.
  Future<int> stop() {
    return _invokeMethod('stop');
  }

  /// Resumes the audio that has been paused.
  Future<int> resume() {
    return _invokeMethod('resume');
  }

  /// Releases the resources associated with this audio player.
  ///
  Future<int> release() {
    return _invokeMethod('release');
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
  Future<Duration> getDuration() async {
    int milliseconds = await _invokeMethod('getDuration');
    return Duration(milliseconds: milliseconds);
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

    final Map<String, dynamic> withPlayerId = Map.of(arguments)
      ..['playerId'] = playerId;

    return _channel
        .invokeMethod(method, withPlayerId)
        .then((result) => (result as int));
  }

  static Future<void> _doHandlePlatformCall(MethodCall call) async {
    final Map<dynamic, dynamic> callArgs = call.arguments as Map;
    _log('_platformCallHandler call ${call.method} $callArgs');

    final playerId = callArgs['playerId'] as String;
    final ExoPlayer player = players[playerId];
    final value = callArgs['value'];

    switch (call.method) {
      case 'audio.onDurationChanged':
        Duration newDuration = Duration(milliseconds: value);
        player._durationController.add(newDuration);
        break;
      case 'audio.onCurrentPositionChanged':
        Duration newDuration = Duration(milliseconds: value);
        player._positionController.add(newDuration);
        break;
      case 'audio.onStateChanged':
        switch (value) {
          case -1:
            {
              player.playerState = PlayerState.RELEASED;
              player._playerStateController.add(player.playerState);
              break;
            }
          case 0:
            {
              player.playerState = PlayerState.STOPPED;
              player._playerStateController.add(player.playerState);
              break;
            }
          case 1:
            {
              player.playerState = PlayerState.BUFFERING;
              player._playerStateController.add(player.playerState);
              player._completionController.add(null);
              break;
            }
          case 2:
            {
              player.playerState = PlayerState.PLAYING;
              player._playerStateController.add(player.playerState);
              break;
            }
          case 3:
            {
              player.playerState = PlayerState.PAUSED;
              player._playerStateController.add(player.playerState);
              break;
            }
          case 4:
            {
              player.playerState = PlayerState.COMPLETED;
              player._playerStateController.add(player.playerState);
              break;
            }
        }
        break;
      case 'audio.onError':
        player.playerState = PlayerState.STOPPED; //! maybe released?
        player._errorController.add(value);
        break;
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
    await _invokeMethod('dispose');
    if (!_playerStateController.isClosed) {
      futures.add(_playerStateController.close());
    }
    if (!_positionController.isClosed) {
      futures.add(_positionController.close());
    }
    if (!_durationController.isClosed) {
      futures.add(_durationController.close());
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
