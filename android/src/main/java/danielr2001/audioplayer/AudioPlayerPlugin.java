package danielr2001.audioplayer;

import danielr2001.audioplayer.audioplayers.ForegroundAudioPlayer;
import danielr2001.audioplayer.audioplayers.BackgroundAudioPlayer;
import danielr2001.audioplayer.interfaces.AudioPlayer;
import danielr2001.audioplayer.models.AudioObject;
import danielr2001.audioplayer.enums.NotificationDefaultActions;
import danielr2001.audioplayer.enums.NotificationCustomActions;
import danielr2001.audioplayer.enums.NotificationActionName;
import danielr2001.audioplayer.enums.NotificationActionCallbackMode;
import danielr2001.audioplayer.enums.PlayerState;
import danielr2001.audioplayer.enums.PlayerMode;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.Handler;
import android.os.Build;
import android.os.IBinder;
import android.content.ServiceConnection;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.PluginRegistry.Registrar;



public class AudioPlayerPlugin implements MethodCallHandler {

  private static final Logger LOGGER = Logger.getLogger(AudioPlayerPlugin.class.getCanonicalName());

  private final MethodChannel channel;
  private final Handler handler = new Handler();
  private Runnable positionUpdates;

  private final Map<String, AudioPlayer> audioPlayers = new HashMap<>();
  private Context context;
  private Activity activity;

  private PlayerMode playerMode;
  private AudioObject audioObject;  
  private final ArrayList<AudioObject> audioObjects = new ArrayList<>();

  //temp variables for foreground player
  private AudioPlayer tempPlayer;  
  private String tempPlayerId;
  private boolean tempRepeatMode;
  private boolean tempRespectAudioFocus;
  private AudioPlayerPlugin tempAudioPlayerPlugin;
  private int tempIndex;

  private ServiceConnection connection = new ServiceConnection() {

    @SuppressWarnings("unchecked")
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      ForegroundAudioPlayer.LocalBinder binder = (ForegroundAudioPlayer.LocalBinder) service;
      tempPlayer = binder.getService(); //just like  tempPlayer = ForegroundAudioPlayer();
      tempPlayer.initAudioPlayer(tempAudioPlayerPlugin, tempAudioPlayerPlugin.activity, tempPlayerId);
      tempPlayer.setPlayerAttributes(tempRepeatMode, tempRespectAudioFocus, playerMode);
      if (playerMode == PlayerMode.PLAYLIST) {
        tempPlayer.playAll((ArrayList<AudioObject>) audioObjects.clone(), tempIndex);
      } else {
        tempPlayer.play(audioObject);
      }
      audioPlayers.put(tempPlayerId, tempPlayer);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {}
  };

