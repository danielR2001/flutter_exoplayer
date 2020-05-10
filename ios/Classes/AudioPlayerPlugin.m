#import "AudioPlayerPlugin.h"
#import <UIKit/UIKit.h>
#import <AVKit/AVKit.h>
#import <AVFoundation/AVFoundation.h>

static NSString *const CHANNEL_NAME = @"danielr2001/audioplayer";

static NSMutableDictionary * players;

@interface AudioPlayerPlugin()
-(void) pause: (NSString *) playerId;
-(void) stop: (NSString *) playerId;
-(void) release: (NSString *) playerId;
-(void) resume: (NSString *) playerId;
-(void) seek: (NSString *) playerId time: (CMTime) time;
-(void) onSoundComplete: (NSString *) playerId;
-(void) onTimeInterval: (NSString *) playerId time: (CMTime) time;
@end

typedef void (^VoidCallback)(NSString * playerId);

NSMutableSet *timeobservers;
FlutterMethodChannel *_channel_audioplayer;

@implementation AudioPlayerPlugin {
  FlutterResult _result;
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
      players = [[NSMutableDictionary alloc] init];
  }
  return self;
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
    return;
  }
  
  NSString * playerId = call.arguments[@"playerId"];
  NSLog(@"iOS => call %@, playerId %@", call.method, playerId);

    typedef void (^CaseBlock)(void);

    // Squint and this looks like a proper switch!
    NSDictionary *methods = @{
      @"playAll":
      ^{
        NSLog(@"playAll!");
          NSArray *urls = call.arguments[@"urls"];
          NSLog(@"%@", urls);
          if (urls == nil)
              result(0);

          int index = call.arguments[@"index"] == [NSNull null] ? 0 : [call.arguments[@"index"] intValue];
          bool isLocals = call.arguments[@"isLocals"] == [NSNull null] ? false : [call.arguments[@"isLocals"] boolValue];
          double milliseconds = call.arguments[@"position"] == [NSNull null] ? 0.0 : [call.arguments[@"position"] doubleValue];
          bool respectAudioFocus = [call.arguments[@"respectAudioFocus"]boolValue] ;
          CMTime time = CMTimeMakeWithSeconds(milliseconds / 1000,NSEC_PER_SEC);
          [self play:playerId url:urls[index] isLocal:isLocals time:time isNotification:respectAudioFocus];
      },
      @"play":
      ^{
          NSLog(@"play!");
          NSString *url = call.arguments[@"url"];
          if (url == nil)
              result(0);
          bool isLocals = call.arguments[@"isLocal"] == [NSNull null] ? false : [call.arguments[@"isLocal"] boolValue];
          double milliseconds = call.arguments[@"position"] == [NSNull null] ? 0.0 : [call.arguments[@"position"] doubleValue];
          bool respectAudioFocus = [call.arguments[@"respectAudioFocus"]boolValue] ;
          CMTime time = CMTimeMakeWithSeconds(milliseconds / 1000,NSEC_PER_SEC);
          [self play:playerId url:url isLocal:isLocals time:time isNotification:respectAudioFocus];
        },
        @"pause":
        ^{
          NSLog(@"pause");
          [self pause:playerId];
        },
        @"resume":
        ^{
          NSLog(@"resume");
          [self resume:playerId];
        },
        @"stop":
        ^{
          NSLog(@"stop");
          [self stop:playerId];
        },
        @"release":
        ^{
            NSLog(@"release");
            [self release:playerId];
        },
        @"seekPosition":
        ^{
          NSLog(@"seekPosition");
          if (!call.arguments[@"position"]) {
            result(0);
          } else {
            double milliseconds = call.arguments[@"position"] == [NSNull null] ? 0.0 : [call.arguments[@"position"] doubleValue];
            [self seek:playerId time:CMTimeMakeWithSeconds(milliseconds / 1000,NSEC_PER_SEC)];
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

    [ self initPlayerInfo:playerId ];
    CaseBlock c = methods[call.method];
    if (c) c(); else {
      NSLog(@"not implemented");
      result(FlutterMethodNotImplemented);
    }
}

-(void) initPlayerInfo: (NSString *) playerId {
  NSMutableDictionary * playerInfo = players[playerId];
  if (!playerInfo) {
    players[playerId] = [@{@"isPlaying": @false, @"volume": @(1.0), @"looping": @(false)} mutableCopy];
  }
}

-(void) setUrl: (NSString*) url
       isLocal: (bool) isLocal
       playerId: (NSString*) playerId
       onReady:(VoidCallback)onReady
{
  NSMutableDictionary * playerInfo = players[playerId];
  //AVQueuePlayer for play multiple audio files.
  AVPlayer *player = playerInfo[@"player"];
  NSMutableSet *observers = playerInfo[@"observers"];
  AVPlayerItem *playerItem;
    
  NSLog(@"setUrl %@", url);

  if (!playerInfo || ![url isEqualToString:playerInfo[@"url"]]) {
    if (isLocal) {
      playerItem = [ [ AVPlayerItem alloc ] initWithURL:[ NSURL fileURLWithPath:url ]];
    } else {
      playerItem = [ [ AVPlayerItem alloc ] initWithURL:[ NSURL URLWithString:url ]];
    }
      
    if (playerInfo[@"url"]) {
      [[player currentItem] removeObserver:self forKeyPath:@"player.currentItem.status" ];

      [ playerInfo setObject:url forKey:@"url" ];

      for (id ob in observers) {
         [ [ NSNotificationCenter defaultCenter ] removeObserver:ob ];
      }
      [ observers removeAllObjects ];
      [ player replaceCurrentItemWithPlayerItem: playerItem ];
    } else {
      player = [[ AVPlayer alloc ] initWithPlayerItem: playerItem ];
      observers = [[NSMutableSet alloc] init];

      [ playerInfo setObject:player forKey:@"player" ];
      [ playerInfo setObject:url forKey:@"url" ];
      [ playerInfo setObject:observers forKey:@"observers" ];

      // playerInfo = [@{@"player": player, @"url": url, @"isPlaying": @false, @"observers": observers, @"volume": @(1.0), @"looping": @(false)} mutableCopy];
      // players[playerId] = playerInfo;

      // stream player position
      CMTime interval = CMTimeMakeWithSeconds(0.2, NSEC_PER_SEC);
      
      id timeObserver = [ player  addPeriodicTimeObserverForInterval: interval queue: nil usingBlock:^(CMTime time){
        [self onTimeInterval:playerId time:time];
      }];
        [timeobservers addObject:@{@"player":player, @"observer":timeObserver}];
    }
      
    id anobserver = [[ NSNotificationCenter defaultCenter ] addObserverForName: AVPlayerItemDidPlayToEndTimeNotification
                                                                        object: playerItem
                                                                         queue: nil
                                                                    usingBlock:^(NSNotification* note){
                                                                        [self onSoundComplete:playerId];
                                                                    }];
    [observers addObject:anobserver];
    // is sound ready
    [playerInfo setObject:onReady forKey:@"onReady"];
    [playerItem addObserver:self
                          forKeyPath:@"player.currentItem.status"
                          options:0
                          context:(void*)playerId];
      
  } else {
    if ([[player currentItem] status ] == AVPlayerItemStatusReadyToPlay) {
      onReady(playerId);
    }
  }
}

-(void) play: (NSString*) playerId
         url: (NSString*) url
     isLocal: (bool) isLocal
        time: (CMTime) time
      isNotification: (bool) respectSilence
{
    NSError *error = nil;
    AVAudioSessionCategory category;
    if (respectSilence) {
        category = AVAudioSessionCategoryAmbient;
    } else {
        category = AVAudioSessionCategoryPlayback;
    }
    BOOL success = [[AVAudioSession sharedInstance]
                    setCategory: category
                    withOptions:AVAudioSessionCategoryOptionMixWithOthers
                    error:&error];
  if (!success) {
    NSLog(@"Error setting speaker: %@", error);
  }
  [[AVAudioSession sharedInstance] setActive:YES error:&error];
  [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @1 }];
  [ self setUrl:url 
         isLocal:isLocal 
         playerId:playerId 
         onReady:^(NSString * playerId) {
           NSMutableDictionary * playerInfo = players[playerId];
           AVPlayer *player = playerInfo[@"player"];
           [ player seekToTime:time ];
           [ player play];
         }    
  ];
}

-(void) updateDuration: (NSString *) playerId
{
  NSMutableDictionary * playerInfo = players[playerId];
  AVPlayer *player = playerInfo[@"player"];

  CMTime duration = [[[player currentItem]  asset] duration];

  NSLog(@"ios -> updateDuration...%f", CMTimeGetSeconds(duration));
  if(CMTimeGetSeconds(duration)>0){
    NSLog(@"ios -> invokechannel");
   int mseconds= CMTimeGetSeconds(duration)*1000;
    [_channel_audioplayer invokeMethod:@"audio.onDurationChanged" arguments:@{@"playerId": playerId, @"value": @(mseconds)}];
  }
}

// No need to spam the logs with every time interval update
-(void) onTimeInterval: (NSString *) playerId
                  time: (CMTime) time {
    // NSLog(@"ios -> onTimeInterval...");
    int mseconds =  CMTimeGetSeconds(time)*1000;
    // NSLog(@"asdff %@ - %d", playerId, mseconds);
    [_channel_audioplayer invokeMethod:@"audio.onCurrentPositionChanged" arguments:@{@"playerId": playerId, @"value": @(mseconds)}];
    //    NSLog(@"asdff end");
}

-(void) pause: (NSString *) playerId {
  NSMutableDictionary * playerInfo = players[playerId];
  AVPlayer *player = playerInfo[@"player"];

  [ player pause ];
  [playerInfo setObject:@false forKey:@"isPlaying"];
  [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @3 }];
}

