#import "AudioPlayerPlugin.h"
#import <UIKit/UIKit.h>
#import <AVKit/AVKit.h>
#import <AVFoundation/AVFoundation.h>
#import "DOUAudioStreamer.h"
#import "STKAudioPlayer.h"
#import "Track.h"

#if TARGET_OS_IPHONE
    #import <MediaPlayer/MediaPlayer.h>
#endif

static NSString *const CHANNEL_NAME = @"danielr2001/audioplayer";


static void *kStatusKVOKey = &kStatusKVOKey;
static void *kDurationKVOKey = &kDurationKVOKey;
static void *kBufferingRatioKVOKey = &kBufferingRatioKVOKey;


@interface AudioPlayerPlugin()
-(void) pause: (NSString *) playerId;
-(void) stop: (NSString *) playerId;
-(void) release: (NSString *) playerId;
-(void) resume: (NSString *) playerId;
-(void) seek: (NSString *) playerId time: (double) time;
-(void) onSoundComplete: (NSString *) playerId;
@end

typedef void (^VoidCallback)(NSString * playerId);

NSMutableSet *timeobservers;
FlutterMethodChannel *_channel_audioplayer;

MPNowPlayingInfoCenter *_infoCenter;
MPRemoteCommandCenter *remoteCommandCenter;

@implementation AudioPlayerPlugin {

    
  FlutterResult _result;
    
    NSMutableArray<Track *> *_tracks;
    DOUAudioStreamer *_streamer;
    STKAudioPlayer *_player;
    Track *_currentTrack;
    NSTimer *_timer;
    
}



typedef void (^VoidCallback)(NSString * playerId);

NSMutableSet *timeobservers;
FlutterMethodChannel *_channel_audioplayer;

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
                                   methodChannelWithName:CHANNEL_NAME
                                   binaryMessenger:[registrar messenger]];
  AudioPlayerPlugin* instance = [[AudioPlayerPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
  _channel_audioplayer = channel;
}

- (id)init {
  self = [super init];
  if (self) {
            
  }
  return self;
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    
    
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
    return;
    
  }
  
  NSString * playerId = call.arguments[@"playerId"];
  //NSLog(@"iOS => call %@, playerId %@", call.method, playerId);

    typedef void (^CaseBlock)(void);
    
//    NSLog(@"______method!______%@", call.method);
//    NSLog(@"______arguments!______%@", call.arguments);
    

    // Squint and this looks like a proper switch!
    NSDictionary *methods = @{
      @"playAll":
      ^{
          
          NSArray *urls = call.arguments[@"urls"];
          NSArray *largeIconUrls = call.arguments[@"largeIconUrls"];
          NSArray *titles = call.arguments[@"titles"];
          NSArray *subTitles = call.arguments[@"subTitles"];
          
          
          if (urls == nil) {
              result(0);
          }
          
          if (self->_tracks != nil) {
              self->_tracks = nil;
          }
          
          self->_tracks = [NSMutableArray array];
          
          [urls enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
              Track *t = [[Track alloc] init];
              t.playerId = playerId;
              t.title = titles[idx];
              t.album = subTitles[idx];
              t.image = largeIconUrls[idx];
              t.audioFileURL = [NSURL URLWithString:obj];
              
              [self->_tracks addObject:t];
          }];
          
          int index = [call.arguments[@"index"] intValue];
          self->_currentTrack = self->_tracks[index];
          
          // start init audio player object
          [self _resetStreamer];

            
      },
      @"play":
      ^{
          
          [self->_player play:_currentTrack.audioFileURL.absoluteString];
      },
        @"pause":
        ^{
          NSLog(@"pause");
            [self pause:self->_currentTrack.playerId];
        },
        @"resume":
        ^{
          NSLog(@"resume");
            [self resume:self->_currentTrack.playerId];
        },
        @"stop":
        ^{
          NSLog(@"stop");
            [self stop:self->_currentTrack.playerId];
        },
      
        @"next":
          ^{
            NSLog(@"next");
              [self next:self->_currentTrack.playerId];
          },
        @"previous":
          ^{
            NSLog(@"previous");
              [self previous:self->_currentTrack.playerId];
        },
        @"release":
        ^{
            NSLog(@"release");
            [self release:self->_currentTrack.playerId];
        },
        @"seekPosition":
        ^{
          NSLog(@"seekPosition");
          if (!call.arguments[@"position"]) {
            result(0);
          } else {
            double milliseconds = call.arguments[@"position"] == [NSNull null] ? 0.0 : [call.arguments[@"position"] doubleValue];
              double second = milliseconds / 1000;
              //NSLog(@"_____second____%f", second);
            [self seek:playerId time: second];
          }
        },
        @"setAudioObject":
        ^{
          NSLog(@"setAudioObject");
        },
        @"setAudioObjects":
        ^{
          NSLog(@"setAudioObjects");
        }
    };

    CaseBlock c = methods[call.method];
    if (c) c(); else {
      NSLog(@"not implemented");
      result(FlutterMethodNotImplemented);
    }
}

