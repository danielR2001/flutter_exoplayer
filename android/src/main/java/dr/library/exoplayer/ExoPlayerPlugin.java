package dr.library.exoplayer;

import android.app.Activity;
import android.os.Handler;
import android.os.Build;
import android.os.IBinder;
import android.content.ServiceConnection;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;

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

enum PlayerState {
  PLAYING, PAUSED, COMPLETED, STOPPED, RELEASED,
}

public class ExoPlayerPlugin implements MethodCallHandler {

  private static final Logger LOGGER = Logger.getLogger(ExoPlayerPlugin.class.getCanonicalName());

  private final MethodChannel channel;
  private final Handler handler = new Handler();
  private Runnable positionUpdates;

  private final Map<String, AudioPlayer> audioPlayers = new HashMap<>();
  private Context context;

  private PlayerMode playerMode;
  private boolean repeatMode;
  private boolean respectAudioFocus;
  private float volume;
  private AudioObject audioObject;
  private AudioObject[] audioObjects;

  private String playerId;
  private AudioPlayer player;

  private ExoPlayerPlugin exoPlayerPlugin;

  public static void registerWith(final Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "danielr2001/exoplayer");
    channel.setMethodCallHandler(new ExoPlayerPlugin(channel, registrar.activity()));
  }

  private ExoPlayerPlugin(final MethodChannel channel, Activity activity) {
    this.channel = channel;
    this.channel.setMethodCallHandler(this);
    this.context = activity.getApplicationContext();
    this.exoPlayerPlugin = this;
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
    this.playerId = call.argument("playerId");
    if(audioPlayers.containsKey(playerId)){
      this.player = getPlayer(playerId);
    }
    switch (call.method) {
    case "play": {
      final String url = call.argument("url");
      final double vol = call.argument("volume");
      this.volume = (float) vol;
      this.repeatMode = call.argument("repeatMode");
      final boolean isBackground = call.argument("isBackground");
      this.respectAudioFocus = call.argument("respectAudioFocus");
      if (isBackground) {
        // init player as BackgroundExoPlayer instance
        player = new BackgroundExoPlayer();
        player.initAudioPlayer(this, this.context, playerId);
        audioPlayers.put(playerId, player);
        player.play(this.repeatMode, this.respectAudioFocus, url);
        
      } else {
        final int smallIcon = call.argument("smallIcon");
        final String title = call.argument("title");
        final String subTitle = call.argument("subTitle");
        final String largeIconUrl = call.argument("largeIconUrl");
        final boolean isLocal = call.argument("isLocal");
        final int notificationModeInt = call.argument("notificationMode");
        NotificationMode notificationMode;
        if (notificationModeInt == 1) {
          notificationMode = NotificationMode.NEXT;
        } else if (notificationModeInt == 2) {
          notificationMode = NotificationMode.PREVIOUS;
        } else {
          notificationMode = NotificationMode.BOTH;
        }

        this.audioObject = new AudioObject(url, smallIcon, title, subTitle, largeIconUrl, isLocal, notificationMode);
        // init player as ForegroundExoPlayer service
        startForegroundService(playerId);
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
      if (isBackground) {
        // init player as BackgroundExoPlayer instance
        player = new BackgroundExoPlayer();
        player.initAudioPlayer(this, this.context, playerId);
        audioPlayers.put(playerId, player);
        player.playAll(this.repeatMode, this.respectAudioFocus, urls);
      } else {
        final ArrayList<Integer> smallIcons = call.argument("smallIcons");
        final ArrayList<String> titles = call.argument("titles");
        final ArrayList<String> subTitles = call.argument("subTitle");
        final ArrayList<String> largeIconUrls = call.argument("largeIconUrl");
        final ArrayList<Boolean> isLocals = call.argument("isLocal");
        final ArrayList<Integer> notificationModeInts = call.argument("notificationModes");

        for(int i = 0; i < urls.size(); i++ ){
          NotificationMode notificationMode;
          if (notificationModeInts.get(i) == 1) {
            notificationMode = NotificationMode.NEXT;
          } else if (notificationModeInts.get(i) == 2) {
            notificationMode = NotificationMode.PREVIOUS;
          } else {
            notificationMode = NotificationMode.BOTH;
          }
          this.audioObjects[i] = new AudioObject(urls.get(i), smallIcons.get(i), titles.get(i), subTitles.get(i), largeIconUrls.get(i), isLocals.get(i), notificationMode);
        }
        // init player as ForegroundExoPlayer service
        startForegroundService(playerId);
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
  }

  private AudioPlayer getPlayer(String playerId) {
    return audioPlayers.get(playerId);
}

  public void handleStateChange(AudioPlayer player, PlayerState playerState) {
    switch (playerState) {
      case PLAYING: { // 3
        channel.invokeMethod("audio.onStateChanged", buildArguments(player.getPlayerId(), 3));
        break;
      }
      case PAUSED: { // 2
        channel.invokeMethod("audio.onStateChanged",buildArguments(player.getPlayerId(), 2));
        break;
      }
      case COMPLETED: { // 1
        channel.invokeMethod("audio.onStateChanged",buildArguments(player.getPlayerId(), 1));
        break;
      }
      case STOPPED: { // 0
        channel.invokeMethod("audio.onStateChanged", buildArguments(player.getPlayerId(), 0));
        break;
      }
      case RELEASED: { // -1
        channel.invokeMethod("audio.onStateChanged",buildArguments(player.getPlayerId(), -1));
        break;
      }
    }
  }

  public void handlePositionUpdates() {
    startPositionUpdates();
  }

  public void handleDurationUpdates() {
    channel.invokeMethod("audio.onDurationChanged",buildArguments(player.getPlayerId(), player.getDuration()));
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

  private ServiceConnection connection = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      ForegroundExoPlayer.LocalBinder binder = (ForegroundExoPlayer.LocalBinder) service;
      player = binder.getService();
      player.initAudioPlayer(exoPlayerPlugin, context, playerId);
      audioPlayers.put(playerId, player);
      // AudioObject audioObject = new AudioObject(); init object
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

  private void dispose() {
    this.context.unbindService(connection);
  }

  private void startForegroundService(String playerId){
    ContextCompat.startForegroundService(this.context, new Intent(this.context, ForegroundExoPlayer.class).putExtra("playerId", playerId));
    context.bindService(new Intent(this.context, AudioPlayer.class), connection, Context.BIND_AUTO_CREATE);
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
