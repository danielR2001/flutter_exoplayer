package dr.library.exoplayer;

interface AudioPlayer {

    String getPlayerId();

    void play(boolean repeatMode, boolean respectAudioFocus, AudioObject audioObject);

    void play(boolean repeatMode, boolean respectAudioFocus, String url);

    void playAll(boolean repeatMode, boolean respectAudioFocus, AudioObject[] audioObjects);

    void playAll(boolean repeatMode, boolean respectAudioFocus, String[] urls);

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
