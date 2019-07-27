package dr.library.exoplayer;

interface AudioPlayer {

    void play(boolean repeatMode, AudioObject audioObject);

    void play(boolean repeatMode);

    void playAll(boolean repeatMode, AudioObject[] audioObjects);

    void playAll(boolean repeatMode);

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