-(void) resume: (NSString *) playerId {
  NSMutableDictionary * playerInfo = players[playerId];
  AVPlayer *player = playerInfo[@"player"];
  [player play];
  [playerInfo setObject:@true forKey:@"isPlaying"];
  [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @2 }];
}

-(void) stop: (NSString *) playerId {
  NSMutableDictionary * playerInfo = players[playerId];

  if ([playerInfo[@"isPlaying"] boolValue]) {
    [ self pause:playerId ];
    [ self seek:playerId time:CMTimeMake(0, 1) ];
    [playerInfo setObject:@false forKey:@"isPlaying"];
    [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @0 }];
  }
}

-(void) release: (NSString *) playerId {
  NSMutableDictionary * playerInfo = players[playerId];

  if ([playerInfo[@"isPlaying"] boolValue]) {
    [ self pause:playerId ];
    [ self seek:playerId time:CMTimeMake(0, 1) ];
    [playerInfo setObject:@false forKey:@"isPlaying"];
    [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @-1 }];
  }
}

-(void) seek: (NSString *) playerId
        time: (CMTime) time {
  NSMutableDictionary * playerInfo = players[playerId];
  AVPlayer *player = playerInfo[@"player"];
  [[player currentItem] seekToTime:time];
}

-(void) onSoundComplete: (NSString *) playerId {
  NSLog(@"ios -> onSoundComplete...");
  [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @4 }];
  NSMutableDictionary * playerInfo = players[playerId];

  if (![playerInfo[@"isPlaying"] boolValue]) {
    return;
  }

  [ self pause:playerId ];
  [ self seek:playerId time:CMTimeMakeWithSeconds(0,1) ];

  if ([ playerInfo[@"looping"] boolValue]) {
    [ self resume:playerId ];
  }

  [ _channel_audioplayer invokeMethod:@"audio.onComplete" arguments:@{@"playerId": playerId}];
}

