import 'dart:async';
import 'package:flutter_exoplayer/audio_notification.dart';
import 'package:flutter/services.dart';
import 'package:uuid/uuid.dart';

enum PlayerState {
  RELEASED,
  STOPPED,
  BUFFERING,
  PLAYING,
  PAUSED,
  COMPLETED,
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

const PlayerStateMap = {
  -1: PlayerState.RELEASED,
  0: PlayerState.STOPPED,
  1: PlayerState.BUFFERING,
  2: PlayerState.PLAYING,
  3: PlayerState.PAUSED,
  4: PlayerState.COMPLETED,
};

const ResultMap = {
  0: Result.ERROR,
  1: Result.FAIL,
  2: Result.SUCCESS,
};

const NotificationDefaultActionsMap = {
  NotificationDefaultActions.NONE: 0,
  NotificationDefaultActions.NEXT: 1,
  NotificationDefaultActions.PREVIOUS: 2,
  NotificationDefaultActions.ALL: 3,
};

const NotificationCustomActionsMap = {
  NotificationCustomActions.DISABLED: 0,
  NotificationCustomActions.ONE: 1,
  NotificationCustomActions.TWO: 2,
};

const NotificationActionNameMap = {
  0: NotificationActionName.PREVIOUS,
  1: NotificationActionName.NEXT,
  2: NotificationActionName.PLAY,
  3: NotificationActionName.PAUSE,
  4: NotificationActionName.CUSTOM1,
  5: NotificationActionName.CUSTOM2,
};

const NotificationActionCallbackModeMap = {
  NotificationActionCallbackMode.DEFAULT: 0,
  NotificationActionCallbackMode.CUSTOM: 1,
};

class AudioPlayer {
  static MethodChannel _channel = const MethodChannel('danielr2001/audioplayer')
    ..setMethodCallHandler(platformCallHandler);

  static final _uuid = Uuid();
  static bool logEnabled = false;
  static final players = Map<String, AudioPlayer>();

  late String _playerId;
  late PlayerState _playerState;

  String get playerId => _playerId;

  PlayerState get playerState => _playerState;

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
  Stream<Duration> get onAudioPositionChanged => _positionController.stream;

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

  /// Stream of current playing index.
  ///
  /// Events are sent when current index of a player is being changed.
  Stream<int> get onCurrentAudioIndexChanged =>
      _currentPlayingIndexController.stream;

  // PlayerState _audioPlayerState;

  // PlayerState get state => _audioPlayerState;

  /// Initializes AudioPlayer
  ///
  AudioPlayer() {
    _playerState = PlayerState.RELEASED;
    _playerId = _uuid.v4();
    players[playerId] = this;
  }

  /// Plays an audio.
  ///
  /// If [PlayerMode] is set to [PlayerMode.FOREGROUND], then you also need to pass:
  /// [audioNotification] for providing the foreground notification.
  Future<Result> play(
    String url, {
    bool repeatMode = false,
    bool respectAudioFocus = false,
    Duration position = const Duration(milliseconds: 0),
    PlayerMode playerMode = PlayerMode.BACKGROUND,
    AudioNotification? audioNotification,
  }) async {
    if (playerMode == PlayerMode.FOREGROUND && audioNotification == null)
      return Result.ERROR;

    return ResultMap[await _invokeMethod('play', {
          'url': url,
          'repeatMode': repeatMode,
          'isBackground': playerMode == PlayerMode.BACKGROUND,
          'respectAudioFocus': respectAudioFocus,
          'position': position.inMilliseconds,
          // audio notification object
          'smallIconFileName': audioNotification?.smallIconFileName,
          'title': audioNotification?.title,
          'subTitle': audioNotification?.subTitle,
          'largeIconUrl': audioNotification?.largeIconUrl,
          'isLocal': audioNotification?.isLocal,
          'notificationDefaultActions':
              audioNotification?.notificationDefaultActions,
          'notificationActionCallbackMode':
              audioNotification?.notificationActionCallbackMode,
          'notificationCustomActions':
              audioNotification?.notificationCustomActions,
        }) as int] ??
        Result.ERROR;
  }

