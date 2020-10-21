package danielr2001.audioplayer.utils

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import danielr2001.audioplayer.models.AudioObject
import java.util.*

object MediaUtils {
    fun createDataSourceFactory(context: Context): DefaultDataSourceFactory {
        val httpDataSourceFactory = DefaultHttpDataSourceFactory(
                Util.getUserAgent(context, "exoPlayerLibrary"),
                null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                true /* allowCrossProtocolRedirects */
        )
        return DefaultDataSourceFactory(
                context,
                null,
                httpDataSourceFactory)
    }
}

private fun AudioObject.toMediaMetadata(): MediaMetadataCompat {
    return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, this.url.asKey())
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, this.largeIcon)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, this.largeIconUrl)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, this.title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, this.subTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, this.url)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, this.duration)
            .build()
}

private fun Collection<AudioObject>?.toMediaMetadata(): List<MediaMetadataCompat> {
    if (this.isNullOrEmpty()) return emptyList()
    return this.map { it.toMediaMetadata() }
}

private fun String.asKey(): String = this.toLowerCase(Locale.US)