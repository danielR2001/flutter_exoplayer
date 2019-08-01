package danielr2001.exoplayer.notifications;

import danielr2001.exoplayer.interfaces.AsyncResponse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LoadImageFromUrl extends AsyncTask<String, Void, Map<String,Bitmap>> {

    private String imageUrl;
    private boolean isLocal;
    private AsyncResponse delegate = null;

    public LoadImageFromUrl(String imageUrl, boolean isLocal, AsyncResponse asyncResponse) {
        super();
        this.imageUrl = imageUrl;
        this.isLocal = isLocal;
        this.delegate = asyncResponse;
    }

    @Override
    protected Map<String, Bitmap> doInBackground(String... strings) {//! TODO check unsafe
        Map<String, Bitmap> bitmap = new HashMap(); 
        if (isLocal) {
            if (new File(this.imageUrl).exists()) {
                bitmap.put(this.imageUrl, BitmapFactory.decodeFile(this.imageUrl));
                return bitmap;
            } else {
                Log.e("ExoPlayerPlugin", "Local image doesnt exist!");
            }
        } else {
            bitmap.put(this.imageUrl, getImageBitmapFromNetworkUrl());
            return bitmap;
        }

        return null;
    }

    private Bitmap getImageBitmapFromNetworkUrl() {
        try {
            URL url = new URL(this.imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream in = connection.getInputStream();
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("ExoPlayerPlugin", "Failed loading image!");
            return null;
        }
    }

    @Override
    protected void onPostExecute(Map<String, Bitmap> bitmap) {
        super.onPostExecute(bitmap);
        delegate.processFinish(bitmap);
    }
}