  public static void registerWith(final Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "danielr2001/audioplayer");
    channel.setMethodCallHandler(new AudioPlayerPlugin(channel, registrar.activity()));
  }

  private AudioPlayerPlugin(final MethodChannel channel, Activity activity) {
    this.channel = channel;
    this.activity = activity;
    this.context = activity.getApplicationContext();
    this.channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(final MethodCall call, final MethodChannel.Result response) {
    try {
      handleMethodCall(call, response);
    } catch (Exception e) {
      dispose();
      LOGGER.log(Level.SEVERE, "Unexpected error!", e);
      response.success(0); //error
    }
  }

  @SuppressWarnings("unchecked")
  private void handleMethodCall(final MethodCall call, final MethodChannel.Result response) {
    final String playerId = call.argument("playerId");
    AudioPlayer player = null;
    this.audioObjects.clear();
    this.audioObject = null;
    if(audioPlayers.containsKey(playerId)){        
      player = getPlayer(playerId);
    }
    if(call.method.equals("play") || call.method.equals("playAll") || player != null){ // check if player is released then do nothing
      switch (call.method) {                                                        
        case "play": {
          final String url = call.argument("url");
          final boolean repeatMode = call.argument("repeatMode");
          final boolean respectAudioFocus = call.argument("respectAudioFocus");
          final boolean isBackground = call.argument("isBackground");
          
          this.playerMode = PlayerMode.SINGLE;
          if (isBackground) {
            // init player as BackgroundAudioPlayer instance
            this.audioObject = new AudioObject(url);
            if(player != null && !player.isPlayerReleased()){
              player.play(this.audioObject);
            }else{
              player = new BackgroundAudioPlayer();
              player.initAudioPlayer(this, this.activity, playerId);
              player.setPlayerAttributes(repeatMode, respectAudioFocus, this.playerMode);
              player.play(this.audioObject);

              audioPlayers.put(playerId, player);
            }
            
          } else {
            final String smallIconFileName = call.argument("smallIconFileName");
            final String title = call.argument("title");
            final String subTitle = call.argument("subTitle");
            final String largeIconUrl = call.argument("largeIconUrl");
            final boolean isLocal = call.argument("isLocal");
            final int notificationDefaultActionsInt = call.argument("notificationDefaultActions");
            final int notificationActionCallbackModeInt = call.argument("notificationActionCallbackMode");
            final int notificationCustomActionsInt = call.argument("notificationCustomActions");

            this.tempPlayer = player;
            this.tempPlayerId = playerId;
            this.tempRepeatMode = repeatMode;
            this.tempRespectAudioFocus = respectAudioFocus;
            this.tempAudioPlayerPlugin = this;
            
            NotificationDefaultActions notificationDefaultActions;
            NotificationActionCallbackMode notificationActionCallbackMode;
            NotificationCustomActions notificationCustomActions;
            if (notificationDefaultActionsInt == 0) {
              notificationDefaultActions = NotificationDefaultActions.NONE;
            } else if (notificationDefaultActionsInt == 1) {
              notificationDefaultActions = NotificationDefaultActions.NEXT;
            } else if (notificationDefaultActionsInt == 2){
              notificationDefaultActions = NotificationDefaultActions.PREVIOUS;
            }else{
              notificationDefaultActions = NotificationDefaultActions.ALL;
            }

            if (notificationCustomActionsInt == 1) {
              notificationCustomActions = NotificationCustomActions.ONE;
            } else if (notificationCustomActionsInt == 2) {
              notificationCustomActions = NotificationCustomActions.TWO;
            } else {
              notificationCustomActions = NotificationCustomActions.DISABLED;
            }

            if(notificationActionCallbackModeInt == 0){
              notificationActionCallbackMode = NotificationActionCallbackMode.DEFAULT;
            }else{
              notificationActionCallbackMode = NotificationActionCallbackMode.CUSTOM;
            }

            this.audioObject = new AudioObject(url, smallIconFileName, title, subTitle, largeIconUrl, isLocal, notificationDefaultActions, notificationActionCallbackMode, notificationCustomActions);
            // init player as ForegroundAudioPlayer service
            if(player != null && !player.isPlayerReleased()){
              player.play(this.audioObject);
            }else{
              startForegroundPlayer();
            }
          }
          break;
        }
        case "playAll": {
          final ArrayList<String> urls = call.argument("urls");
          final boolean repeatMode = call.argument("repeatMode");
          final boolean isBackground = call.argument("isBackground");
          final boolean respectAudioFocus = call.argument("respectAudioFocus");
          final int index = call.argument("index");

          this.playerMode = PlayerMode.PLAYLIST;
          if (isBackground) {
            // init player as BackgroundAudioPlayer instance
            for(String url : urls){
              this.audioObjects.add(new AudioObject(url));
            }
            if(player != null && !player.isPlayerReleased()){
              player.playAll((ArrayList<AudioObject>) this.audioObjects.clone(), index);
            }else{
              player = new BackgroundAudioPlayer();
              player.initAudioPlayer(this, this.activity, playerId);
              player.setPlayerAttributes(repeatMode, respectAudioFocus, this.playerMode);
              player.playAll((ArrayList<AudioObject>) this.audioObjects.clone(), index);

              audioPlayers.put(playerId, player);
            }
          } else {
            final ArrayList<String> smallIconFileNames = call.argument("smallIconFileNames");
            final ArrayList<String> titles = call.argument("titles");
            final ArrayList<String> subTitles = call.argument("subTitles");
            final ArrayList<String> largeIconUrls = call.argument("largeIconUrls");
            final ArrayList<Boolean> isLocals = call.argument("isLocals");
            final ArrayList<Integer> notificationDefaultActionsInts = call.argument("notificationDefaultActionsList");
            final ArrayList<Integer> notificationActionCallbackModeInts = call.argument("notificationActionCallbackModes");
            final ArrayList<Integer> notificationCustomActionsInts = call.argument("notificationCustomActionsList");

            this.tempPlayer = player;
            this.tempPlayerId = playerId;
            this.tempRepeatMode = repeatMode;
            this.tempRespectAudioFocus = respectAudioFocus;
            this.tempAudioPlayerPlugin = this;
            this.tempIndex = index;

            for(int i = 0; i < urls.size(); i++ ){
              NotificationDefaultActions notificationDefaultActions;
              NotificationActionCallbackMode notificationActionCallbackMode;
              NotificationCustomActions notificationCustomActions;
              if (notificationDefaultActionsInts.get(i) == 0) {
                notificationDefaultActions = NotificationDefaultActions.NONE;
              } else if (notificationDefaultActionsInts.get(i) == 1) {
                notificationDefaultActions = NotificationDefaultActions.NEXT;
              } else if (notificationDefaultActionsInts.get(i) == 2){
                notificationDefaultActions = NotificationDefaultActions.PREVIOUS;
              }else{
                notificationDefaultActions = NotificationDefaultActions.ALL;
              }

              if (notificationCustomActionsInts.get(i) == 1) {
                notificationCustomActions = NotificationCustomActions.ONE;
              } else if (notificationCustomActionsInts.get(i) == 2) {
                notificationCustomActions = NotificationCustomActions.TWO;
              } else {
                notificationCustomActions = NotificationCustomActions.DISABLED;
              }

              if(notificationActionCallbackModeInts.get(i) == 0){
                notificationActionCallbackMode = NotificationActionCallbackMode.DEFAULT;
              }else{
                notificationActionCallbackMode = NotificationActionCallbackMode.CUSTOM;
              }

              this.audioObjects.add(new AudioObject(urls.get(i), smallIconFileNames.get(i), titles.get(i), subTitles.get(i), largeIconUrls.get(i), isLocals.get(i), notificationDefaultActions, notificationActionCallbackMode, notificationCustomActions));
            }
            // init player as ForegroundAudioPlayer service
            if(player != null && !player.isPlayerReleased()){
              player.playAll((ArrayList<AudioObject>) this.audioObjects.clone(), index);
            }else{
              startForegroundPlayer();
            }
          }
          break;
        }
        case "next": {
          player.next();
          break;
        }
        case "previous": {
          player.previous();
          break;
        }
        case "resume": {
          player.resume();
          break;
        }
        case "pause": {
          player.pause();
          break;
        }
        case "stop": {
          player.stop();
          break;
        }
        case "release": {
          if(!player.isBackground() && !player.isPlayerReleased()){
            this.context.unbindService(connection);
          }
          audioPlayers.remove(player.getPlayerId());
          player.release();
          break;
        }
        case "seekPosition": {
          final int position = call.argument("position");
          player.seekPosition(position);  
          break;
        }
        case "seekIndex": {
          final int index = call.argument("index");
          player.seekIndex(index);  
          break;
        }
        case "setVolume": {
          final double vol = call.argument("volume");
          final float volume = (float)vol;
          player.setVolume(volume);
          break;
        }
        case "setRepeatMode": {
          final boolean repeatMode = call.argument("repeatMode");
          player.setRepeatMode(repeatMode);
          break;
        }
        case "getVolume": {
          response.success(player.getVolume());
          return;
        }
        case "getDuration": {
          response.success(player.getDuration());
          return;
        }
        case "getCurrentPosition": {
          response.success(player.getCurrentPosition());
          return;
        }
        case "getCurrentPlayingAudioIndex": {
          response.success(player.getCurrentPlayingAudioIndex());
          return;
        }
        case "dispose": {
          dispose();
          return;
        }
        default: {
          response.notImplemented();
          return;
        }
      }
      response.success(2); //success
    }else{
      response.success(1); //fail
    }
  }

  public void handleNotificationActionCallback(AudioPlayer audioplayer, NotificationActionName notificationActionName){
      switch(notificationActionName){
        case PREVIOUS:
          channel.invokeMethod("audio.onNotificationActionCallback",buildArguments(audioplayer.getPlayerId(), 0));  
          break;
        case NEXT:
          channel.invokeMethod("audio.onNotificationActionCallback",buildArguments(audioplayer.getPlayerId(), 1));  
          break;
        case PLAY:
          channel.invokeMethod("audio.onNotificationActionCallback",buildArguments(audioplayer.getPlayerId(), 2));  
          break;
        case PAUSE:
          channel.invokeMethod("audio.onNotificationActionCallback",buildArguments(audioplayer.getPlayerId(), 3));  
          break;
        case CUSTOM1:
          channel.invokeMethod("audio.onNotificationActionCallback",buildArguments(audioplayer.getPlayerId(), 4));  
          break;
        case CUSTOM2:
          channel.invokeMethod("audio.onNotificationActionCallback",buildArguments(audioplayer.getPlayerId(), 5));  
          break;
      }
  }

  public void handleAudioSessionIdChange(AudioPlayer audioplayer, int audioSessionId){
    channel.invokeMethod("audio.onAudioSessionIdChange",buildArguments(audioplayer.getPlayerId(), audioSessionId));
  }

  public void handlePlayerIndex(AudioPlayer audioplayer) {
    channel.invokeMethod("audio.onCurrentPlayingAudioIndexChange",buildArguments(audioplayer.getPlayerId(), audioplayer.getCurrentPlayingAudioIndex()));
  }

  public void handleStateChange(AudioPlayer audioplayer, PlayerState playerState) {
    switch (playerState) {
      case RELEASED: { // -1
        channel.invokeMethod("audio.onStateChanged",buildArguments(audioplayer.getPlayerId(), -1));
        break;
      }
      case STOPPED: { // 0
        channel.invokeMethod("audio.onStateChanged", buildArguments(audioplayer.getPlayerId(), 0));
        break;
      }
      case BUFFERING: { // 1
        channel.invokeMethod("audio.onStateChanged",buildArguments(audioplayer.getPlayerId(), 1));
        break;
      }
      case PLAYING: { // 2
        channel.invokeMethod("audio.onStateChanged", buildArguments(audioplayer.getPlayerId(), 2));
        break;
      }
      case PAUSED: { // 3
        channel.invokeMethod("audio.onStateChanged",buildArguments(audioplayer.getPlayerId(), 3));
        break;
      }
      case COMPLETED: { // 4
        channel.invokeMethod("audio.onStateChanged",buildArguments(audioplayer.getPlayerId(), 4));
        break;
      }
    }
  }

  public void handlePositionUpdates() {
    startPositionUpdates();
  }

  
  private AudioPlayer getPlayer(String playerId) {
    return audioPlayers.get(playerId);
  }

  private void startForegroundPlayer(){
    if(!isMyServiceRunning(ForegroundAudioPlayer.class)){
      ContextCompat.startForegroundService(this.context, new Intent(this.context, ForegroundAudioPlayer.class));
      this.context.bindService(new Intent(this.context, ForegroundAudioPlayer.class), connection, Context.BIND_AUTO_CREATE);
    }else{
      Log.e("AudioPlayerPlugin", "Can't start more than 1 service at a time, to stop service call release method");
    }
  }
  
  private void startPositionUpdates() {
    if (positionUpdates != null) {
      return;
    }
    positionUpdates = new UpdateCallback(audioPlayers, channel, handler, this);
    handler.post(positionUpdates);
  }

  private void stopPositionUpdates() {
    positionUpdates = null;
    handler.removeCallbacksAndMessages(null);
  }

  private static Map<String, Object> buildArguments(String playerId, Object value) {
    Map<String, Object> result = new HashMap<>();
    result.put("playerId", playerId);
    result.put("value", value);
    return result;
}

  private void dispose() {
    for (AudioPlayer player : audioPlayers.values()) {
      if(player.isPlayerInitialized()){
        if(!player.isBackground() && !player.isPlayerReleased()){
          this.context.unbindService(connection);
        }
        player.release();
      }
    }
    audioPlayers.clear();
  }

  @SuppressWarnings( "deprecation" )
  private boolean isMyServiceRunning(Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) this.context.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.getName().equals(service.service.getClassName())) {
            return true;
        }
    }
    return false;
}
  private static final class UpdateCallback implements Runnable {

    private final WeakReference<Map<String, AudioPlayer>> audioPlayers;
    private final WeakReference<MethodChannel> channel;
    private final WeakReference<Handler> handler;
    private final WeakReference<AudioPlayerPlugin> audioPlayerPlugin;

    private UpdateCallback(final Map<String, AudioPlayer> audioPlayers, final MethodChannel channel, final Handler handler,
        final AudioPlayerPlugin audioPlayerPlugin) {
      this.audioPlayers = new WeakReference<>(audioPlayers);
      this.channel = new WeakReference<>(channel);
      this.handler = new WeakReference<>(handler);
      this.audioPlayerPlugin = new WeakReference<>(audioPlayerPlugin);
    }

    @Override
    public void run() {
      final Map<String, AudioPlayer> audioPlayers = this.audioPlayers.get();
      final MethodChannel channel = this.channel.get();
      final Handler handler = this.handler.get();
      final AudioPlayerPlugin audioPlayerPlugin = this.audioPlayerPlugin.get();

      if (audioPlayers == null || channel == null || handler == null || audioPlayerPlugin == null) {
        if (audioPlayerPlugin != null) {
          audioPlayerPlugin.stopPositionUpdates();
        }
        return;
      }
      
      boolean nonePlaying = true;
      for (AudioPlayer player : audioPlayers.values()) {
          if (!player.isPlaying()) {
            if(player.isPlayerCompleted()){
              channel.invokeMethod("audio.onDurationChanged",buildArguments(player.getPlayerId(), player.getDuration()));
            }
            continue;
          }
          try {
            nonePlaying = false;
            channel.invokeMethod("audio.onDurationChanged",buildArguments(player.getPlayerId(), player.getDuration()));
            channel.invokeMethod("audio.onCurrentPositionChanged", buildArguments(player.getPlayerId(), player.getCurrentPosition())); 
          } catch(UnsupportedOperationException e) {
            Log.e("AudioPlayerPlugin", "Error when updating position and duration");
          }
      }

      if (nonePlaying) {
          audioPlayerPlugin.stopPositionUpdates();
      } else {
          handler.postDelayed(this, 200);
      }
    }
  }
}
