import 'dart:async';

import 'package:flutter_exoplayer/audio_notification.dart';
import 'package:flutter_exoplayer/audioplayer.dart';
import 'package:flutter_exoplayer_example/main.dart';
import 'package:flutter/material.dart';

const imageUrl1 = "https://www.bensound.com/bensound-img/buddy.jpg";
const imageUrl2 = "https://www.bensound.com/bensound-img/epic.jpg";
const imageUrl3 = "https://www.bensound.com/bensound-img/onceagain.jpg";

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

  AudioPlayer _audioPlayer;
  Duration _duration;
  Duration _position;
  int _currentIndex = 0;

  PlayerState _playerState = PlayerState.RELEASED;
  StreamSubscription _durationSubscription;
  StreamSubscription _positionSubscription;
  StreamSubscription _playerCompleteSubscription;
  StreamSubscription _playerErrorSubscription;
  StreamSubscription _playerStateSubscription;
  StreamSubscription _playerIndexSubscription;
  StreamSubscription _playerAudioSessionIdSubscription;
  StreamSubscription _notificationActionCallbackSubscription;

  final List<AudioNotification> audioNotifications = [
    AudioNotification(
      smallIconFileName: "ic_launcher",
      title: "title1",
      subTitle: "artist1",
      largeIconUrl: imageUrl1,
      isLocal: false,
      notificationDefaultActions: NotificationDefaultActions.ALL,
      notificationCustomActions: NotificationCustomActions.TWO,
    ),
    AudioNotification(
        smallIconFileName: "ic_launcher",
        title: "title2",
        subTitle: "artist2",
        largeIconUrl: imageUrl2,
        isLocal: false,
        notificationDefaultActions: NotificationDefaultActions.ALL),
    AudioNotification(
        smallIconFileName: "ic_launcher",
        title: "title3",
        subTitle: "artist3",
        largeIconUrl: imageUrl3,
        isLocal: false,
        notificationDefaultActions: NotificationDefaultActions.ALL),
  ];

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
    _playerIndexSubscription?.cancel();
    _playerAudioSessionIdSubscription?.cancel();
    _notificationActionCallbackSubscription?.cancel();
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
                    onTap: () => _play(),
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
                  onPressed: () => _previous(),
                  iconSize: 45.0,
                  icon: Icon(Icons.skip_previous),
                  color: Colors.pink),
              IconButton(
                  onPressed: _isPlaying ? () => _pause() : () => _resume(),
                  iconSize: 45.0,
                  icon: _isPlaying ? Icon(Icons.pause) : Icon(Icons.play_arrow),
                  color: Colors.pink),
              IconButton(
                  onPressed: () => _next(),
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
                final Result result = await _audioPlayer
                    .seekPosition(Duration(milliseconds: value.toInt()));
                if (result == Result.FAIL) {
                  print(
                      "you tried to call audio conrolling methods on released audio player :(");
                } else if (result == Result.ERROR) {
                  print("something went wrong in seek :(");
                }
                _position = Duration(milliseconds: value.toInt());
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
        Text("index: $_currentIndex"),
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
    _audioPlayer = AudioPlayer();
    _durationSubscription = _audioPlayer.onDurationChanged.listen((duration) {
      setState(() {
        _duration = duration;
      });
    });
    _positionSubscription = _audioPlayer.onAudioPositionChanged.listen((pos) {
      setState(() {
        _position = pos;
      });
    });
    _playerStateSubscription =
        _audioPlayer.onPlayerStateChanged.listen((playerState) {
      setState(() {
        _playerState = playerState;
        print(_playerState);
      });
    });
    _playerIndexSubscription =
        _audioPlayer.onCurrentAudioIndexChanged.listen((index) {
      setState(() {
        _position = Duration(milliseconds: 0);
        _currentIndex = index;
      });
    });
    _playerAudioSessionIdSubscription =
        _audioPlayer.onAudioSessionIdChange.listen((audioSessionId) {
      print("audio Session Id: $audioSessionId");
    });
    _notificationActionCallbackSubscription = _audioPlayer
        .onNotificationActionCallback
        .listen((notificationActionName) {
      //do something
    });
    _playerCompleteSubscription = _audioPlayer.onPlayerCompletion.listen((a) {
      _position = Duration(milliseconds: 0);
    });
  }

  Future<void> _play() async {
    if (url != null) {
      final Result result = await _audioPlayer.play(
        url,
        repeatMode: true,
        respectAudioFocus: false,
        playerMode: PlayerMode.BACKGROUND,
      );
      if (result == Result.ERROR) {
        print("something went wrong in play method :(");
      }
    } else {
      final Result result = await _audioPlayer.playAll(
        urls,
        repeatMode: false,
        respectAudioFocus: true,
        playerMode: PlayerMode.FOREGROUND,
        audioNotifications: audioNotifications,
      );
      if (result == Result.ERROR) {
        print("something went wrong in playAll method :(");
      }
    }
  }

  Future<void> _resume() async {
    final Result result = await _audioPlayer.resume();
    if (result == Result.FAIL) {
      print(
          "you tried to call audio conrolling methods on released audio player :(");
    } else if (result == Result.ERROR) {
      print("something went wrong in resume :(");
    }
  }

  Future<void> _pause() async {
    final Result result = await _audioPlayer.pause();
    if (result == Result.FAIL) {
      print(
          "you tried to call audio conrolling methods on released audio player :(");
    } else if (result == Result.ERROR) {
      print("something went wrong in pause :(");
    }
  }

  Future<void> _stop() async {
    final Result result = await _audioPlayer.stop();
    if (result == Result.FAIL) {
      print(
          "you tried to call audio conrolling methods on released audio player :(");
    } else if (result == Result.ERROR) {
      print("something went wrong in stop :(");
    }
  }

  Future<void> _release() async {
    final Result result = await _audioPlayer.release();
    if (result == Result.FAIL) {
      print(
          "you tried to call audio conrolling methods on released audio player :(");
    } else if (result == Result.ERROR) {
      print("something went wrong in release :(");
    }
  }

  Future<void> _next() async {
    final Result result = await _audioPlayer.next();
    if (result == Result.FAIL) {
      print(
          "you tried to call audio conrolling methods on released audio player :(");
    } else if (result == Result.ERROR) {
      print("something went wrong in next :(");
    }
  }

  Future<void> _previous() async {
    final Result result = await _audioPlayer.previous();
    if (result == Result.FAIL) {
      print(
          "you tried to call audio conrolling methods on released audio player :(");
    } else if (result == Result.ERROR) {
      print("something went wrong in previous :(");
    }
  }

  String getUrlMatchingImage() {
    if (url == kUrl1) {
      return imageUrl1;
    } else if (url == kUrl2) {
      return imageUrl2;
    } else if (url == kUrl2) {
      return imageUrl3;
    } else {
      return imageUrl1;
    }
  }
}
