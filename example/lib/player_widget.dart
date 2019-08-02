import 'dart:async';

import 'package:exoplayer/audio_object.dart';
import 'package:exoplayer/exoplayer.dart';
import 'package:flutter/material.dart';

class PlayerWidget extends StatefulWidget {
  final String url;
  final List<String> urls;

  PlayerWidget({this.url, this.urls});

  @override
  State<StatefulWidget> createState() {
    return _PlayerWidgetState(url, urls);
  }
}

class _PlayerWidgetState extends State<PlayerWidget> {
  String url;
  List<String> urls;

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
  get _durationText => _duration?.toString()?.split('.')?.first ?? '';
  get _positionText => _position?.toString()?.split('.')?.first ?? '';

  _PlayerWidgetState(this.url, this.urls);

  @override
  void initState() {
    super.initState();
    _initAudioPlayer();
  }

  @override
  void dispose() {
    _audioPlayer.dispose();
    _durationSubscription?.cancel();
    _positionSubscription?.cancel();
    _playerCompleteSubscription?.cancel();
    _playerErrorSubscription?.cancel();
    _playerStateSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.only(top: 20),
          child: Row(
            children: <Widget>[
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  child: GestureDetector(
                    child: Container(
                      width: 70,
                      height: 45,
                      decoration: BoxDecoration(
                          color: Colors.pink,
                          borderRadius: BorderRadius.circular(5)),
                      child: Center(
                        child: Text(
                          "Play",
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                    onTap: _isPlaying ? null : () => _play(),
                  ),
                ),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  child: GestureDetector(
                    child: Container(
                      width: 70,
                      height: 45,
                      decoration: BoxDecoration(
                          color: Colors.pink,
                          borderRadius: BorderRadius.circular(5)),
                      child: Center(
                        child: Text(
                          "Stop",
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                    onTap: () => _stop(),
                  ),
                ),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  child: GestureDetector(
                    child: Container(
                      width: 70,
                      height: 45,
                      decoration: BoxDecoration(
                          color: Colors.pink,
                          borderRadius: BorderRadius.circular(5)),
                      child: Center(
                        child: Text(
                          "Release",
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                    onTap: () => _release(),
                  ),
                ),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.only(top: 20),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              IconButton(
                  onPressed: () => _next(),
                  iconSize: 45.0,
                  icon: Icon(Icons.skip_previous),
                  color: Colors.pink),
              IconButton(
                  onPressed: _isPlaying ? () => _pause() : () => _resume(),
                  iconSize: 45.0,
                  icon: _isPlaying ? Icon(Icons.pause) : Icon(Icons.play_arrow),
                  color: Colors.pink),
              IconButton(
                  onPressed: () => _previous(),
                  iconSize: 45.0,
                  icon: Icon(Icons.skip_next),
                  color: Colors.pink),
            ],
          ),
        ),
        SizedBox(
          width: 400,
          height: 30,
          child: SliderTheme(
            data: SliderThemeData(
              thumbShape: RoundSliderThumbShape(enabledThumbRadius: 5),
              trackHeight: 3,
              thumbColor: Colors.pink,
              inactiveTrackColor: Colors.grey,
              activeTrackColor: Colors.pink,
              overlayColor: Colors.transparent,
            ),
            child: Slider(
              value:
                  _position != null ? _position.inMilliseconds.toDouble() : 0.0,
              min: 0.0,
              max:
                  _duration != null ? _duration.inMilliseconds.toDouble() : 0.0,
              onChanged: (double value) async {
                final int result = await _audioPlayer
                    .seek(Duration(milliseconds: value.toInt()));
                if (result != 1) {
                  print("something went wrong in resume method :(");
                }
              },
            ),
          ),
        ),
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              _position != null
                  ? '${_positionText ?? ''} / ${_durationText ?? ''}'
                  : _duration != null ? _durationText : '',
              style: TextStyle(fontSize: 24.0),
            ),
          ],
        ),
        Text("State: $_playerState"),
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 15),
          child: Container(
            height: 2,
            //width: 350,
            decoration: BoxDecoration(
              border: Border(
                bottom: BorderSide(
                  color: Colors.pink,
                ),
              ),
            ),
          ),
        ),
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
    if (url != null) {
      AudioObject audioObject = AudioObject(
          smallIconFileName: "ic_launcher",
          title: "title",
          subTitle: "artist",
          largeIconUrl: "https://www.bensound.com/bensound-img/buddy.jpg",
          isLocal: false,
          notificationMode: NotificationMode.BOTH);
      final int result = await _audioPlayer.play(url,
          repeatMode: true,
          respectAudioFocus: true,
          playerMode: PlayerMode.FOREGROUND,
          audioObject: audioObject);
      if (result != 1) {
        print("something went wrong in resume method :(");
      }
    } else {
      List<AudioObject> audioObjects = [
        AudioObject(
            smallIconFileName: "ic_launcher",
            title: "title1",
            subTitle: "artist2",
            largeIconUrl: "https://www.bensound.com/bensound-img/buddy.jpg",
            isLocal: false,
            notificationMode: NotificationMode.BOTH),
        AudioObject(
            smallIconFileName: "ic_launcher",
            title: "title2",
            subTitle: "artist3",
            largeIconUrl: "https://www.bensound.com/bensound-img/epic.jpg",
            isLocal: false,
            notificationMode: NotificationMode.BOTH),
        AudioObject(
            smallIconFileName: "ic_launcher",
            title: "title3",
            subTitle: "artist3",
            largeIconUrl: "https://www.bensound.com/bensound-img/onceagain.jpg",
            isLocal: false,
            notificationMode: NotificationMode.BOTH),
      ];

      final int result = await _audioPlayer.playAll(urls,
          repeatMode: true,
          respectAudioFocus: true,
          playerMode: PlayerMode.FOREGROUND,
          audioObjects: audioObjects);
      if (result != 1) {
        print("something went wrong in resume method :(");
      }
    }
  }

  Future<void> _resume() async {
    final int result = await _audioPlayer.resume();
    if (result != 1) {
      print("something went wrong in resume method :(");
    }
  }

  Future<void> _pause() async {
    final int result = await _audioPlayer.pause();
    if (result != 1) {
      print("something went wrong in pause method :(");
    }
  }

  Future<void> _stop() async {
    final int result = await _audioPlayer.stop();
    if (result != 1) {
      print("something went wrong in stop method :(");
    }
  }

  Future<void> _release() async {
    final int result = await _audioPlayer.release();
    if (result != 1) {
      print("something went wrong in release method :(");
    }
  }

  Future<void> _next() async {
    final int result = await _audioPlayer.next();
    if (result != 1) {
      print("something went wrong in resume next :(");
    }
  }

  Future<void> _previous() async {
    final int result = await _audioPlayer.previous();
    if (result != 1) {
      print("something went wrong in previous method :(");
    }
  }
}
