#import <Flutter/Flutter.h>
#import "DOUAudioStreamer.h"
#import "STKAudioPlayer.h"


@interface AudioPlayerPlugin : NSObject<FlutterPlugin, STKAudioPlayerDelegate>

@property (readwrite, unsafe_unretained) id<STKAudioPlayerDelegate> delegate;

@end
