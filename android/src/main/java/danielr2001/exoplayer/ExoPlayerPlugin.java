package danielr2001.exoplayer;

import danielr2001.exoplayer.audioplayers.ForegroundExoPlayer;
import danielr2001.exoplayer.audioplayers.BackgroundExoPlayer;
import danielr2001.exoplayer.interfaces.AudioPlayer;
import danielr2001.exoplayer.models.AudioObject;
import danielr2001.exoplayer.enums.NotificationMode;
import danielr2001.exoplayer.enums.PlayerState;

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


enum PlayerMode {
  PLAYLIST,
  SINGLE,
}


public class ExoPlayerPlugin implements MethodCallHandler {

  private static final Logger LOGGER = Logger.getLogger(ExoPlayerPlugin.class.getCanonicalName());

  private final MethodChannel channel;
  private final Handler handler = new Handler();
  private Runnable positionUpdates;

  private final Map<String, AudioPlayer> audioPlayers = new HashMap<>();
  private Context context;
  private Activity activity;

  private PlayerMode playerMode;
  private boolean repeatMode;
  private boolean respectAudioFocus;
  private float volume;
  private AudioObject audioObject;  //! TODO set cleanup for player and audioObjects
  private final ArrayList<AudioObject> audioObjects = new ArrayList<>();

  private String playerId;
  private AudioPlayer player;

  private ExoPlayerPlugin exoPlayerPlugin;

  private ServiceConnection connection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      ForegroundExoPlayer.LocalBinder binder = (ForegroundExoPlayer.LocalBinder) service;
      player = binder.getService();
      player.initAudioPlayer(exoPlayerPlugin, activity, playerId);
      audioPlayers.put(playerId, player);

      if (playerMode == PlayerMode.PLAYLIST) {
        player.playAll(repeatMode, respectAudioFocus, audioObjects);
      } else {
        player.play(repeatMode, respectAudioFocus, audioObject);
      }