- (void)_initTimer {
    
    [self _cancelTimer];
    
    _timer = [NSTimer scheduledTimerWithTimeInterval:1.0 target:self selector:@selector(_timerAction:) userInfo:nil repeats:YES];
}

- (void)_cancelTimer {
    if (_timer != nil) {
        [_timer invalidate];
        _timer = nil;
    }
}

- (void)_cancelStreamer
{
      if (_streamer != nil) {
        [_streamer pause];
        [_streamer removeObserver:self forKeyPath:@"status"];
        [_streamer removeObserver:self forKeyPath:@"duration"];
        [_streamer removeObserver:self forKeyPath:@"bufferingRatio"];
        _streamer = nil;
      }
    
    if (_player != nil) {
        [_player pause];
        _player.delegate = self;
        _player = nil;
    }
    
    [self _cancelTimer];
}

- (void)_resetStreamer {
    
    [self _cancelStreamer];

    if (0 != [_tracks count]) {
        
        [self _initTimer];
        
        _player = [[STKAudioPlayer alloc] init];
        [_player setMuted:false];
        _player.delegate = self;
                
        [_player play:_currentTrack.audioFileURL.absoluteString];
        
        [self _updateBufferingStatus];
        
    }
}

///    -1: PlayerState.RELEASED,
///    0: PlayerState.STOPPED,
///    1: PlayerState.BUFFERING,
///    2: PlayerState.PLAYING,
///    3: PlayerState.PAUSED,
///    4: PlayerState.COMPLETED,


- (void)_updateBufferingStatus
{
  
//    NSString *duration = [NSString stringWithFormat:@"Received %.2f/%.2f MB (%.2f %%), Speed %.2f MB/s", (double)[_streamer receivedLength] / 1024 / 1024, (double)[_streamer expectedLength] / 1024 / 1024, [_streamer bufferingRatio] * 100.0, (double)[_streamer downloadSpeed] / 1024 / 1024];

  if ([_streamer bufferingRatio] >= 1.0) {
    //NSLog(@"sha256: %@", [_streamer sha256]);
  }
}

-(void) playerIndexChanged: (NSInteger)newIndex {
    [_channel_audioplayer invokeMethod:@"audio.onCurrentPlayingAudioIndexChange" arguments:@{ @"playerId": _currentTrack.playerId, @"value": @(newIndex)}];

}


-(void) pause: (NSString *) playerId {
    
    [_player pause];
    [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @3 }];
}

-(void) resume: (NSString *) playerId {
    
    [_player resume];
    [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @2 }];
}

-(void) next: (NSString *) playerId {
    
    NSInteger currentTrackIndex = [_tracks indexOfObject:_currentTrack];
    
    if (++ currentTrackIndex < [_tracks count]) {
        _currentTrack = _tracks[currentTrackIndex];
    } else {
        _currentTrack = _tracks[0];
    }
    [self playerIndexChanged:currentTrackIndex];
    [self _resetStreamer];
}

-(void) previous: (NSString *) playerId {
    
    int currentTrackIndex = (int)[_tracks indexOfObject:_currentTrack];
    
    if (-- currentTrackIndex >= 0) {
        _currentTrack = _tracks[currentTrackIndex];
    } else {
        _currentTrack = _tracks[0];
    }

    [self playerIndexChanged:currentTrackIndex];
    [self _resetStreamer];
}


