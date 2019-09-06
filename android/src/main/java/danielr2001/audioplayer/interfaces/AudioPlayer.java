package danielr2001.audioplayer.interfaces;

import danielr2001.audioplayer.AudioPlayerPlugin;
import danielr2001.audioplayer.models.AudioObject;
import danielr2001.audioplayer.enums.PlayerMode;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;

public interface AudioPlayer {

    //initializers
    void initAudioPlayer(AudioPlayerPlugin ref, Activity activity, String playerId);

    void initExoPlayer(int index);

    //player contols
    void play(AudioObject audioObject); 

    void playAll(ArrayList<AudioObject> audioObjects, int index); 

    void next();

    void previous();

    void pause();

    void resume();

    void stop();

    void release();

    void seekPosition(int position);

    void seekIndex(int index);

    //state check
    boolean isPlaying();

    boolean isBackground();

    boolean isPlayerInitialized();

    boolean isPlayerReleased();

    boolean isPlayerCompleted();

    //getters
    String getPlayerId();

    long getDuration();

    long getCurrentPosition();

    int getCurrentPlayingAudioIndex();

    float getVolume();

    //setters
    void setPlayerAttributes(boolean repeatMode, boolean respectAudioFocus, PlayerMode playerMode);

    void setVolume(float volume);

    void setRepeatMode(boolean repeatMode);
}
