package danielr2001.audioplayer.interfaces;

import android.graphics.Bitmap;

import java.util.Map;

public interface AsyncResponse { 
    void processFinish(Map<String,Bitmap> bitmapMap); 
}