  /// Plays your playlist.
  ///
  /// If [PlayerMode] is set to [PlayerMode.FOREGROUND], then you also need to pass:
  /// [audioNotifications] for providing the foreground notification.
  Future<Result> playAll(
    List<String> urls, {
    int index = 0,
    bool repeatMode = false,
    bool respectAudioFocus = false,
    Duration position = const Duration(milliseconds: 0),
    PlayerMode playerMode = PlayerMode.BACKGROUND,
    List<AudioNotification> audioNotifications = const [],
  }) async {
    if (audioNotifications.length != urls.length &&
        playerMode == PlayerMode.FOREGROUND) return Result.ERROR;

    return ResultMap[await _invokeMethod('playAll', {
          'urls': urls,
          'repeatMode': repeatMode,
          'isBackground': playerMode == PlayerMode.BACKGROUND,
          'respectAudioFocus': respectAudioFocus,
          'position': position.inMilliseconds,
          'index': index,
          // audio notification objects
          'smallIconFileNames':
              audioNotifications.map((e) => e.smallIconFileName).toList(),
          'titles': audioNotifications.map((e) => e.title).toList(),
          'subTitles': audioNotifications.map((e) => e.subTitle).toList(),
          'largeIconUrls':
              audioNotifications.map((e) => e.largeIconUrl).toList(),
          'isLocals': audioNotifications.map((e) => e.isLocal).toList(),
          'notificationDefaultActionsList': audioNotifications
              .map((e) => e.notificationDefaultActions)
              .toList(),
          'notificationActionCallbackModes': audioNotifications
              .map((e) => e.notificationActionCallbackMode)
              .toList(),
          'notificationCustomActionsList': audioNotifications
              .map((e) => e.notificationCustomActions)
              .toList(),
        }) as int] ??
        Result.ERROR;
  }

  /// Pauses the audio that is currently playing.
  ///
  /// If you call [resume] later, the audio will resume from the point that it
  /// has been paused.
  Future<Result> pause() async {
    return ResultMap[await _invokeMethod('pause') as int] ?? Result.ERROR;
  }

  /// Plays the next song.
  ///
  /// If playing only single audio it will restart the current.
  Future<Result> next() async {
    return ResultMap[await _invokeMethod('next') as int] ?? Result.ERROR;
  }

  /// Plays the previous song.
  ///
  /// If playing only single audio it will restart the current.
  Future<Result> previous() async {
    return ResultMap[await _invokeMethod('previous') as int] ?? Result.ERROR;
  }

  /// Stops the audio that is currently playing.
  ///
  /// The position is going to be reset and you will no longer be able to resume
  /// from the last point.
  Future<Result> stop() async {
    return ResultMap[await _invokeMethod('stop') as int] ?? Result.ERROR;
  }

  /// Resumes the audio that has been paused.
  Future<Result> resume() async {
    return ResultMap[await _invokeMethod('resume') as int] ?? Result.ERROR;
  }

  /// Releases the resources associated with this audio player.
  ///
  Future<Result> release() async {
    return ResultMap[await _invokeMethod('release') as int] ?? Result.ERROR;
  }

  /// Moves the cursor to the desired position.
  Future<Result> seekPosition(Duration position) async {
    return ResultMap[await _invokeMethod(
            'seekPosition', {'position': position.inMilliseconds}) as int] ??
        Result.ERROR;
  }

  /// Switches to the desired index in playlist.
  Future<Result> seekIndex(int index) async {
    return ResultMap[
            await _invokeMethod('seekIndex', {'index': index}) as int] ??
        Result.ERROR;
  }

  /// Gets audio duration after setting url.
  ///
  /// It will be available as soon as the audio duration is available
  /// (it might take a while to download or buffer it if file is not local).
  Future<Duration> getDuration() async {
    return Duration(milliseconds: await _invokeMethod('getDuration') as int);
  }

  /// Gets audio volume
  ///
  /// Volume range is from 0-1.
  Future<double> getVolume() async {
    return await _invokeMethod('getVolume') as double;
  }

  /// Gets the Playback speed
  ///
  /// speed range is from 1-8.
  Future<double> getPlaybackSpeed() async {
    return await _invokeMethod('getPlaybackSpeed') as double;
  }

  /// Gets audio current playing position
  ///
  /// the position starts from 0.
  Future<Duration> getCurrentPosition() async {
    int milliseconds = await _invokeMethod('getCurrentPosition') as int;
    return Duration(milliseconds: milliseconds);
  }

  /// Gets current playing audio index
  Future<int> getCurrentPlayingAudioIndex() async {
    return await _invokeMethod('getCurrentPlayingAudioIndex') as int;
  }

  /// Sets the volume (amplitude).
  ///
  /// Volume range is from 0-1.
  /// 0 is mute and 1 is the max volume. The values between 0 and 1 are linearly
  /// interpolated.
  Future<Result> setVolume(double volume) async {
    return ResultMap[
            await _invokeMethod('setVolume', {'volume': volume}) as int] ??
        Result.ERROR;
  }

  // Sets the repeat mode.
  Future<Result> setRepeatMode(bool repeatMode) async {
    return ResultMap[
            await _invokeMethod('setRepeatMode', {'repeatMode': repeatMode})
                as int] ??
        Result.ERROR;
  }

  // Sets the playback speed.
  ///
  /// speed range is from 1-8.
  /// 1 is regular speed.
  Future<Result> setPlaybackSpeed(double speed) async {
    return ResultMap[
            await _invokeMethod('setPlaybackSpeed', {'speed': speed}) as int] ??
        Result.ERROR;
  }