-(void) stop: (NSString *) playerId {
  
    if ([_player state] == STKAudioPlayerStatePlaying) {
        [_player stop];
    }
    [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @0 }];
}

-(void) release: (NSString *) playerId {

  if ([_player state] == STKAudioPlayerStatePlaying) {
    [_player pause];
      [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": _currentTrack.playerId, @"value": @-1 }];
  }
}

-(void) seek: (NSString *) playerId
        time: (double) time {
  
    NSLog(@"____Seek_time_____%f__", time);
    
    [_player seekToTime:time];
}

- (void)_timerAction:(id)timer {
    
    
    if ([_player duration] != 0.0) {
        //NSLog(@"___________duration____%f", [_player duration]);
        [self updateDuration];
    }
    
    NSInteger mSecond = [_player progress] * 1000;
    [_channel_audioplayer invokeMethod:@"audio.onCurrentPositionChanged" arguments:@{@"playerId": _currentTrack.playerId, @"value": @((int)mSecond)}];
    
    if ([_player state] == STKAudioPlayerStatePlaying) {
        [self updateNotification];
    }

}


-(void) onSoundComplete: (NSString *) playerId {
  NSLog(@"ios -> onSoundComplete...");
  [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @4 }];

//  [ _channel_audioplayer invokeMethod:@"audio.onComplete" arguments:@{@"playerId": playerId}];
}

- (void)dealloc {
  
    [self _cancelStreamer];
    _streamer = nil;
    _player = nil;
}

#if TARGET_OS_IPHONE
    -(void) setNotification: (int) duration
            elapsedTime:  (int) elapsedTime {
        

        _infoCenter = [MPNowPlayingInfoCenter defaultCenter];
        
        if (remoteCommandCenter == nil) {
          remoteCommandCenter = [MPRemoteCommandCenter sharedCommandCenter];

          MPRemoteCommand *nextCommand = [remoteCommandCenter nextTrackCommand];
          [nextCommand setEnabled:YES];
          [nextCommand addTarget:self action:@selector(nextCommand:)];
            
          MPRemoteCommand *previousCommand = [remoteCommandCenter previousTrackCommand];
          [previousCommand setEnabled:YES];
          [previousCommand addTarget:self action:@selector(previousCommand:)];
            
          MPRemoteCommand *pauseCommand = [remoteCommandCenter pauseCommand];
          [pauseCommand setEnabled:YES];
          [pauseCommand addTarget:self action:@selector(playOrPauseEvent:)];
          
          MPRemoteCommand *playCommand = [remoteCommandCenter playCommand];
          [playCommand setEnabled:YES];
          [playCommand addTarget:self action:@selector(playOrPauseEvent:)];

          MPRemoteCommand *togglePlayPauseCommand = [remoteCommandCenter togglePlayPauseCommand];
          [togglePlayPauseCommand setEnabled:YES];
          [togglePlayPauseCommand addTarget:self action:@selector(playOrPauseEvent:)];
        }
    }

    -(MPRemoteCommandHandlerStatus) nextCommand: (MPSkipIntervalCommandEvent *) playOrPauseEvent {
        
        NSLog(@"playOrPauseEvent");
        
        [self next:_currentTrack.playerId];
        
        return MPRemoteCommandHandlerStatusSuccess;
    }

    -(MPRemoteCommandHandlerStatus) previousCommand: (MPSkipIntervalCommandEvent *) playOrPauseEvent {
        
        NSLog(@"playOrPauseEvent");
        
        [self previous:_currentTrack.playerId];
        
        return MPRemoteCommandHandlerStatusSuccess;
    }


    -(MPRemoteCommandHandlerStatus) playOrPauseEvent: (MPSkipIntervalCommandEvent *) playOrPauseEvent {
        
        NSLog(@"playOrPauseEvent");
        
        if ([_streamer status] == DOUAudioStreamerPaused) {
            [_streamer play];
        } else if ([_streamer status] == DOUAudioStreamerPlaying) {
            [_streamer pause];
        }
        
        return MPRemoteCommandHandlerStatusSuccess;
    }

    -(void) updateNotification {
        
      NSMutableDictionary *playingInfo = [NSMutableDictionary dictionary];
        playingInfo[MPMediaItemPropertyTitle] = _currentTrack.title;
        playingInfo[MPMediaItemPropertyAlbumTitle] = _currentTrack.album;
      playingInfo[MPMediaItemPropertyArtist] = _currentTrack.artist;
      
      // fetch notification image in async fashion to avoid freezing UI
      dispatch_queue_t queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
      dispatch_async(queue, ^{
          NSURL *url = [[NSURL alloc] initWithString: _currentTrack.image];
          UIImage *artworkImage = [_currentTrack.image hasPrefix:@"http"] ? [UIImage imageWithData:[NSData dataWithContentsOfURL:url]] : [UIImage imageWithContentsOfFile: _currentTrack.image];
          if (artworkImage)
          {
              MPMediaItemArtwork *albumArt = [[MPMediaItemArtwork alloc] initWithImage: artworkImage];
              playingInfo[MPMediaItemPropertyArtwork] = albumArt;
          }

          playingInfo[MPMediaItemPropertyPlaybackDuration] = [NSNumber numberWithInt: [_streamer duration]];
          
          playingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = [NSNumber numberWithInt: [_streamer currentTime]];

//          playingInfo[MPNowPlayingInfoPropertyPlaybackRate] = @(_defaultPlaybackRate);
          
//          NSLog(@"setNotification done");

          if (_infoCenter != nil) {
            _infoCenter.nowPlayingInfo = playingInfo;
          }
      });
    }
