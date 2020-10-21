package danielr2001.audioplayer.notifications

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import danielr2001.audioplayer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val NOW_PLAYING_CHANNEL_ID = "com.example.android.uamp.media.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION_ID = 0xb339 // Arbitrary number used to identify our notification

class AudioNotificationManager(
        private val context: Context,
        @DrawableRes private val smallIcon: Int,
        sessionToken: MediaSessionCompat.Token,
        notificationListener: PlayerNotificationManager.NotificationListener
) {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val notificationManager: PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)

        notificationManager = PlayerNotificationManager.createWithNotificationChannel(
                context,
                NOW_PLAYING_CHANNEL_ID,
                R.string.notification_channel,
                R.string.notification_channel_description,
                NOW_PLAYING_NOTIFICATION_ID,
                DescriptionAdapter(mediaController),
                notificationListener
        ).apply {
            setMediaSessionToken(sessionToken)
            setSmallIcon(smallIcon)
            setRewindIncrementMs(0)
            setFastForwardIncrementMs(0)
        }
    }

    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    fun showNotificationForPlayer(player: Player) {
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) :
            PlayerNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
                controller.sessionActivity

        override fun getCurrentContentText(player: Player) =
                controller.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player) =
                controller.metadata.description.title.toString()

        override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val iconUri = controller.metadata.description.iconUri
            return if (currentIconUri != iconUri || currentBitmap == null) {
                currentIconUri = iconUri
                serviceScope.launch {
                    currentBitmap = iconUri?.let {
                        resolveUriAsBitmap(it)
                    }
                    currentBitmap?.let { callback.onBitmap(it) }
                }
                null
            } else {
                currentBitmap
            }
        }

        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(NOTIFICATION_LARGE_ICON_SIZE)
                    .build()

            val imageLoader = ImageLoader.Builder(context)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .addLastModifiedToFileCacheKey(true)
                    .availableMemoryPercentage(0.5)
                    .build()

            return imageLoader.execute(request).drawable?.toBitmap(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
        }
    }
}

const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px