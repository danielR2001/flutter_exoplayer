package danielr2001.audioplayer.audioplayers;

import danielr2001.audioplayer.interfaces.AudioPlayer;
import danielr2001.audioplayer.notifications.MediaNotificationManager;
import danielr2001.audioplayer.AudioPlayerPlugin;
import danielr2001.audioplayer.models.AudioObject;
import danielr2001.audioplayer.enums.PlayerState;


import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

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

public class BackgroundAudioPlayer implements AudioPlayer {

    private Context context;
    private AudioPlayerPlugin ref;
    private BackgroundAudioPlayer backgroundAudioPlayer;

    private float volume = 1;
    private boolean repeatMode = false;
    private boolean respectAudioFocus = false;

    private boolean initialized = false;
    private boolean released = true;
    private boolean playing = false;
    private boolean buffering = false;

    private String playerId;
    private SimpleExoPlayer player;
    private ArrayList<AudioObject> audioObjects = new ArrayList<>();
    private AudioObject audioObject;

    @Override
    public void initAudioPlayer (AudioPlayerPlugin ref, Activity activity, String playerId) {
        this.ref = ref;
        this.context = activity.getApplicationContext();
        this.playerId = playerId;
        this.backgroundAudioPlayer = this;
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
        player.setPlayWhenReady(true);
    }

    @Override
    public void next() {
        player.next();
        this.resume();
    }

    @Override
    public void previous() {
        player.previous();
        this.resume();
    }

    @Override
    public void pause() {
        if (!this.released && this.playing) {
            player.setPlayWhenReady(false);
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
        }
    }

    @Override
    public void release() {
        if (!this.released) {
            this.playing = false;
            this.released = true;
            this.audioObject = null;
            this.audioObjects = null;
            player.release();
            player = null;
            ref.handleStateChange(this, PlayerState.RELEASED);
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
        return true;
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
        // playlist/single audio load
        if(audioObjects != null){
            ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
            for (AudioObject audioObject : audioObjects) {
                MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(audioObject.getUrl()));
                concatenatingMediaSource.addMediaSource(mediaSource);
            }
            player.prepare(concatenatingMediaSource);
        }else{
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(audioObject.getUrl()));
            player.prepare(mediaSource);
        }
        //handle audio focus
        if(this.respectAudioFocus){
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
                        ref.handleStateChange(backgroundAudioPlayer, PlayerState.BUFFERING);
                    }
                    case Player.STATE_READY:{
                        if(buffering){
                            //play
                            playing = true;
                            buffering = false;
                            ref.handleStateChange(backgroundAudioPlayer, PlayerState.PLAYING);
                            ref.handlePlayerIndex();
                        }else{
                            if(playWhenReady && playing){
                                //first play
                                ref.handlePositionUpdates();
                            }else if (playWhenReady && !playing) {
                                //resumed   
                                playing = true;                         
                                ref.handlePositionUpdates();
                                ref.handleStateChange(backgroundAudioPlayer, PlayerState.PLAYING);
                                
                            }else if(!playWhenReady && playing){
                                //paused
                                playing = false;
                                ref.handleStateChange(backgroundAudioPlayer, PlayerState.PAUSED);
                            }
                        }
                        break;
                    }
                    case Player.STATE_ENDED:{
                        //completed
                        playing = false;
                        ref.handleStateChange(backgroundAudioPlayer, PlayerState.COMPLETED);
                        break;
                    }
                    case Player.STATE_IDLE:{
                        //stopped
                        playing = false;
                        ref.handleStateChange(backgroundAudioPlayer, PlayerState.STOPPED);
                        break;
                    }
                }
            }
        });
    }
}