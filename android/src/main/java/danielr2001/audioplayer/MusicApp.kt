package danielr2001.audioplayer

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.request.CachePolicy

class MusicApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val imageLoader = ImageLoader.Builder(this)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(true)
                .addLastModifiedToFileCacheKey(true)
                .availableMemoryPercentage(0.5)
                .build()
        Coil.setImageLoader(imageLoader)
    }
}