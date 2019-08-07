import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'player_widget.dart';
import 'package:http/http.dart';

const kUrl1 = 'https://www.bensound.org/bensound-music/bensound-buddy.mp3';
const kUrl2 = 'https://www.bensound.org/bensound-music/bensound-epic.mp3';
const kUrl3 = 'https://www.bensound.org/bensound-music/bensound-onceagain.mp3';

//const kUrl3 = 'http://bbcmedia.ic.llnwd.net/stream/bbcmedia_radio1xtra_mf_p';
final List<String> urls = [kUrl1, kUrl2, kUrl3];

void main() {
  runApp(MaterialApp(home: ExampleApp()));
}

class ExampleApp extends StatefulWidget {
  @override
  _ExampleAppState createState() => _ExampleAppState();
}

class _ExampleAppState extends State<ExampleApp> {
  String localFilePath;

  Widget _tab(List<Widget> children) {
    return Center(
      child: Container(
        padding: EdgeInsets.all(16.0),
        child: Column(
          children: children
              .map((w) => Container(child: w, padding: EdgeInsets.all(6.0)))
              .toList(),
        ),
      ),
    );
  }

  Widget remoteUrl() {
    return SingleChildScrollView(
      child: _tab([
        Text(
          'Sample 1: ($kUrl1)',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        PlayerWidget(url: kUrl1),
        Text(
          'Sample 2: ($kUrl2)',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        PlayerWidget(url: kUrl2),
        Text(
          'Sample 3: ($kUrl3)',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        PlayerWidget(url: kUrl3),
        Text(
          'Sample 4: playlist of (samples 1-3)',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        PlayerWidget(urls: urls),
        Text(
          "Credit to www.bensound.com for the music and images",
          style: TextStyle(
            color: Colors.grey,
            fontSize: 12,
          ),
        ),
      ]),
    );
  }

  Widget localFile() {
    return _tab([
      Text('File: $kUrl1'),
      _btn('Download File to your Device', () => _loadFile()),
      Text('Current local file path: $localFilePath'),
      localFilePath == null ? Container() : PlayerWidget(url: localFilePath),
      Text(
        "Credit to www.bensound.com for the music and images",
        style: TextStyle(
          color: Colors.grey,
          fontSize: 12,
        ),
      ),
    ]);
  }

  Widget localAndRemote() {
    return _tab([
      Text('File: $kUrl1'),
      _btn('Download File to your Device', () => _loadFile()),
      localFilePath == null
          ? Container()
          : PlayerWidget(urls: [localFilePath, kUrl2, kUrl3]),
      Text(
        "Credit to www.bensound.com for the music and images",
        style: TextStyle(
          color: Colors.grey,
          fontSize: 12,
        ),
      ),
    ]);
  }

  Widget _btn(String txt, VoidCallback onPressed) {
    return ButtonTheme(
      minWidth: 48.0,
      child: RaisedButton(child: Text(txt), onPressed: onPressed),
      buttonColor: Colors.pink,
    );
  }

  Future<void> _loadFile() async {
    final bytes = await readBytes(kUrl1);
    final dir = await getApplicationDocumentsDirectory();
    final file = File('${dir.path}/audio.mp3');

    await file.writeAsBytes(bytes);
    if (await file.exists()) {
      setState(() {
        localFilePath = file.path;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          backgroundColor: Colors.pink,
          bottomOpacity: 1.0,
          bottom: TabBar(
            indicatorColor: Colors.pink,
            tabs: [
              Tab(text: 'Remote Url'),
              Tab(text: 'Local File'),
              Tab(text: 'Local & Remote Mix'),
            ],
          ),
          title: Text('flutter_exoplayer Example'),
        ),
        body: TabBarView(
          children: [
            remoteUrl(),
            localFile(),
            localAndRemote(),
          ],
        ),
      ),
    );
  }
}
