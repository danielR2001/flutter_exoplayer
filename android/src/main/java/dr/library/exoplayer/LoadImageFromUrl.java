package dr.library.exoplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.File;

public class LoadImageFromUrl extends AsyncTask<String, Void, Bitmap> {

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
    protected Bitmap doInBackground(String... strings) {
        if (isLocal) {
            if (new File(this.imageUrl).exists()) {
                return BitmapFactory.decodeFile(this.imageUrl);
            } else {
                Log.e("LoadImageFromUrl", "Local image doesnt exist!");
            }
        } else {
            return getImageBitmapFromNetworkUrl();
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
            Log.e("LoadImageFromUrl", "Failed loading image!");
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        delegate.processFinish(bitmap);
    }
}
