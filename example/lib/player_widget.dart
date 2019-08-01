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
              onChanged: (double value) {
                _audioPlayer.seek(Duration(milliseconds: value.toInt()));
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
                "https://d3vhc53cl8e8km.cloudfront.net/hello-staging/wp-content/uploads/2016/01/07185911/lyENmGPExUMOmaMngcMplWHy64QeAr0PxWpJHDwA-972x597.jpeg",
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
            notificationMode: NotificationMode.BOTH),
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

  Future<void> _stop() async {
    await _audioPlayer.stop();
  }

  Future<void> _release() async {
    await _audioPlayer.release();
  }

  Future<void> _next() async {
    await _audioPlayer.next();
  }

  Future<void> _previous() async {
    await _audioPlayer.previous();
  }
}