#endif

- (void)updateDuration {
    NSInteger mSecond = [_player duration] * 1000;
    [_channel_audioplayer invokeMethod:@"audio.onDurationChanged" arguments:@{@"playerId": _currentTrack.playerId, @"value": @((int)mSecond)}];
    
    [self setNotification:[_player duration] elapsedTime:10];
}

- (void)audioPlayer:(nonnull STKAudioPlayer *)audioPlayer didFinishBufferingSourceWithQueueItemId:(nonnull NSObject *)queueItemId {
    NSLog(@"didFinishBufferingSourceWithQueueItemId");
}

- (void)audioPlayer:(nonnull STKAudioPlayer *)audioPlayer didFinishPlayingQueueItemId:(nonnull NSObject *)queueItemId withReason:(STKAudioPlayerStopReason)stopReason andProgress:(double)progress andDuration:(double)duration {
    
    NSInteger currentIndex  = [_tracks indexOfObject:_currentTrack];
    if (++ currentIndex < [_tracks count]) {
        [self next:_currentTrack.playerId];
    }
    
    
}

- (void)audioPlayer:(nonnull STKAudioPlayer *)audioPlayer didStartPlayingQueueItemId:(nonnull NSObject *)queueItemId {
    NSLog(@"didStartPlayingQueueItemId");
}

- (void)audioPlayer:(nonnull STKAudioPlayer *)audioPlayer stateChanged:(STKAudioPlayerState)state previousState:(STKAudioPlayerState)previousState {
    NSLog(@"stateChanged");
    
      int status = -1;
      
    switch (state) {
            
       case STKAudioPlayerStateDisposed:
           status = 0;
           break;
            
       case STKAudioPlayerStateBuffering:
            status = 1;
            break;
            
        case STKAudioPlayerStatePlaying:
            status = 2;
            break;
            
        case STKAudioPlayerStatePaused:
            status = 3;
            break;

        case STKAudioPlayerStateStopped: {
            status = 0;
            break;
        }
        case STKAudioPlayerStateError:
            status = 5;
            break;
        case STKAudioPlayerStateReady:
            status = 6;
            break;
        case STKAudioPlayerStateRunning:
            status = 7;
            break;
    }
      
      [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": _currentTrack.playerId, @"value": @(status)}];

}

- (void)audioPlayer:(nonnull STKAudioPlayer *)audioPlayer unexpectedError:(STKAudioPlayerErrorCode)errorCode {
    NSLog(@"unexpectedError");
}

@end
