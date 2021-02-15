package danielr2001.audioplayer.audioplayers;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;

public class AudioFocusManager {

    private final Context context;
    private AudioFocusRequest audioFocusRequest;

    public AudioFocusManager(Context context) {
        this.context = context;
    }

    public void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest =
                    new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                            .build();
            getAudioManager().requestAudioFocus(audioFocusRequest);
        } else {
            getAudioManager().requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    public void releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                getAudioManager().abandonAudioFocusRequest(audioFocusRequest);
                audioFocusRequest = null;
            }
        } else {
            getAudioManager().abandonAudioFocus(null);
        }
    }

    private AudioManager getAudioManager() {
        return ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
    }
}