  /// Sets the [AudioNotification] for the single player, if you want to change specific
  /// notification in [AudioNotification]s list use [setSpecificAudioNotification].
  Future<Result> setAudioNotification(
      AudioNotification audioNotification) async {
    return ResultMap[await _invokeMethod('setAudioObject', {
          'smallIconFileName': audioNotification.smallIconFileName,
          'title': audioNotification.title,
          'subTitle': audioNotification.subTitle,
          'largeIconUrl': audioNotification.largeIconUrl,
          'notificationDefaultActions':
              audioNotification.notificationDefaultActions,
          'notificationActionCallbackMode':
              audioNotification.notificationActionCallbackMode,
          'notificationCustomActions':
              audioNotification.notificationCustomActions,
        }) as int] ??
        Result.ERROR;
  }

  /// Sets the [AudioNotification]s for the playlist player.
  Future<Result> setAudioNotifications(
      List<AudioNotification> audioNotifications) async {
    return ResultMap[await _invokeMethod('setAudioObjects', {
          'smallIconFileNames':
              audioNotifications.map((e) => e.smallIconFileName).toList(),
          'titles': audioNotifications.map((e) => e.title).toList(),
          'subTitles': audioNotifications.map((e) => e.subTitle).toList(),
          'largeIconUrls':
              audioNotifications.map((e) => e.largeIconUrl).toList(),
          'notificationDefaultActionsList': audioNotifications
              .map((e) => e.notificationDefaultActions)
              .toList(),
          'notificationActionCallbackModes': audioNotifications
              .map((e) => e.notificationActionCallbackMode)
              .toList(),
          'notificationCustomActionsList': audioNotifications
              .map((e) => e.notificationCustomActions)
              .toList(),
        }) as int] ??
        Result.ERROR;
  }

  /// Sets a sepcific [AudioNotification] in the [AudioNotification]s for the playlist player.
  Future<Result> setSpecificAudioNotification(
      AudioNotification audioNotification, int index) async {
    return ResultMap[await _invokeMethod('setSpecificAudioNotification', {
          'smallIconFileName': audioNotification.smallIconFileName,
          'title': audioNotification.title,
          'subTitle': audioNotification.subTitle,
          'largeIconUrl': audioNotification.largeIconUrl,
          'notificationDefaultActions':
              audioNotification.notificationDefaultActions,
          'notificationActionCallbackMode':
              audioNotification.notificationActionCallbackMode,
          'notificationCustomActions':
              audioNotification.notificationCustomActions,
          'index': index,
        }) as int] ??
        Result.ERROR;
  }

  static Future<void> platformCallHandler(MethodCall call) async {
    try {
      _doHandlePlatformCall(call);
    } catch (ex) {
      _log('Unexpected error: $ex');
    }
  }

  Future<dynamic> _invokeMethod(
    String method, [
    Map<String, dynamic> arguments = const {},
  ]) {
    final Map<String, dynamic> withPlayerId = Map.of(arguments)
      ..['playerId'] = playerId;

    return _channel
        .invokeMethod(method, withPlayerId)
        .then((result) => (result));
  }

  static Future<void> _doHandlePlatformCall(MethodCall call) async {
    final Map<dynamic, dynamic> callArgs = call.arguments as Map;
    _log('_platformCallHandler call ${call.method} $callArgs');

    final playerId = callArgs['playerId'] as String;
    final AudioPlayer player = players[playerId]!;
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
        player._playerState = PlayerStateMap[value]!;
        player._playerStateController.add(player._playerState);
        break;
      case 'audio.onCurrentPlayingAudioIndexChange':
        player._currentPlayingIndexController.add(value);
        print("track changed to: $value");
        break;
      case 'audio.onAudioSessionIdChange':
        player._audioSessionIdController.add(value);
        break;
      case 'audio.onNotificationActionCallback':
        player._notificationActionController
            .add(NotificationActionNameMap[value]!);
        break;
      case 'audio.onError':
        player._playerState = PlayerState.RELEASED;
        player._errorController.add(value);
        break;
      default:
        _log('Unknown method ${call.method} ');
    }
  }

  static void _log(String param) {
    if (logEnabled) print(param);
  }

  Future<void> dispose() async {
    List<Future> futures = [];
    await _invokeMethod('dispose');
    if (!_playerStateController.isClosed)
      futures.add(_playerStateController.close());
    if (!_positionController.isClosed) futures.add(_positionController.close());
    if (!_durationController.isClosed) futures.add(_durationController.close());
    if (!_completionController.isClosed)
      futures.add(_completionController.close());
    if (!_errorController.isClosed) futures.add(_errorController.close());
    if (!_currentPlayingIndexController.isClosed)
      futures.add(_currentPlayingIndexController.close());
    if (!_audioSessionIdController.isClosed)
      futures.add(_audioSessionIdController.close());
    if (!_notificationActionController.isClosed)
      futures.add(_notificationActionController.close());
    await Future.wait(futures);
  }
}
