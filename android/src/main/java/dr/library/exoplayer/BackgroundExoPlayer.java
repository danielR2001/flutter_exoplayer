package dr.library.exoplayer;

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
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import android.util.Log;
public class BackgroundExoPlayer implements AudioPlayer {

    private Context context;

    private float volume = 1;
    private boolean repeatMode = false;
    private boolean respectAudioFocus = false;

    private boolean released = true;
    private boolean playing = false;
    private boolean buffering = false;

    private String playerId;

    private SimpleExoPlayer player;
    private ExoPlayerPlugin ref;

    private BackgroundExoPlayer backgroundExoPlayer;

    private String[] urls;
    private String url;

    public BackgroundExoPlayer (ExoPlayerPlugin ref, Context context, String playerId) {
        this.ref = ref;
        this.context = context;
        this.playerId = playerId;
        this.backgroundExoPlayer = this;
    }

    @Override
    public String getPlayerId() {
        return this.playerId;
    }

    @Override
    public void play(boolean repeatMode, boolean respectAudioFocus, String url) {
        this.released = false;
        this.repeatMode = repeatMode;
        this.respectAudioFocus = respectAudioFocus;
        this.url = url;
        this.urls = null;
        initExoPlayer();
        initStateChangeListener();
        player.setPlayWhenReady(true);
    }

    @Override
    public void play(boolean repeatMode, boolean respectAudioFocus, AudioObject audioObject) {}

    @Override
    public void playAll(boolean repeatMode, boolean respectAudioFocus, String[] urls) {
        this.released = false;
        this.repeatMode = repeatMode;
        this.respectAudioFocus = respectAudioFocus;
        this.urls = urls;
        this.url = null;
        initExoPlayer();
        initStateChangeListener();
        player.setPlayWhenReady(true);
    }

    @Override
    public void playAll(boolean repeatMode, boolean respectAudioFocus, AudioObject[] audioObjects) {}

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
            this.released = true;
            this.url = null;
            this.urls = null;
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

    private void initExoPlayer() {
        player = ExoPlayerFactory.newSimpleInstance(this.context, new DefaultTrackSelector());
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this.context, Util.getUserAgent(this.context, "exoPlayerLibrary"));
        // playlist/single audio load
        if(urls != null){
            ConcatenatingMediaSource concatenatingMediaSource = new ConcatenatingMediaSource();
            for (String url : urls) {
                MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url));
                concatenatingMediaSource.addMediaSource(mediaSource);
            }
            player.prepare(concatenatingMediaSource);
        }else{
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(this.url));
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

    private void initStateChangeListener() {
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState){
                    case Player.STATE_BUFFERING:{
                        //play first time
                        buffering = true;
                    }
                    case Player.STATE_READY:{
                        if(!buffering){
                            if (playWhenReady) {
                                //resumed
                                playing = true;
                                ref.handleStateChange(backgroundExoPlayer, PlayerState.PLAYING);
                                ref.handlePositionUpdates();
                            }else{
                                //paused
                                playing = false;
                                ref.handleStateChange(backgroundExoPlayer, PlayerState.PAUSED);
                            }
                        }else{
                            //play
                            playing = true;
                            buffering = false;
                            ref.handleStateChange(backgroundExoPlayer, PlayerState.PLAYING);
                            ref.handlePositionUpdates();
                        }
                        break;
                    }
                    case Player.STATE_ENDED:{
                        //completed
                        playing = false;
                        ref.handleStateChange(backgroundExoPlayer, PlayerState.COMPLETED);
                        break;
                    }
                    case Player.STATE_IDLE:{
                        //stopped
                        playing = false;
                        ref.handleStateChange(backgroundExoPlayer, PlayerState.STOPPED);
                        break;
                    }
                    //handle of released is in release method!
                }
            }
        });
    }
}