package danielr2001.audioplayer.notifications;

import danielr2001.audioplayer.interfaces.AsyncResponse;

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

public class LoadImageFromUrl extends AsyncTask<String, Void, Map<String, Bitmap>> {

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
    protected Map<String, Bitmap> doInBackground(String... strings) {
        Map<String, Bitmap> bitmapMap = new HashMap<String, Bitmap>();
        if (isLocal) {
            if (new File(this.imageUrl).exists()) {
                Bitmap temp = BitmapFactory.decodeFile(this.imageUrl);
                if (temp != null) {
                    bitmapMap.put(this.imageUrl, temp);
                    return bitmapMap;
                }
            } else {
                Log.e("ExoPlayerPlugin", "Local image doesn`t exist!");
            }
        } else {
            Bitmap temp = getImageBitmapFromNetworkUrl();
            if (temp != null) {
                bitmapMap.put(this.imageUrl, temp);
                return bitmapMap;
            }
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
    protected void onPostExecute(Map<String, Bitmap> bitmapMap) {
        super.onPostExecute(bitmapMap);
        delegate.processFinish(bitmapMap);
    }
}
