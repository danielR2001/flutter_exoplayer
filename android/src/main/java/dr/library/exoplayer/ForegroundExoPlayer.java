package dr.library.exoplayer;

import android.app.Service;
import android.content.Intent;
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

public class ForegroundExoPlayer extends Service implements AudioPlayer {
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        ForegroundExoPlayer getService() {
            return ForegroundExoPlayer.this;
        }
    }

    private float volume = 1;
    private boolean repeatMode = false;

    private boolean released = true;
    private boolean playing = false;
    private boolean buffering = false;

    private SimpleExoPlayer player;
    private ExoPlayerPlugin ref;

    private MediaNotificationManager mediaNotificationManager;

    private AudioObject[] audioObjects;
    private AudioObject audioObject;

    public void passPluginRef(ExoPlayerPlugin ref) {
        this.ref = ref;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction().equals(MediaNotificationManager.SERVICE_STARTED)){
            this.mediaNotificationManager= new MediaNotificationManager(this);
        }else if (intent.getAction().equals(MediaNotificationManager.PREVIOUS_ACTION)) {
            previous();
        } else if (intent.getAction().equals(MediaNotificationManager.PLAY_ACTION)) {
            resume();
        } else if (intent.getAction().equals(MediaNotificationManager.PAUSE_ACTION)) {
            pause();
        }else if (intent.getAction().equals(MediaNotificationManager.NEXT_ACTION)) {
            next();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.release();
        stopForeground(true);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        this.release();
        stopForeground(true);
    }

    @Override
    public void play(boolean repeatMode, AudioObject audioObject) {
        this.released = false;
        this.audioObject = audioObject;
        this.audioObjects = null;
        this.repeatMode = repeatMode;
        initExoPlayer();
        initStateChangeListener();
        player.setPlayWhenReady(true);
    }

    @Override
    public void playAll(boolean repeatMode, AudioObject[] audioObjects) {
        this.released = false;
        this.audioObjects = audioObjects;
        this.audioObject = null;
        initExoPlayer();
        initStateChangeListener();
        player.setPlayWhenReady(true);
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
            this.playing = false;
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void resume() {
        if (!this.released && !this.playing) {
            this.playing = true;
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void stop() {
        if (!this.released) {
            this.playing = false;
            player.stop(true);
        }
    }

    @Override
    public void release() {
        if (!this.released) {
            this.released = true;
            this.playing = false;
            this.audioObject = null;
            this.audioObjects = null;
            player.release();
            player = null;
            ref.handleStateChange(PlayerState.RELEASED);
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
            return player.getContentPosition();
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
        player = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector());
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "exoPlayerLibrary"));
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
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build();
        player.setAudioAttributes(audioAttributes, true);
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
                                if(audioObjects != null) {
                                    mediaNotificationManager.makeNotification(audioObjects[player.getCurrentWindowIndex()], true);
                                }else {
                                    mediaNotificationManager.makeNotification(audioObject, true);
                                }
                                ref.handleStateChange(PlayerState.PLAYING);
                            }else{
                                //paused
                                if(audioObjects != null) {
                                    mediaNotificationManager.makeNotification(audioObjects[player.getCurrentWindowIndex()], false);
                                }else {
                                    mediaNotificationManager.makeNotification(audioObject, false);
                                }
                                ref.handleStateChange(PlayerState.PAUSED);
                            }
                        }else{
                            //play
                            if(audioObjects != null) {
                                mediaNotificationManager.makeNotification(audioObjects[player.getCurrentWindowIndex()], true);
                            }else {
                                mediaNotificationManager.makeNotification(audioObject, true);
                            }
                            ref.handleStateChange(PlayerState.PLAYING);
                            buffering = false;
                        }
                        break;
                    }
                    case Player.STATE_ENDED:{
                        //completed
                        ref.handleStateChange(PlayerState.COMPLETED);
                        break;
                    }
                    case Player.STATE_IDLE:{
                        //stopped
                        ref.handleStateChange(PlayerState.STOPPED);
                        break;
                    }
                    //handle of released is in release method!
                }
            }
        });
    }
}