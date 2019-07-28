import 'dart:async';

import 'package:exoplayer/audio_object.dart';
import 'package:exoplayer/exoplayer.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

class PlayerWidget extends StatefulWidget {
  final String url;
  final bool isLocal;

  PlayerWidget({@required this.url, this.isLocal = false});

  @override
  State<StatefulWidget> createState() {
    return new _PlayerWidgetState(url, isLocal);
  }
}

class _PlayerWidgetState extends State<PlayerWidget> {
  String url;
  bool isLocal;

  ExoPlayer _audioPlayer;
  Duration _duration;
  Duration _position;

  PlayerState _playerState = PlayerState.STOPPED;
  StreamSubscription _durationSubscription;
  StreamSubscription _positionSubscription;
  StreamSubscription _playerCompleteSubscription;
  StreamSubscription _playerErrorSubscription;
  StreamSubscription _playerStateSubscription;

  get _isPlaying => _playerState == PlayerState.PLAYING;
  get _isPaused => _playerState == PlayerState.PAUSED;
  get _durationText => _duration?.toString()?.split('.')?.first ?? '';
  get _positionText => _position?.toString()?.split('.')?.first ?? '';

  _PlayerWidgetState(this.url, this.isLocal);

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
                onPressed: _isPlaying || _isPaused ? () => _stop() : null,
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
    _playerStateSubscription =
        _audioPlayer.onPlayerStateChanged.listen((playerState) {
      setState(() {
        _playerState = playerState;
      });
    });
  }

  Future<int> _play() async {
    final result = await _audioPlayer.play(url, repeatMode: true);
    if (result == 1) setState(() => _playerState = PlayerState.PLAYING);
    return result;
  }

  Future<int> _resume() async {
    //AudioObject audioObject = new AudioObject()
    final result = await _audioPlayer.resume();
    if (result == 1) setState(() => _playerState = PlayerState.PLAYING);
    return result;
  }

  Future<int> _pause() async {
    final result = await _audioPlayer.pause();
    if (result == 1) setState(() => _playerState = PlayerState.PAUSED);
    return result;
  }

  Future<int> _stop() async {
    final result = await _audioPlayer.stop();
    if (result == 1) {
      setState(() {
        _playerState = PlayerState.STOPPED;
        _position = Duration();
      });
    }
    return result;
  }
}
