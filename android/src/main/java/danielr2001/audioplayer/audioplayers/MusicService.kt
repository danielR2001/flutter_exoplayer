//package danielr2001.audioplayer.audioplayers
//
//import android.app.Notification
//import android.app.PendingIntent
//import android.app.Service
//import android.content.Intent
//import android.support.v4.media.MediaDescriptionCompat
//import android.support.v4.media.MediaMetadataCompat
//import android.support.v4.media.session.MediaSessionCompat
//import android.support.v4.media.session.PlaybackStateCompat
//import androidx.core.content.ContextCompat
//import androidx.media.MediaBrowserServiceCompat
//import com.google.android.exoplayer2.*
//import com.google.android.exoplayer2.audio.AudioAttributes
//import com.google.android.exoplayer2.ui.PlayerNotificationManager
//import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
//import com.google.android.exoplayer2.util.Log
//import com.google.android.exoplayer2.util.Util
//import danielr2001.audioplayer.notifications.UampNotificationManager
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//
//open class MusicService : Service() {
//
//    private lateinit var notificationManager: UampNotificationManager
//    private lateinit var mediaSource: MusicSource
//
//    // The current player will either be an ExoPlayer (for local playback) or a CastPlayer (for
//    // remote playback through a Cast device).
//    private lateinit var currentPlayer: Player
//
//    protected lateinit var mediaSession: MediaSessionCompat
//    protected lateinit var mediaSessionConnector: MediaSessionConnector
//    private var currentPlaylistItems: List<MediaMetadataCompat> = emptyList()
//
//
//    private val dataSourceFactory: DefaultDataSourceFactory by lazy {
//        DefaultDataSourceFactory(
//                /* context= */ this,
//                Util.getUserAgent(/* context= */ this, UAMP_USER_AGENT), /* listener= */
//                null
//        )
//    }
//
//    private var isForegroundService = false
//
//    private val uAmpAudioAttributes = AudioAttributes.Builder()
//            .setContentType(C.CONTENT_TYPE_MUSIC)
//            .setUsage(C.USAGE_MEDIA)
//            .build()
//
//    private val playerListener = PlayerEventListener()
//
//    /**
//     * Configure ExoPlayer to handle audio focus for us.
//     * See [Player.AudioComponent.setAudioAttributes] for details.
//     */
//    private val exoPlayer: ExoPlayer by lazy {
//        SimpleExoPlayer.Builder(this).build().apply {
//            setAudioAttributes(uAmpAudioAttributes, true)
//            setHandleAudioBecomingNoisy(true)
//            addListener(playerListener)
//        }
//    }
//
//    @ExperimentalCoroutinesApi
//    override fun onCreate() {
//        super.onCreate()
//
//        // Build a PendingIntent that can be used to launch the UI.
//        val sessionActivityPendingIntent =
//                packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
//                    PendingIntent.getActivity(this, 0, sessionIntent, 0)
//                }
//
//        // Create a new MediaSession.
//        mediaSession = MediaSessionCompat(this, "MusicService")
//                .apply {
//                    setSessionActivity(sessionActivityPendingIntent)
//                    isActive = true
//                }
//
//        /**
//         * In order for [MediaBrowserCompat.ConnectionCallback.onConnected] to be called,
//         * a [MediaSessionCompat.Token] needs to be set on the [MediaBrowserServiceCompat].
//         *
//         * It is possible to wait to set the session token, if required for a specific use-case.
//         * However, the token *must* be set by the time [MediaBrowserServiceCompat.onGetRoot]
//         * returns, or the connection will fail silently. (The system will not even call
//         * [MediaBrowserCompat.ConnectionCallback.onConnectionFailed].)
//         */
//        sessionToken = mediaSession.sessionToken
//
//        /**
//         * The notification manager will use our player and media session to decide when to post
//         * notifications. When notifications are posted or removed our listener will be called, this
//         * allows us to promote the service to foreground (required so that we're not killed if
//         * the main UI is not visible).
//         */
//        notificationManager = UampNotificationManager(
//                this,
//                mediaSession.sessionToken,
//                PlayerNotificationListener()
//        )
//
//        // ExoPlayer will manage the MediaSession for us.
//        mediaSessionConnector = MediaSessionConnector(mediaSession)
//        mediaSessionConnector.setPlaybackPreparer(UampPlaybackPreparer())
//        mediaSessionConnector.setQueueNavigator(UampQueueNavigator(mediaSession))
//
//        notificationManager.showNotificationForPlayer(currentPlayer)
//    }
//
//    /**
//     * This is the code that causes UAMP to stop playing when swiping the activity away from
//     * recents. The choice to do this is app specific. Some apps stop playback, while others allow
//     * playback to continue and allow users to stop it with the notification.
//     */
//    override fun onTaskRemoved(rootIntent: Intent) {
//        saveRecentSongToStorage()
//        super.onTaskRemoved(rootIntent)
//        currentPlayer.stop(true)
//    }
//
//    override fun onDestroy() {
//        mediaSession.run {
//            isActive = false
//            release()
//        }
//
//        // Cancel coroutines when the service is going away.
//        serviceJob.cancel()
//        // Free ExoPlayer resources.
//        exoPlayer.removeListener(playerListener)
//        exoPlayer.release()
//    }
//
//    /**
//     * Load the supplied list of songs and the song to play into the current player.
//     */
//    private fun preparePlaylist(
//            metadataList: List<MediaMetadataCompat>,
//            itemToPlay: MediaMetadataCompat?,
//            playWhenReady: Boolean,
//            playbackStartPositionMs: Long
//    ) {
//        // Since the playlist was probably based on some ordering (such as tracks
//        // on an album), find which window index to play first so that the song the
//        // user actually wants to hear plays first.
//        val initialWindowIndex = if (itemToPlay == null) 0 else metadataList.indexOf(itemToPlay)
//        currentPlaylistItems = metadataList
//
//        currentPlayer.playWhenReady = playWhenReady
//        currentPlayer.stop(/* reset= */ true)
//        if (currentPlayer == exoPlayer) {
//            val mediaSource = metadataList.toMediaSource(dataSourceFactory)
//            exoPlayer.prepare(mediaSource)
//            exoPlayer.seekTo(initialWindowIndex, playbackStartPositionMs)
//        } else /* currentPlayer == castPlayer */ {
//            val items: Array<MediaQueueItem> = metadataList.map {
//                it.toMediaQueueItem()
//            }.toTypedArray()
//            castPlayer.loadItems(
//                    items,
//                    initialWindowIndex,
//                    playbackStartPositionMs,
//                    Player.REPEAT_MODE_OFF
//            )
//        }
//    }
//
//    private fun switchToPlayer(previousPlayer: Player?, newPlayer: Player) {
//        if (previousPlayer == newPlayer) {
//            return
//        }
//        currentPlayer = newPlayer
//        if (previousPlayer != null) {
//            val playbackState = previousPlayer.playbackState
//            if (currentPlaylistItems.isEmpty()) {
//                // We are joining a playback session. Loading the session from the new player is
//                // not supported, so we stop playback.
//                currentPlayer.stop(/* reset= */true)
//            } else if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
//                preparePlaylist(
//                        metadataList = currentPlaylistItems,
//                        itemToPlay = currentPlaylistItems[previousPlayer.currentWindowIndex],
//                        playWhenReady = previousPlayer.playWhenReady,
//                        playbackStartPositionMs = previousPlayer.currentPosition
//                )
//            }
//        }
//        mediaSessionConnector.setPlayer(newPlayer)
//        previousPlayer?.stop(/* reset= */true)
//    }
//
//    private inner class UampQueueNavigator(
//            mediaSession: MediaSessionCompat
//    ) : TimelineQueueNavigator(mediaSession) {
//        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
//                currentPlaylistItems[windowIndex].description
//    }
//
//    private inner class UampPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {
//
//        /**
//         * UAMP supports preparing (and playing) from search, as well as media ID, so those
//         * capabilities are declared here.
//         *
//         * TODO: Add support for ACTION_PREPARE and ACTION_PLAY, which mean "prepare/play something".
//         */
//        override fun getSupportedPrepareActions(): Long =
//                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
//                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
//                        PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
//                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
//
//        override fun onPrepare(playWhenReady: Boolean) {
//            onPrepareFromMediaId(
//                    recentSong.mediaId!!,
//                    playWhenReady,
//                    recentSong.description.extras
//            )
//        }
//
//        override fun onPrepareFromMediaId(
//                mediaId: String,
//                playWhenReady: Boolean,
//                extras: Bundle?
//        ) {
//            mediaSource.whenReady {
//                val itemToPlay: MediaMetadataCompat? = mediaSource.find { item ->
//                    item.id == mediaId
//                }
//                if (itemToPlay == null) {
//                    Log.w(TAG, "Content not found: MediaID=$mediaId")
//                    // TODO: Notify caller of the error.
//                } else {
//
//                    val playbackStartPositionMs =
//                            extras.getLong(MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS, C.TIME_UNSET)
//                                    ?: C.TIME_UNSET
//
//                    preparePlaylist(
//                            buildPlaylist(itemToPlay),
//                            itemToPlay,
//                            playWhenReady,
//                            playbackStartPositionMs
//                    )
//                }
//            }
//        }
//
//        private fun buildPlaylist(item: MediaMetadataCompat): List<MediaMetadataCompat> =
//                mediaSource.filter { it.album == item.album }.sortedBy { it.trackNumber }
//    }
//
//    /**
//     * Listen for notification events.
//     */
//    private inner class PlayerNotificationListener :
//            PlayerNotificationManager.NotificationListener {
//        override fun onNotificationPosted(
//                notificationId: Int,
//                notification: Notification,
//                ongoing: Boolean
//        ) {
//            if (ongoing && !isForegroundService) {
//                ContextCompat.startForegroundService(
//                        applicationContext,
//                        Intent(applicationContext, this@MusicService.javaClass)
//                )
//
//                startForeground(notificationId, notification)
//                isForegroundService = true
//            }
//        }
//
//        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
//            stopForeground(true)
//            isForegroundService = false
//            stopSelf()
//        }
//    }
//
//    /**
//     * Listen for events from ExoPlayer.
//     */
//    private inner class PlayerEventListener : Player.EventListener {
//        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
//            when (playbackState) {
//                Player.STATE_BUFFERING,
//                Player.STATE_READY -> {
//                    notificationManager.showNotificationForPlayer(currentPlayer)
//                    if (playbackState == Player.STATE_READY) {
//                        if (!playWhenReady) {
//                            stopForeground(false)
//                        }
//                    }
//                }
//                else -> {
//                    notificationManager.hideNotification()
//                }
//            }
//        }
//
//        override fun onPlayerError(error: ExoPlaybackException) {
//            Log.e(TAG, "Error", error)
//        }
//    }
//}
//
//private const val UAMP_USER_AGENT = "uamp.next"
//val MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS = "playback_start_position_ms"
//private const val TAG = "MusicService"