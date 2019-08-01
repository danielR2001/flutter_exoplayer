package danielr2001.exoplayer.interfaces;

import danielr2001.exoplayer.ExoPlayerPlugin;
import danielr2001.exoplayer.models.AudioObject;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;

public interface AudioPlayer {

    void initAudioPlayer(ExoPlayerPlugin ref, Activity activity, String playerId);

    String getPlayerId();

    void play(boolean repeatMode, boolean respectAudioFocus, AudioObject audioObject); //! TODO make optional variables!

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
}
