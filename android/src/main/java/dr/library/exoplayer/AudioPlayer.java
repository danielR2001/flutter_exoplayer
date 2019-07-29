package dr.library.exoplayer;

import android.content.Context;

import java.util.ArrayList;

interface AudioPlayer {

    void initAudioPlayer(ExoPlayerPlugin ref, Context context, String playerId);

    String getPlayerId();

    void play(boolean repeatMode, boolean respectAudioFocus, AudioObject audioObject);

    void play(boolean repeatMode, boolean respectAudioFocus, String url);

    void playAll(boolean repeatMode, boolean respectAudioFocus, AudioObject[] audioObjects);

    void playAll(boolean repeatMode, boolean respectAudioFocus, ArrayList<String> urls);

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
}