-(void)observeValueForKeyPath:(NSString *)keyPath
                     ofObject:(id)object
                       change:(NSDictionary *)change
                      context:(void *)context {
  if ([keyPath isEqualToString: @"player.currentItem.status"]) {
    NSString *playerId = (__bridge NSString*)context;
    NSMutableDictionary * playerInfo = players[playerId];
    AVPlayer *player = playerInfo[@"player"];

    AVPlayerItemStatus status = [[player currentItem] status ];

    NSLog(@"player status: %ld", (long)status);
    // Do something with the status…
    if (status == AVPlayerItemStatusReadyToPlay) {
      [self updateDuration:playerId];

      VoidCallback onReady = playerInfo[@"onReady"];
      if (onReady != nil) {
        [playerInfo removeObjectForKey:@"onReady"];  
        onReady(playerId);

        [_channel_audioplayer invokeMethod:@"audio.onStateChanged" arguments:@{ @"playerId": playerId, @"value": @2 }];
      }
    } else if (status == AVPlayerItemStatusFailed) {
      [_channel_audioplayer invokeMethod:@"audio.onError" arguments:@{@"playerId": playerId, @"value": @"AVPlayerItemStatus.failed"}];
      [ self release:playerId ];
    }
  } else {
    // Any unrecognized context must belong to super
    [super observeValueForKeyPath:keyPath
                         ofObject:object
                           change:change
                          context:context];
  }
}

- (void)dealloc {
  for (id value in timeobservers)
    [value[@"player"] removeTimeObserver:value[@"observer"]];
  timeobservers = nil;

  for (NSString* playerId in players) {
      NSMutableDictionary * playerInfo = players[playerId];
      NSMutableSet * observers = playerInfo[@"observers"];
      for (id ob in observers)
        [[NSNotificationCenter defaultCenter] removeObserver:ob];
  }
  players = nil;
}

@end