      player.setVolume(volume);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
    }
  };

  public static void registerWith(final Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "danielr2001/exoplayer");
    channel.setMethodCallHandler(new ExoPlayerPlugin(channel, registrar.activity()));
  }

  private ExoPlayerPlugin(final MethodChannel channel, Activity activity) {
    this.channel = channel;
    this.activity = activity;
    this.context = activity.getApplicationContext();
    this.exoPlayerPlugin = this;
    this.channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(final MethodCall call, final MethodChannel.Result response) {
    try {
      handleMethodCall(call, response);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Unexpected error!", e);
      response.error("Unexpected error!", e.getMessage(), e);
    }
  }

  private void handleMethodCall(final MethodCall call, final MethodChannel.Result response) {
    this.player = null; //cleaning up the player
    this.playerId = call.argument("playerId");
    if(audioPlayers.containsKey(playerId)){
      this.player = getPlayer(playerId);
    }
    if(call.method.equals("play") || call.method.equals("playAll") || this.player != null){ // check if player is released then do nothing
      switch (call.method) {
        case "play": {
          final String url = call.argument("url");
          final double vol = call.argument("volume");
          this.volume = (float) vol;
          this.repeatMode = call.argument("repeatMode");
          final boolean isBackground = call.argument("isBackground");
          this.respectAudioFocus = call.argument("respectAudioFocus");
          playerMode = PlayerMode.SINGLE;
          if (isBackground) {
            // init player as BackgroundExoPlayer instance
            this.audioObject = new AudioObject(url);
            if(player != null && !player.isPlayerReleased()){
              player.play(this.repeatMode, this.respectAudioFocus, this.audioObject);
            }else{
              player = new BackgroundExoPlayer();
              player.initAudioPlayer(this, this.activity, playerId);
              audioPlayers.put(playerId, player);
              player.play(this.repeatMode, this.respectAudioFocus, this.audioObject);
            }
            
          } else {
            final String smallIconFileName = call.argument("smallIconFileName");
            final String title = call.argument("title");
            final String subTitle = call.argument("subTitle");
            final String largeIconUrl = call.argument("largeIconUrl");
            final boolean isLocal = call.argument("isLocal");
            final int notificationModeInt = call.argument("notificationMode");
            NotificationMode notificationMode;
            if (notificationModeInt == 0) {
              notificationMode = NotificationMode.NONE;
            } else if (notificationModeInt == 1) {
              notificationMode = NotificationMode.NEXT;
            } else if (notificationModeInt == 2){
              notificationMode = NotificationMode.PREVIOUS;
            }else{
              notificationMode = NotificationMode.BOTH;
            }

            this.audioObject = new AudioObject(url, smallIconFileName, title, subTitle, largeIconUrl, isLocal, notificationMode);
            // init player as ForegroundExoPlayer service
            if(player != null && !player.isPlayerReleased()){
              player.play(this.repeatMode, this.respectAudioFocus, this.audioObject);
            }else{
              startForegroundPlayer();
            }
          }
          break;
        }
        case "playAll": {
          final ArrayList<String> urls = call.argument("urls");
          final double vol = call.argument("volume");
          this.volume = (float) vol;
          this.repeatMode = call.argument("repeatMode");
          final boolean isBackground = call.argument("isBackground");
          this.respectAudioFocus = call.argument("respectAudioFocus");
          playerMode = PlayerMode.PLAYLIST;
          if (isBackground) {
            // init player as BackgroundExoPlayer instance
            for(String url : urls){
              this.audioObjects.add(new AudioObject(url));
            }
            if(player != null && !player.isPlayerReleased()){
              player.playAll(this.repeatMode, this.respectAudioFocus, this.audioObjects);
            }else{
              player = new BackgroundExoPlayer();
              player.initAudioPlayer(this, this.activity, playerId);
              audioPlayers.put(playerId, player);
              player.playAll(this.repeatMode, this.respectAudioFocus, this.audioObjects);
            }
          } else {
            final ArrayList<String> smallIconFileNames = call.argument("smallIconFileNames");
            final ArrayList<String> titles = call.argument("titles");
            final ArrayList<String> subTitles = call.argument("subTitles");
            final ArrayList<String> largeIconUrls = call.argument("largeIconUrls");
            final ArrayList<Boolean> isLocals = call.argument("isLocals");
            final ArrayList<Integer> notificationModeInts = call.argument("notificationModes");

            for(int i = 0; i < urls.size(); i++ ){
              NotificationMode notificationMode;
              if (notificationModeInts.get(i) == 0) {
                notificationMode = NotificationMode.NONE;
              } else if (notificationModeInts.get(i) == 1) {
                notificationMode = NotificationMode.NEXT;
              } else if (notificationModeInts.get(i) == 2){
                notificationMode = NotificationMode.PREVIOUS;
              }else{
                notificationMode = NotificationMode.BOTH;
              }
              this.audioObjects.add(new AudioObject(urls.get(i), smallIconFileNames.get(i), titles.get(i), subTitles.get(i), largeIconUrls.get(i), isLocals.get(i), notificationMode));
            }
            // init player as ForegroundExoPlayer service
            if(player != null && !player.isPlayerReleased()){
              player.playAll(this.repeatMode, this.respectAudioFocus, this.audioObjects);
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
        case "seek": {
          final int position = call.argument("position");
          player.seek(position);
          break;
        }
        case "setVolume": {
          final Float volume = call.argument("volume");
          player.setVolume(volume);
          break;
        }
        case "getDuration": {
          response.success(player.getDuration());
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
      response.success(1);
    }else{
      response.success(0);
    }
  }


  public void handlePlayerIndex(){
    channel.invokeMethod("audio.onCurrentPlayingAudioIndex",buildArguments(player.getPlayerId(), player.getCurrentPlayingAudioIndex()));
  }

  public void handleStateChange(AudioPlayer player, PlayerState playerState) {
    switch (playerState) {
      case RELEASED: { // -1
        channel.invokeMethod("audio.onStateChanged",buildArguments(player.getPlayerId(), -1));
        break;
      }
      case STOPPED: { // 0
        channel.invokeMethod("audio.onStateChanged", buildArguments(player.getPlayerId(), 0));
        break;
      }
      case BUFFERING: { // 1
        channel.invokeMethod("audio.onStateChanged",buildArguments(player.getPlayerId(), 1));
        break;
      }
      case PLAYING: { // 2
        channel.invokeMethod("audio.onStateChanged", buildArguments(player.getPlayerId(), 2));
        break;
      }
      case PAUSED: { // 3
        channel.invokeMethod("audio.onStateChanged",buildArguments(player.getPlayerId(), 3));
        break;
      }
      case COMPLETED: { // 4
        channel.invokeMethod("audio.onStateChanged",buildArguments(player.getPlayerId(), 4));
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
    if(!isMyServiceRunning(ForegroundExoPlayer.class)){
      ContextCompat.startForegroundService(this.context, new Intent(this.context, ForegroundExoPlayer.class));
      this.context.bindService(new Intent(this.context, ForegroundExoPlayer.class), connection, Context.BIND_AUTO_CREATE);
    }else{
      Log.e("ExoPlayerPlugin", "Can't start more than 1 service at a time");
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
    private final WeakReference<ExoPlayerPlugin> exoPlayerPlugin;

    private UpdateCallback(final Map<String, AudioPlayer> audioPlayers, final MethodChannel channel, final Handler handler,
        final ExoPlayerPlugin exoPlayerPlugin) {
      this.audioPlayers = new WeakReference<>(audioPlayers);
      this.channel = new WeakReference<>(channel);
      this.handler = new WeakReference<>(handler);
      this.exoPlayerPlugin = new WeakReference<>(exoPlayerPlugin);
    }

    @Override
    public void run() {
      final Map<String, AudioPlayer> audioPlayers = this.audioPlayers.get();
      final MethodChannel channel = this.channel.get();
      final Handler handler = this.handler.get();
      final ExoPlayerPlugin exoPlayerPlugin = this.exoPlayerPlugin.get();

      if (audioPlayers == null || channel == null || handler == null || exoPlayerPlugin == null) {
        if (exoPlayerPlugin != null) {
          exoPlayerPlugin.stopPositionUpdates();
        }
        return;
      }
      
      boolean nonePlaying = true;
      for (AudioPlayer player : audioPlayers.values()) {
          if (!player.isPlaying()) {
              continue;
          }
          try {
              nonePlaying = false;
              final String key = player.getPlayerId();
              final long position = player.getCurrentPosition();
                channel.invokeMethod("audio.onDurationChanged",buildArguments(player.getPlayerId(), player.getDuration()));
                channel.invokeMethod("audio.onCurrentPositionChanged", buildArguments(key, position)); 
          } catch(UnsupportedOperationException e) {

          }
      }

      if (nonePlaying) {
          exoPlayerPlugin.stopPositionUpdates();
      } else {
          handler.postDelayed(this, 200);
      }
    }
  }
}
