package danielr2001.audioplayer.interfaces;

import danielr2001.audioplayer.AudioPlayerPlugin;
import danielr2001.audioplayer.models.AudioObject;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;

public interface AudioPlayer {

    void initAudioPlayer(AudioPlayerPlugin ref, Activity activity, String playerId);

    String getPlayerId();

    void play(boolean repeatMode, boolean respectAudioFocus, AudioObject audioObject); 

    void playAll(boolean repeatMode, boolean respectAudioFocus, ArrayList<AudioObject> audioObjects);

    void next();

    void previous();

    void pause();

    void resume();

    void stop();

    void release();

    void setVolume(float volume);

    long getDuration();

    long getCurrentPosition();

    void seek(int position);

    boolean isPlaying();

    boolean isBackground();

    boolean isPlayerInitialized();

    boolean isPlayerReleased();

    int getCurrentPlayingAudioIndex();

    void setRepeatMode(boolean repeatMode);
}
