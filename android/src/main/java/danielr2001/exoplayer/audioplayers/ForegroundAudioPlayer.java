package danielr2001.exoplayer.audioplayers;

import danielr2001.exoplayer.interfaces.AudioPlayer;
import danielr2001.exoplayer.notifications.MediaNotificationManager;
import danielr2001.exoplayer.ExoPlayerPlugin;
import danielr2001.exoplayer.models.AudioObject;
import danielr2001.exoplayer.enums.PlayerState;

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
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
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
    private ExoPlayerPlugin ref;
    private MediaSessionCompat mediaSession;

    private float volume = 1;
    private boolean repeatMode = false;
    private boolean respectAudioFocus = false;

    private boolean initialized = false;
    private boolean released = true;
    private boolean playing = false;
    private boolean buffering = false;

    private String playerId;
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
        //! TODO handle MediaButtonReceiver's callbacks
        //MediaButtonReceiver.handleIntent(mediaSession, intent);
        //mediaSession.setCallback(mediaSessionCallback);
        if(intent.getAction() != null){
            if (intent.getAction().equals(MediaNotificationManager.PREVIOUS_ACTION)) {
                previous();
            } else if (intent.getAction().equals(MediaNotificationManager.PLAY_ACTION)) {
                resume();
            } else if (intent.getAction().equals(MediaNotificationManager.PAUSE_ACTION)) {
                pause();
            }else if (intent.getAction().equals(MediaNotificationManager.NEXT_ACTION)) {
                next();
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
    public void initAudioPlayer(ExoPlayerPlugin ref, Activity activity, String playerId) {
        this.ref = ref;
        this.playerId = playerId;
        this.mediaNotificationManager = new MediaNotificationManager(this, this.context, this.mediaSession, activity);
        this.foregroundAudioPlayer = this;
        this.initialized = true;
    }

    @Override
    public String getPlayerId() {
        return this.playerId;
    }

    @Override
    public void play(boolean repeatMode, boolean respectAudioFocus, AudioObject audioObject) {
        this.released = false;
        this.repeatMode = repeatMode;
        this.respectAudioFocus = respectAudioFocus;
        this.audioObject = audioObject;
        this.audioObjects = null;
        initExoPlayer();
        initListeners();
        loadNewAudioNotification();
        player.setPlayWhenReady(true);
    }

    @Override
    public void playAll(boolean repeatMode, boolean respectAudioFocus, ArrayList<AudioObject> audioObjects) {
        this.released = false;
        this.repeatMode = repeatMode;
        this.respectAudioFocus = respectAudioFocus;
        this.audioObjects = audioObjects;
        this.audioObject = null;
        initExoPlayer();
        initListeners();
        loadNewAudioNotification();
        player.setPlayWhenReady(true);
    }

    @Override
    public void next() {
        if (!this.released) {
            player.next();
            loadNewAudioNotification();
            this.resume();
        }
    }

    @Override
    public void previous() { //!TODO first time go to previous (maybe make counter for 3 sec)
        if (!this.released) {
            player.previous();
            loadNewAudioNotification();
            this.resume();
        }
    }

    @Override
    public void pause() {
        if (!this.released && this.playing) {
            player.setPlayWhenReady(false);
            stopForeground(false);
        }
    }

    @Override
    public void resume() {
        if (!this.released && !this.playing) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void stop() {
        if (!this.released) {
            player.stop(true);
            stopForeground(true);
        }
    }

    @Override
    public void release() {
        if (!this.released) {
            if(this.playing){
                stopForeground(true);
            }
            this.released = true;
            this.playing = false;
            this.audioObject = null;
            this.audioObjects = null;
            player.release();
            player = null;
            ref.handleStateChange(this, PlayerState.RELEASED);
            stopSelf();
        }
    }

    @Override
    public void setVolume(float volume) {
        if (!this.released && this.volume != volume) {
            this.volume = volume;
            player.setVolume(volume);
        }
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
    public void seek(int position) {
        if (!this.released) {
            player.seekTo(player.getCurrentWindowIndex(), position);
        }
    }

    @Override
    public boolean isPlaying() {
        return this.playing;
    }

    @Override
    public boolean isBackground(){
        return false;
    }
    
    @Override
    public boolean isPlayerInitialized(){
        return this.initialized;
    }
    
    @Override
    public boolean isPlayerReleased(){
        return this.released;
    }

    @Override
    public int getCurrentPlayingAudioIndex(){
        return player.getCurrentWindowIndex();
    }

    private void initExoPlayer() {
        player = ExoPlayerFactory.newSimpleInstance(this.context, new DefaultTrackSelector());
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this.context, Util.getUserAgent(this.context, "exoPlayerLibrary"));
        player.setForegroundMode(true);
        // playlist/single audio load
        if(this.audioObjects != null){
            ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
            for (AudioObject audioObject : audioObjects) {
                MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(audioObject.getUrl()));
                concatenatingMediaSource.addMediaSource(mediaSource);
            }
            player.prepare(concatenatingMediaSource);
        }else{
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(this.audioObject.getUrl()));
            player.prepare(mediaSource);
        }
        //handle audio focus
        if(this.respectAudioFocus){ //! TODO catch duck pause!
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .build();
            player.setAudioAttributes(audioAttributes, true);
        }
        //set repeat mode
        if (repeatMode) {
            player.setRepeatMode(player.REPEAT_MODE_ALL);
        }
    }

    private void initListeners() {
        player.addAnalyticsListener(new AnalyticsListener(){
            @Override
            public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
                ref.handleAudioSessionIdChange(audioSessionId);
            }
        });
        player.addListener(new Player.EventListener() {

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) { 
                switch (playbackState){
                    case Player.STATE_BUFFERING:{
                        //buffering
                        buffering = true;
                        ref.handleStateChange(foregroundAudioPlayer, PlayerState.BUFFERING);
                    }
                    case Player.STATE_READY:{
                        if(buffering){
                            //play
                            playing = true;
                            buffering = false;
                            ref.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING);
                            ref.handlePlayerIndex();
                        }else{
                            if(playWhenReady && playing){
                                //first play
                                if(audioObjects != null) {
                                    mediaNotificationManager.makeNotification(true);
                                }else {
                                    mediaNotificationManager.makeNotification(true);
                                }
                                ref.handlePositionUpdates();
                            }else if (playWhenReady && !playing) {
                                //resumed   
                                playing = true;                         
                                if(audioObjects != null) {
                                    mediaNotificationManager.makeNotification(true);
                                }else {
                                    mediaNotificationManager.makeNotification(true);
                                }
                                ref.handlePositionUpdates();
                                ref.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING);
                                
                            }else if(!playWhenReady && playing){
                                //paused
                                playing = false;
                                if(audioObjects != null) {
                                    mediaNotificationManager.makeNotification(false);
                                }else {
                                    mediaNotificationManager.makeNotification(false);
                                }
                                ref.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED);
                            }
                        }
                        break;
                    }
                    case Player.STATE_ENDED:{
                        //completed
                        playing = false;
                        ref.handleStateChange(foregroundAudioPlayer, PlayerState.COMPLETED);
                        break;
                    }
                    case Player.STATE_IDLE:{
                        //stopped
                        playing = false;
                        ref.handleStateChange(foregroundAudioPlayer, PlayerState.STOPPED);
                        break;
                    }  //handle of released is in release method!
                }
            }
        });
    }

    private void loadNewAudioNotification(){
        if(audioObjects != null) {
            mediaNotificationManager.makeNotification(audioObjects.get(player.getCurrentWindowIndex()), false);
        }else {
            mediaNotificationManager.makeNotification(audioObject, false); 
        }
    }
    
    //// private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {
    ////     @Override
    ////     public void onPlay() {
    ////         Log.d("hii","play!");
    ////         super.onPlay();
    ////     }

    ////     @Override
    ////     public void onPause() {
    ////         Log.d("hii","pause!");
    ////         super.onPause();
    ////     }
    //// };
}