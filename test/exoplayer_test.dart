import 'package:exoplayer/exoplayer.dart';
import 'package:flutter/services.dart';
import 'package:test/test.dart';

void main() {
  List<MethodCall> calls = [];
  const channel = const MethodChannel('com.daniel/exoplayer');
  channel.setMockMethodCallHandler((MethodCall call) {
    calls.add(call);
  });

  group('ExoPlayer', () {
    test('#play', () async {
      calls.clear();
      ExoPlayer player = ExoPlayer();
      await player.play('internet.com/file.mp3');
      expect(calls, hasLength(1));
      expect(calls[0].method, 'play');
      expect(calls[0].arguments['url'], 'internet.com/file.mp3');
    });
  });
}
