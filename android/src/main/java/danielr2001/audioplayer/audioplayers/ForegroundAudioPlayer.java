package danielr2001.audioplayer.audioplayers;

import danielr2001.audioplayer.interfaces.AudioPlayer;
import danielr2001.audioplayer.notifications.MediaNotificationManager;
import danielr2001.audioplayer.AudioPlayerPlugin;
import danielr2001.audioplayer.models.AudioObject;
import danielr2001.audioplayer.enums.PlayerState;
import danielr2001.audioplayer.enums.NotificationActionName;
import danielr2001.audioplayer.enums.NotificationActionCallbackMode;
import danielr2001.audioplayer.enums.PlayerMode;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import android.util.Log;
public class ForegroundAudioPlayer extends Service implements AudioPlayer {
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public ForegroundAudioPlayer getService() {
            return ForegroundAudioPlayer.this;
        }
    }

    private ForegroundAudioPlayer foregroundAudioPlayer;
    private MediaNotificationManager mediaNotificationManager;
    private Context context;
    private AudioPlayerPlugin ref;
    private MediaSessionCompat mediaSession;
    private String playerId;

    //player attributes
    private float volume = 1;
    private boolean repeatMode;
    private boolean respectAudioFocus;
    private PlayerMode playerMode;

    //player states
    private boolean initialized = false;
    private boolean buffering = false;
    private boolean playing = false;
    private boolean stopped = false;
    private boolean released = true;
    private boolean completed = false;

    //ExoPlayer
    private SimpleExoPlayer player;

    private ArrayList<AudioObject> audioObjects;
    private AudioObject audioObject;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.context = getApplicationContext();
        mediaSession = new MediaSessionCompat(this.context, "playback");
        // ! TODO handle MediaButtonReceiver's callbacks
        // MediaButtonReceiver.handleIntent(mediaSession, intent);
        // mediaSession.setCallback(mediaSessionCallback);
        if (intent.getAction() != null) {
            AudioObject currentAudioObject;
            if (this.playerMode == PlayerMode.PLAYLIST) {
                currentAudioObject = this.audioObjects.get(player.getCurrentWindowIndex());
            } else {
                currentAudioObject = this.audioObject;
            }
            if (intent.getAction().equals(MediaNotificationManager.PREVIOUS_ACTION)) {
                if (currentAudioObject.getNotificationActionCallbackMode() == NotificationActionCallbackMode.DEFAULT) {
                    previous();
                } else {
                    ref.handleNotificationActionCallback(this.foregroundAudioPlayer, NotificationActionName.PREVIOUS);
                }
            } else if (intent.getAction().equals(MediaNotificationManager.PLAY_ACTION)) {
                if (currentAudioObject.getNotificationActionCallbackMode() == NotificationActionCallbackMode.DEFAULT) {
                    if(!stopped){
                        resume();
                    }else{
                        if(playerMode == PlayerMode.PLAYLIST){
                            playAll(audioObjects, 0);
                        }else{
                            play(audioObject);
                        }
                    }
                } else {
                    ref.handleNotificationActionCallback(this.foregroundAudioPlayer, NotificationActionName.PLAY);
                }
            } else if (intent.getAction().equals(MediaNotificationManager.PAUSE_ACTION)) {
                if (currentAudioObject.getNotificationActionCallbackMode() == NotificationActionCallbackMode.DEFAULT) {
                    pause();
                } else {
                    ref.handleNotificationActionCallback(this.foregroundAudioPlayer, NotificationActionName.PAUSE);
                }
            } else if (intent.getAction().equals(MediaNotificationManager.NEXT_ACTION)) {
                if (currentAudioObject.getNotificationActionCallbackMode() == NotificationActionCallbackMode.DEFAULT) {
                    next();
                } else {
                    ref.handleNotificationActionCallback(this.foregroundAudioPlayer, NotificationActionName.NEXT);
                }
            } else if(intent.getAction().equals(MediaNotificationManager.CUSTOM1_ACTION)) {
                ref.handleNotificationActionCallback(this.foregroundAudioPlayer, NotificationActionName.CUSTOM1);
            } else if(intent.getAction().equals(MediaNotificationManager.CUSTOM2_ACTION)){
                ref.handleNotificationActionCallback(this.foregroundAudioPlayer, NotificationActionName.CUSTOM2);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        this.release();
    }


    @Override
    public void initAudioPlayer(AudioPlayerPlugin ref, Activity activity, String playerId) {
        this.initialized = true;

        this.playerId = playerId;
        this.ref = ref;
        this.mediaNotificationManager = new MediaNotificationManager(this, this.context, this.mediaSession, activity);
        this.foregroundAudioPlayer = this;
    }

    @Override
    public void initExoPlayer(int index) {
        player = ExoPlayerFactory.newSimpleInstance(this.context, new DefaultTrackSelector());
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this.context,
                Util.getUserAgent(this.context, "exoPlayerLibrary"));
        player.setForegroundMode(true);
        // playlist/single audio load
        if (this.playerMode == PlayerMode.PLAYLIST) {
            ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
            for (AudioObject audioObject : audioObjects) {
                MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(Uri.parse(audioObject.getUrl()));
                concatenatingMediaSource.addMediaSource(mediaSource);
            }
            player.prepare(concatenatingMediaSource);
            if(index != 0) {
                player.seekTo(index,0);
            }
        } else {
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(this.audioObject.getUrl()));
            player.prepare(mediaSource);
        }
        // handle audio focus
        if (this.respectAudioFocus) { // ! TODO catch duck pause!
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC).build();
            player.setAudioAttributes(audioAttributes, true);
        }
        // set repeat mode
        if (repeatMode) {
            player.setRepeatMode(player.REPEAT_MODE_ALL);
        }
    }

    @Override
    public void play(AudioObject audioObject) {
        if(this.completed){
            this.resume();
        }else{
            this.released = false;

            this.audioObject = audioObject;
            this.initExoPlayer(0);
            initEventListeners();
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void playAll(ArrayList<AudioObject> audioObjects, int index) {
        if(this.completed || this.stopped){
            this.resume();
        }else{
            this.released = false;

            this.audioObjects = audioObjects;
            this.initExoPlayer(index);
            initEventListeners();
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void next() {
        if (!this.released) {
            player.next();
            resume();
        }
    }

    @Override
    public void previous() {
        if (!this.released) {
            player.previous();
            resume();
        }
    }

    @Override
    public void pause() {
        if (!this.released && this.playing) {
            stopForeground(false);
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void resume() { 
        if (!this.released && !this.playing) {
            if(!this.stopped){
                this.completed = false;
                player.setPlayWhenReady(true);
            }else{
                this.stopped = false;
                this.initExoPlayer(0);
                initEventListeners();
                player.setPlayWhenReady(true);
            }
        }
    }

    @Override
    public void stop() {
        if (!this.released) {
            mediaNotificationManager.setIsShowing(false);
            stopForeground(true);
            player.stop(true);
        }
    }

    @Override
    public void release() {
        if (!this.released) {
            if (this.playing) {
                stopForeground(true);
                mediaNotificationManager.setIsShowing(false);
            }
            this.initialized = false;
            this.buffering = false;
            this.playing = false;
            this.stopped = false;
            this.released = true;
            this.completed = false;

            this.audioObject = null;
            this.audioObjects = null;
            player.release();
            player = null;
            ref.handleStateChange(this, PlayerState.RELEASED);
            stopSelf();
        }
    }

    
    @Override
    public void seekPosition(int position) {
        if (!this.released) {
            player.seekTo(player.getCurrentWindowIndex(), position);
        }
    }

    @Override
    public void seekIndex(int index) {
        if (!this.released) {
            player.seekTo(index, 0);
        }
    }

    @Override
    public boolean isPlaying() {
        return this.playing;
    }

    @Override
    public boolean isBackground() {
        return false;
    }

    @Override
    public boolean isPlayerInitialized() {
        return this.initialized;
    }

    @Override
    public boolean isPlayerReleased() {
        return this.released;
    }

    @Override
    public boolean isPlayerCompleted() {
        return this.completed;
    }

    @Override
    public String getPlayerId() {
        return this.playerId;
    }

    @Override
    public long getDuration() {
        if (!this.released) {
            return player.getDuration();
        } else {
            return -1;
        }
    }

    @Override
    public long getCurrentPosition() {
        if (!this.released) {
            return player.getCurrentPosition();
        } else {
            return -1;
        }
    }

    @Override
    public int getCurrentPlayingAudioIndex() {
        return player.getCurrentWindowIndex();
    }

    @Override
    public float getVolume() {
        return player.getVolume();
    }

    @Override
    public void setPlayerAttributes(boolean repeatMode, boolean respectAudioFocus, PlayerMode playerMode) {
        this.repeatMode = repeatMode;
        this.respectAudioFocus = respectAudioFocus;
        this.playerMode = playerMode;
    }

    @Override
    public void setVolume(float volume) {
        if (!this.released && this.volume != volume) {
            this.volume = volume;
            player.setVolume(volume);
        }
    }

    @Override
    public void setRepeatMode(boolean repeatMode){
        if(!this.released && this.repeatMode != repeatMode){
            this.repeatMode = repeatMode;
            if(this.repeatMode){
                player.setRepeatMode(player.REPEAT_MODE_ALL);
            }else{
                player.setRepeatMode(player.REPEAT_MODE_OFF);
            }
        }
    }

    private void initEventListeners() {
        player.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
                ref.handleAudioSessionIdChange(foregroundAudioPlayer, audioSessionId);
            }
        });
        player.addListener(new Player.EventListener() {
            
            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                if(mediaNotificationManager.isShowing() || !mediaNotificationManager.isInitialized()) {
                    if (playerMode == PlayerMode.PLAYLIST) {
                        mediaNotificationManager.makeNotification(audioObjects.get(player.getCurrentWindowIndex()), true);
                    } else {
                        mediaNotificationManager.makeNotification(audioObject, true);
                    }
                }else{
                    mediaNotificationManager.setIsInitialized(false); //the player was stopped.
                }
                ref.handlePlayerIndex(foregroundAudioPlayer);
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING: {
                        // buffering
                        buffering = true;
                        ref.handleStateChange(foregroundAudioPlayer, PlayerState.BUFFERING);
                        break;
                    }
                    case Player.STATE_READY: {
                        if(completed) {
                            buffering = false;
                            if(mediaNotificationManager.isShowing()) {
                                mediaNotificationManager.makeNotification(false);
                            }
                            ref.handleStateChange(foregroundAudioPlayer, PlayerState.COMPLETED);
                        } else if (buffering) {
                            // playing
                            buffering = false;
                            if(playWhenReady){
                                playing = true;
                                if(mediaNotificationManager.isShowing()) {
                                    mediaNotificationManager.makeNotification(true);
                                }
                                ref.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING);
                                ref.handlePositionUpdates();
                            }else{
                                ref.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED);
                            }
                        } else if (playWhenReady) {
                            // resumed
                            playing = true;
                            if(mediaNotificationManager.isShowing()) {
                                mediaNotificationManager.makeNotification(true);
                            }
                            ref.handlePositionUpdates();
                            ref.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING);
                        } else if(!playWhenReady) {
                            // paused
                            playing = false;
                            if(mediaNotificationManager.isShowing()) {
                                mediaNotificationManager.makeNotification(false);
                            }
                            ref.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED);
                        }

                        break;
                    }
                    case Player.STATE_ENDED: {
                        // completed
                        playing = false;
                        completed = true;

                        stopForeground(false);
                        player.setPlayWhenReady(false);
                        player.seekTo(0, 0);
                        break;
                    }
                    case Player.STATE_IDLE: {
                        // stopped
                        playing = false;
                        stopped = true;
                        completed = false;
                        buffering = false;
                        ref.handleStateChange(foregroundAudioPlayer, PlayerState.STOPPED);
                        
                        break;
                    } // handle of released is in release method!
                }
            }
        });
    }

    //// private MediaSessionCompat.Callback mediaSessionCallback = new
    //// MediaSessionCompat.Callback() {
    //// @Override
    //// public void onPlay() {
    //// Log.d("hii","play!");
    //// super.onPlay();
    //// }

    //// @Override
    //// public void onPause() {
    //// Log.d("hii","pause!");
    //// super.onPause();
    //// }
    //// };
}