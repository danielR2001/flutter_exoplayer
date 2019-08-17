package danielr2001.audioplayer.audioplayers;

import danielr2001.audioplayer.interfaces.AudioPlayer;
import danielr2001.audioplayer.notifications.MediaNotificationManager;
import danielr2001.audioplayer.AudioPlayerPlugin;
import danielr2001.audioplayer.models.AudioObject;
import danielr2001.audioplayer.enums.PlayerState;
import danielr2001.audioplayer.enums.PlayerMode;


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
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

public class BackgroundAudioPlayer implements AudioPlayer {

    private Context context;
    private AudioPlayerPlugin ref;
    private BackgroundAudioPlayer backgroundAudioPlayer;
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

    
    @Override
    public void initAudioPlayer (AudioPlayerPlugin ref, Activity activity, String playerId) {
        this.initialized = true;

        this.ref = ref;
        this.context = activity.getApplicationContext();
        this.playerId = playerId;
        this.backgroundAudioPlayer = this;
    }

    @Override
    public void initExoPlayer(int index) {
        player = ExoPlayerFactory.newSimpleInstance(this.context, new DefaultTrackSelector());
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this.context, Util.getUserAgent(this.context, "exoPlayerLibrary"));
        // playlist/single audio load
        if(playerMode == PlayerMode.PLAYLIST){
            ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
            for (AudioObject audioObject : audioObjects) {
                MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(audioObject.getUrl()));
                concatenatingMediaSource.addMediaSource(mediaSource);
            }
            player.prepare(concatenatingMediaSource);
            if(index != 0) {
                player.seekTo(index,0);
            }
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

    @Override
    public void play(AudioObject audioObject) {
        if(this.completed || this.stopped){
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
        player.next();
    }

    @Override
    public void previous() {
        player.previous();
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
            player.stop(true);
        }
    }

    @Override
    public void release() {
        if (!this.released) {
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
        }
    }

    @Override
    public void seek(int position) {
        if (!this.released) {
            player.seekTo(player.getCurrentWindowIndex(), position);
        }
    }

    @Override
    public void seekTo(int index) {
        if (!this.released && playerMode == PlayerMode.PLAYLIST) {
            player.seekTo(index, 0);
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
    public int getCurrentPlayingAudioIndex(){
        return player.getCurrentWindowIndex();
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
        player.addAnalyticsListener(new AnalyticsListener(){
            @Override
            public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
                ref.handleAudioSessionIdChange(backgroundAudioPlayer, audioSessionId);
            }
        });
        player.addListener(new Player.EventListener() {

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                ref.handlePlayerIndex(backgroundAudioPlayer);
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING: {
                        // buffering
                        buffering = true;
                        ref.handleStateChange(backgroundAudioPlayer, PlayerState.BUFFERING);
                        break;
                    }
                    case Player.STATE_READY: {
                        if(completed) {
                            buffering = false;
                            ref.handleStateChange(backgroundAudioPlayer, PlayerState.COMPLETED);
                        } else if (buffering) {
                            // playing
                            buffering = false;
                            playing = true;
                            ref.handlePositionUpdates();
                            ref.handleStateChange(backgroundAudioPlayer, PlayerState.PLAYING);
                        } else if (playWhenReady) {
                            // resumed
                            playing = true;
                            ref.handlePositionUpdates();
                            ref.handleStateChange(backgroundAudioPlayer, PlayerState.PLAYING);
                        } else if (!playWhenReady){
                            // paused
                            playing = false;
                            ref.handleStateChange(backgroundAudioPlayer, PlayerState.PAUSED);
                        }
                        break;
                    }
                    case Player.STATE_ENDED: {
                        // completed
                        playing = false;
                        completed = true;
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
                        ref.handleStateChange(backgroundAudioPlayer, PlayerState.STOPPED);
                        break;
                    } 
                    // handle of released is in release method!
                }
            }
        });
    }
}