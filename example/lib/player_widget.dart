import 'dart:async';

import 'package:exoplayer/audio_object.dart';
import 'package:exoplayer/exoplayer.dart';
import 'package:flutter/material.dart';

class PlayerWidget extends StatefulWidget {
  final String url;
  final List<String> urls;
  final bool isLocal;

  PlayerWidget({this.url, this.urls, this.isLocal = false});

  @override
  State<StatefulWidget> createState() {
    return new _PlayerWidgetState(url, urls, isLocal);
  }
}

class _PlayerWidgetState extends State<PlayerWidget> {
  String url;
  List<String> urls;
  bool isLocal;

  ExoPlayer _audioPlayer;
  Duration _duration;
  Duration _position;

  PlayerState _playerState = PlayerState.RELEASED;
  StreamSubscription _durationSubscription;
  StreamSubscription _positionSubscription;
  StreamSubscription _playerCompleteSubscription;
  StreamSubscription _playerErrorSubscription;
  StreamSubscription _playerStateSubscription;

  get _isPlaying => _playerState == PlayerState.PLAYING;
  get _isPaused => _playerState == PlayerState.PAUSED;
  get _durationText => _duration?.toString()?.split('.')?.first ?? '';
  get _positionText => _position?.toString()?.split('.')?.first ?? '';

  _PlayerWidgetState(this.url, this.urls, this.isLocal);

  @override
  void initState() {
    super.initState();
    _initAudioPlayer();
  }

  @override
  void dispose() {
    _audioPlayer.release();
    _durationSubscription?.cancel();
    _positionSubscription?.cancel();
    _playerCompleteSubscription?.cancel();
    _playerErrorSubscription?.cancel();
    _playerStateSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return new Column(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        new Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            new IconButton(
                onPressed: _isPlaying ? null : () => _play(),
                iconSize: 64.0,
                icon: new Icon(Icons.play_arrow),
                color: Colors.cyan),
            new IconButton(
                onPressed: _isPlaying ? null : () => _resume(),
                iconSize: 64.0,
                icon: new Icon(Icons.play_arrow),
                color: Colors.cyan),
            new IconButton(
                onPressed: _isPlaying ? () => _pause() : null,
                iconSize: 64.0,
                icon: new Icon(Icons.pause),
                color: Colors.cyan),
            new IconButton(
                onPressed: _isPlaying || _isPaused ? () => _release() : null,
                iconSize: 64.0,
                icon: new Icon(Icons.stop),
                color: Colors.cyan),
          ],
        ),
        new Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            new Padding(
              padding: new EdgeInsets.all(12.0),
              child: new Stack(
                children: [
                  new CircularProgressIndicator(
                    value: 1.0,
                    valueColor: new AlwaysStoppedAnimation(Colors.grey[300]),
                  ),
                  new CircularProgressIndicator(
                    value: (_position != null &&
                            _duration != null &&
                            _position.inMilliseconds > 0 &&
                            _position.inMilliseconds < _duration.inMilliseconds)
                        ? _position.inMilliseconds / _duration.inMilliseconds
                        : 0.0,
                    valueColor: new AlwaysStoppedAnimation(Colors.cyan),
                  ),
                ],
              ),
            ),
            new Text(
              _position != null
                  ? '${_positionText ?? ''} / ${_durationText ?? ''}'
                  : _duration != null ? _durationText : '',
              style: new TextStyle(fontSize: 24.0),
            ),
          ],
        ),
        new Text("State: $_playerState")
      ],
    );
  }

  void _initAudioPlayer() {
    _audioPlayer = ExoPlayer();
    _positionSubscription = _audioPlayer.onAudioPositionChanged.listen((pos) {
      setState(() {
        _position = pos;
      });
    });
    _durationSubscription = _audioPlayer.onDurationChanged.listen((duration) {
      setState(() {
        _duration = duration;
      });
    });
    _playerStateSubscription =
        _audioPlayer.onPlayerStateChanged.listen((playerState) {
      setState(() {
        _playerState = playerState;
      });
    });
  }

  Future<void> _play() async {
    AudioObject audioObject = AudioObject(
        smallIconFileName: "ic_launcher",
        title: "title",
        subTitle: "artist",
        largeIconUrl:
            "https://www.clashmusic.com/sites/default/files/field/image/BobMarley_0.jpg",
        isLocal: false,
        notificationMode: NotificationMode.BOTH);
    if (url != null) {
      await _audioPlayer.play(url,
          repeatMode: true,
          respectAudioFocus: true,
          playerMode: PlayerMode.FOREGROUND,
          audioObject: audioObject);
    } else {
      List<AudioObject> audioObjects = [
        AudioObject(
            smallIconFileName: "ic_launcher",
            title: "title1",
            subTitle: "artist1",
            largeIconUrl:
                "https://www.clashmusic.com/sites/default/files/field/image/BobMarley_0.jpg",
            isLocal: false,
            notificationMode: NotificationMode.BOTH),
        AudioObject(
            smallIconFileName: "ic_launcher",
            title: "title2",
            subTitle: "artist2",
            largeIconUrl:
                "https://specials-images.forbesimg.com/imageserve/5be1e2a3a7ea437059163919/960x0.jpg?cropX1=0&cropX2=1999&cropY1=0&cropY2=1999",
            isLocal: false,
            notificationMode: NotificationMode.BOTH),
        AudioObject(
            smallIconFileName: "ic_launcher",
            title: "title3",
            subTitle: "artist3",
            largeIconUrl:
                "https://mixmag.net/assets/uploads/images/_full/aviciiobit.jpg",
            isLocal: false,
            notificationMode: NotificationMode.NONE),
      ];

      await _audioPlayer.playAll(urls,
          repeatMode: true,
          respectAudioFocus: true,
          playerMode: PlayerMode.FOREGROUND,
          audioObjects: audioObjects);
    }
  }

  Future<void> _resume() async {
    await _audioPlayer.resume();
  }

  Future<void> _pause() async {
    await _audioPlayer.pause();
  }

  Future<void> _release() async {
    await _audioPlayer.release();
  }
}
