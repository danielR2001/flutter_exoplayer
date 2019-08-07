import 'dart:async';
import 'package:flutter_exoplayer/audio_notification.dart';
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
enum Result {
  SUCCESS,
  FAIL,
  ERROR,
}

class AudioPlayer {
  static MethodChannel _channel = const MethodChannel('danielr2001/audioplayer')
    ..setMethodCallHandler(platformCallHandler);
  static final _uuid = Uuid();
  static bool logEnabled = false;
  static final players = Map<String, AudioPlayer>();

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

  final StreamController<int> _currentPlayingIndexController =
      StreamController<int>.broadcast();

  final StreamController<int> _audioSessionIdController =
      StreamController<int>.broadcast();

  final StreamController<NotificationActionName> _notificationActionController =
      StreamController<NotificationActionName>.broadcast();

  /// Stream of changes on player playerState.
  /// 
  /// Events are sent every time the state of the audioplayer is changed
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

  /// Stream of player audio session ID.
  ///
  /// Events are sent every time the audio session id is changed.
  Stream<int> get onAudioSessionIdChange => _audioSessionIdController.stream;

  /// Stream of player errors.
  ///
  /// Events are sent when an unexpected error is thrown in the native code.
  Stream<String> get onPlayerError => _errorController.stream;

  /// Stream of notification actions callback.
  ///
  /// Events are sent every time the user taps on one of the notification`s
  /// actions, if `NotificationActionCallbackMode.CUSTOM` is passed to `AudioNotification`.
  Stream<NotificationActionName> get onNotificationActionCallback =>
      _notificationActionController.stream;

  /// Stream of player errors.
  ///
  /// Events are sent when an unexpected error is thrown in the native code.
  Stream<int> get onPlayerIndexChanged => _currentPlayingIndexController.stream;

  PlayerState _audioPlayerState;

  PlayerState get state => _audioPlayerState;

  /// Initializes AudioPlayer
  ///
  AudioPlayer() {
    playerState = PlayerState.RELEASED;
    playerId = _uuid.v4();
    players[playerId] = this;
  }

  /// Plays an audio.
  ///
  /// If [PlayerMode] is set to [PlayerMode.FOREGROUND], then you also need to pass:
  /// [audioNotification] for providing the foreground notification.
  Future<Result> play(
    String url, {
    double volume = 1.0,
    bool repeatMode = false,
    bool respectAudioFocus = false,
    PlayerMode playerMode = PlayerMode.BACKGROUND,
    AudioNotification audioNotification,
  }) async {
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
    int notificationActionMode;
    int notificationActionCallbackMode = 0;
    if (playerMode == PlayerMode.FOREGROUND) {
      smallIconFileName = audioNotification.getSmallIconFileName();
      title = audioNotification.getTitle();
      subTitle = audioNotification.getSubTitle();
      largeIconUrl = audioNotification.getLargeIconUrl();
      isLocal = audioNotification.getIsLocal();
      if (audioNotification.getNotificationMode() ==
          NotificationActionMode.NONE) {
        notificationActionMode = 0;
      } else if (audioNotification.getNotificationMode() ==
          NotificationActionMode.NEXT) {
        notificationActionMode = 1;
      } else if (audioNotification.getNotificationMode() ==
          NotificationActionMode.PREVIOUS) {
        notificationActionMode = 2;
      } else {
        notificationActionMode = 3;
      }

      if (audioNotification.getNotificationActionCallbackMode() ==
          NotificationActionCallbackMode.CUSTOM) {
        notificationActionCallbackMode = 1;
      }

      isBackground = false;
    }

    switch (await _invokeMethod('play', {
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
      'notificationActionMode': notificationActionMode,
      'notificationActionCallbackMode': notificationActionCallbackMode,
    })) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Plays your playlist.
  ///
  /// If [PlayerMode] is set to [PlayerMode.FOREGROUND], then you also need to pass:
  /// [audioNotifications] for providing the foreground notification.
  Future<Result> playAll(
    List<String> urls, {
    double volume = 1.0,
    bool repeatMode = false,
    bool respectAudioFocus = false,
    PlayerMode playerMode = PlayerMode.BACKGROUND,
    List<AudioNotification> audioNotifications,
  }) async {
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
    final List<int> notificationActionCallbackModes = List();
    if (playerMode == PlayerMode.FOREGROUND) {
      for (AudioNotification audioNotification in audioNotifications) {
        smallIconFileNames.add(audioNotification.getSmallIconFileName());
        titles.add(audioNotification.getTitle());
        subTitles.add(audioNotification.getSubTitle());
        largeIconUrls.add(audioNotification.getLargeIconUrl());
        isLocals.add(audioNotification.getIsLocal());

        if (audioNotification.getNotificationMode() ==
            NotificationActionMode.NONE) {
          notificationModes.add(0);
        } else if (audioNotification.getNotificationMode() ==
            NotificationActionMode.NEXT) {
          notificationModes.add(1);
        } else if (audioNotification.getNotificationMode() ==
            NotificationActionMode.PREVIOUS) {
          notificationModes.add(2);
        } else {
          notificationModes.add(3);
        }

        if (audioNotification.getNotificationActionCallbackMode() ==
            NotificationActionCallbackMode.CUSTOM) {
          notificationActionCallbackModes.add(1);
        } else {
          notificationActionCallbackModes.add(0);
        }
      }

      isBackground = false;
    }

    switch (await _invokeMethod('playAll', {
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
      'notificationActionCallbackModes': notificationActionCallbackModes,
    })) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Pauses the audio that is currently playing.
  ///
  /// If you call [resume] later, the audio will resume from the point that it
  /// has been paused.
  Future<Result> pause() async {
    switch (await _invokeMethod('pause')) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Plays the next song.
  ///
  /// If playing only single audio it will restart the current.
  Future<Result> next() async {
    switch (await _invokeMethod('next')) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Plays the previous song.
  ///
  /// If playing only single audio it will restart the current.
  Future<Result> previous() async {
    switch (await _invokeMethod('previous')) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Stops the audio that is currently playing.
  ///
  /// The position is going to be reset and you will no longer be able to resume
  /// from the last point.
  Future<Result> stop() async {
    switch (await _invokeMethod('stop')) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Resumes the audio that has been paused.
  Future<Result> resume() async {
    switch (await _invokeMethod('resume')) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Releases the resources associated with this audio player.
  ///
  Future<Result> release() async {
    switch (await _invokeMethod('release')) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Moves the cursor to the desired position.
  Future<Result> seek(Duration position) async {
    switch (
        await _invokeMethod('seek', {'position': position.inMilliseconds})) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Sets the volume (amplitude).
  ///
  /// 0 is mute and 1 is the max volume. The values between 0 and 1 are linearly
  /// interpolated.
  Future<Result> setVolume(double volume) async {
    switch (await _invokeMethod('setVolume', {'volume': volume})) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
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
  ///
  /// the position starts from 0.
  Future<Result> getCurrentPosition() async {
    switch (await _invokeMethod('getCurrentPosition')) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
  }

  /// Gets current playing audio index
  Future<Result> getCurrentPlayingAudioIndex() async {
    switch (await _invokeMethod('getCurrentPlayingAudioIndex')) {
      case 0:
        return Result.FAIL;
      case 1:
        return Result.SUCCESS;
      default:
        return Result.ERROR;
    }
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
    final AudioPlayer player = players[playerId];
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
      case 'audio.onCurrentPlayingAudioIndex':
        player._currentPlayingIndexController.add(value);
        break;
      case 'audio.onAudioSessionIdChange':
        player._audioSessionIdController.add(value);
        break;
      case 'audio.onNotificationActionCallback':
        switch (value) {
          case 0:
            {
              player._notificationActionController
                  .add(NotificationActionName.PREVIOUS);
              break;
            }
          case 1:
            {
              player._notificationActionController
                  .add(NotificationActionName.NEXT);
              break;
            }
          case 2:
            {
              player._notificationActionController
                  .add(NotificationActionName.PLAY);
              break;
            }
          case 3:
            {
              player._notificationActionController
                  .add(NotificationActionName.PAUSE);
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
    if (!_currentPlayingIndexController.isClosed) {
      futures.add(_currentPlayingIndexController.close());
    }
    if (!_audioSessionIdController.isClosed) {
      futures.add(_audioSessionIdController.close());
    }
    if(!_notificationActionController.isClosed) {
      futures.add(_notificationActionController.close());
    }
    await Future.wait(futures);
  }
}
