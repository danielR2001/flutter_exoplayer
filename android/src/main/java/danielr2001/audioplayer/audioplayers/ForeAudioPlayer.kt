//package danielr2001.audioplayer.audioplayers
//
//import android.app.Activity
//import android.app.Notification
//import android.app.PendingIntent
//import android.app.Service
//import android.content.Intent
//import android.net.Uri
//import android.os.Binder
//import android.os.Bundle
//import android.os.IBinder
//import android.os.ResultReceiver
//import android.support.v4.media.MediaDescriptionCompat
//import android.support.v4.media.MediaMetadataCompat
//import android.support.v4.media.session.MediaSessionCompat
//import android.support.v4.media.session.PlaybackStateCompat
//import android.util.Log
//import androidx.core.content.ContextCompat
//import com.google.android.exoplayer2.*
//import com.google.android.exoplayer2.analytics.AnalyticsListener
//import com.google.android.exoplayer2.analytics.AnalyticsListener.EventTime
//import com.google.android.exoplayer2.audio.AudioAttributes
//import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
//import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
//import com.google.android.exoplayer2.source.TrackGroupArray
//import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
//import com.google.android.exoplayer2.trackselection.TrackSelectionArray
//import com.google.android.exoplayer2.ui.PlayerNotificationManager
//import danielr2001.audioplayer.AudioPlayerPlugin
//import danielr2001.audioplayer.enums.PlayerMode
//import danielr2001.audioplayer.enums.PlayerState
//import danielr2001.audioplayer.interfaces.AudioPlayer
//import danielr2001.audioplayer.models.AudioObject
//import danielr2001.audioplayer.notifications.AudioNotificationManager
//import danielr2001.audioplayer.utils.MediaUtils
//import danielr2001.audioplayer.utils.PersistentStorage
//import danielr2001.audioplayer.utils.ext.id
//import danielr2001.audioplayer.utils.ext.toMediaSource
//import okio.ByteString.Companion.decodeHex
//import okio.ByteString.Companion.encodeUtf8
//import java.util.*
//
//class ForegroundAudioPlayer : Service(), AudioPlayer {
//    private val binder: IBinder = LocalBinder()
//
//    inner class LocalBinder : Binder() {
//        val service: ForegroundAudioPlayer
//            get() = this@ForegroundAudioPlayer
//    }
//
//    private var foregroundAudioPlayer: ForegroundAudioPlayer? = null
//    private var mediaNotificationManager: AudioNotificationManager? = null
//    private var ref: AudioPlayerPlugin? = null
//    private var playerId: String? = null
//
//    // player attributes
//    private var speed = 1f
//    private var volume = 1f
//    private var repeatMode = false
//    private var respectAudioFocus = false
//    private var playerMode: PlayerMode? = null
//
//    // player states
//    private var initialized = false
//    private var buffering = false
//    private var playing = false
//    private var stopped = false
//    private var released = true
//    private var completed = false
//
//    private var isForegroundService = false
//
//    private val uAmpAudioAttributes = AudioAttributes.Builder()
//            .setContentType(C.CONTENT_TYPE_MUSIC)
//            .setUsage(C.USAGE_MEDIA)
//            .build()
//
//    private val playerListener = PlayerEventListener()
//    private val playerAnalyticsListenerListener = PlayerAnalyticsListener()
//
//    // ExoPlayer
//    private val player: SimpleExoPlayer by lazy {
//        SimpleExoPlayer.Builder(this).setTrackSelector(DefaultTrackSelector(this)).build().apply {
//            setHandleAudioBecomingNoisy(true)
//            addListener(playerListener)
//            addAnalyticsListener(playerAnalyticsListenerListener)
//        }
//    }
//
//    private lateinit var mediaSession: MediaSessionCompat
//    private lateinit var mediaSessionConnector: MediaSessionConnector
//    private var currentPlaylistItems: List<MediaMetadataCompat> = emptyList()
//    private lateinit var storage: PersistentStorage
//
//    override fun onBind(intent: Intent): IBinder? {
//        return binder
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        val sessionActivityPendingIntent =
//                packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
//                    PendingIntent.getActivity(this, 0, sessionIntent, 0)
//                }
//
//        mediaSession = MediaSessionCompat(this, "MusicService")
//                .apply {
//                    setSessionActivity(sessionActivityPendingIntent)
//                    isActive = true
//                }
//
//        mediaSessionConnector = MediaSessionConnector(mediaSession)
//        mediaSessionConnector.setPlaybackPreparer(MediaPlaybackPreparer())
//        mediaSessionConnector.setQueueNavigator(MediaQueueNavigator(mediaSession))
//
//        mediaNotificationManager = AudioNotificationManager(
//                this,
//                resources.getIdentifier("ic_logo_sm", "drawable", packageName),
//                mediaSession.sessionToken,
//                PlayerNotificationListener()
//        )
//
//        storage = PersistentStorage.getInstance(applicationContext)
//    }
//
//    override fun onTaskRemoved(rootIntent: Intent) {
//        saveRecentSongToStorage()
//        super.onTaskRemoved(rootIntent)
//        player.stop(true)
//    }
//
//    override fun onDestroy() {
//        mediaSession.run {
//            isActive = false
//            release()
//        }
//
//        player.removeListener(playerListener)
//        player.removeAnalyticsListener(playerAnalyticsListenerListener)
//        player.release()
//    }
//
//    override fun setAudioObjects(audioObjects: ArrayList<AudioObject>) {
//        this.currentPlaylistItems = audioObjects.toMediaMetadata()
//    }
//
//    override fun setAudioObject(audioObject: AudioObject) {
//        this.currentPlaylistItems = listOf(audioObject.toMediaMetadata())
//    }
//
//    override fun setSpecificAudioObject(audioObject: AudioObject, index: Int) {
//        this.currentPlaylistItems = listOf(audioObject.toMediaMetadata())
//    }
//
//    override fun initAudioPlayer(ref: AudioPlayerPlugin, activity: Activity, playerId: String) {
//        initialized = true
//        this.playerId = playerId
//        this.ref = ref
//        foregroundAudioPlayer = this
//    }
//
//    override fun initExoPlayer(index: Int, position: Int) {
//        player.setForegroundMode(true)
//        val playbackState = player.playbackState
//        if (currentPlaylistItems.isEmpty()) {
//            player.stop(true)
//        } else if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
//            preparePlaylist(currentPlaylistItems[index], true, position.toLong())
//        }
//        // handle audio focus
//        if (respectAudioFocus) {
//            player.setAudioAttributes(uAmpAudioAttributes, true)
//        }
//        // set repeat mode
//        if (repeatMode) {
//            player.repeatMode = Player.REPEAT_MODE_ALL
//        }
//
//        mediaSessionConnector.setPlayer(player)
//    }
//
//    override fun play(audioObject: AudioObject, position: Int) {
//        this.currentPlaylistItems = listOf(audioObject.toMediaMetadata())
//        initExoPlayer(0, position)
//        player.playWhenReady = true
//    }
//
//    override fun playAll(audioObjects: ArrayList<AudioObject>, index: Int, position: Int) {
//        this.currentPlaylistItems = audioObjects.toMediaMetadata()
//        initExoPlayer(index, position)
//        player.playWhenReady = true
//    }
//
//    override fun next() {
//        player.next()
//        resume()
//    }
//
//    override fun previous() {
//        player.previous()
//        resume()
//    }
//
//    override fun pause() {
//        if (playing) {
//            stopForeground(false)
//            player.playWhenReady = false
//        }
//    }
//
//    override fun resume() {
//        if (!stopped) {
//            completed = false
//        } else {
//            stopped = false
//            initExoPlayer(0, 0)
//            initEventListeners()
//        }
//        player.playWhenReady = true
//    }
//
//    override fun stop() {
//        player.stop(true)
//    }
//
//    override fun release() {
//        if (playing) {
//            stopForeground(true)
//        }
//        player.release()
//        ref?.handleStateChange(this, PlayerState.RELEASED)
//        stopSelf()
//    }
//
//    override fun seekPosition(position: Int) {
//        player.seekTo(player.currentWindowIndex, position.toLong())
//    }
//
//    override fun seekIndex(index: Int) {
//        player.seekTo(index, 0)
//    }
//
//    override fun isPlaying(): Boolean {
//        return playing
//    }
//
//    override fun isBackground(): Boolean {
//        return false
//    }
//
//    override fun isPlayerInitialized(): Boolean {
//        return initialized
//    }
//
//    override fun isPlayerReleased(): Boolean {
//        return released
//    }
//
//    override fun isPlayerCompleted(): Boolean {
//        return completed
//    }
//
//    override fun getPlayerId(): String {
//        return playerId!!
//    }
//
//    override fun getDuration(): Long {
//        return if (!released) {
//            player.duration
//        } else {
//            -1
//        }
//    }
//
//    override fun getCurrentPosition(): Long {
//        return if (!released) {
//            player.currentPosition
//        } else {
//            -1
//        }
//    }
//
//    override fun getCurrentPlayingAudioIndex(): Int {
//        return player.currentWindowIndex
//    }
//
//    override fun getVolume(): Float {
//        return player.volume
//    }
//
//    override fun getPlaybackSpeed(): Float {
//        return speed
//    }
//
//    override fun setPlayerAttributes(repeatMode: Boolean, respectAudioFocus: Boolean, playerMode: PlayerMode) {
//        this.repeatMode = repeatMode
//        this.respectAudioFocus = respectAudioFocus
//        this.playerMode = playerMode
//    }
//
//    override fun setVolume(volume: Float) {
//        if (this.volume != volume) {
//            this.volume = volume
//            player.volume = volume
//        }
//    }
//
//    override fun setRepeatMode(repeatMode: Boolean) {
//        if (this.repeatMode != repeatMode) {
//            this.repeatMode = repeatMode
//            if (this.repeatMode) {
//                player.repeatMode = Player.REPEAT_MODE_ALL
//            } else {
//                player.repeatMode = Player.REPEAT_MODE_OFF
//            }
//        }
//    }
//
//    override fun setPlaybackSpeed(speed: Float) {
//        if (this.speed != speed) {
//            this.speed = speed
//            val param = PlaybackParameters(speed)
//            player.setPlaybackParameters(param)
//        }
//    }
//
//    private fun preparePlaylist(
//            itemToPlay: MediaMetadataCompat?,
//            playWhenReady: Boolean,
//            playbackStartPositionMs: Long
//    ) {
//        val initialWindowIndex = if (itemToPlay == null) 0 else currentPlaylistItems.indexOf(itemToPlay)
//        player.playWhenReady = playWhenReady
//        player.stop(true)
//        val mediaSource = currentPlaylistItems.toMediaSource(MediaUtils.createDataSourceFactory(this))
//        player.prepare(mediaSource)
//        player.seekTo(initialWindowIndex, playbackStartPositionMs)
//    }
//
//    private fun initEventListeners() {
////        mediaSession?.setCallback(object : MediaSessionCompat.Callback() {
////            override fun onSeekTo(pos: Long) {
////                seekPosition(pos.toInt())
////            }
////        })
////        player.addListener(object : Player.EventListener {
////            override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
////                if (mediaNotificationManager!!.isShowing || !mediaNotificationManager!!.isInitialized) {
////                    if (playerMode == PlayerMode.PLAYLIST) {
////                        mediaNotificationManager!!.makeNotification(audioObjects!![player.currentWindowIndex],
////                                true)
////                    } else {
////                        mediaNotificationManager!!.makeNotification(audioObject, true)
////                    }
////                } else {
////                    mediaNotificationManager!!.setIsInitialized(false) // the player was stopped.
////                }
////                ref!!.handlePlayerIndex(foregroundAudioPlayer)
////            }
////
////            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
////                when (playbackState) {
////                    Player.STATE_BUFFERING -> {
////
////                        // buffering
////                        buffering = true
////                        ref!!.handleStateChange(foregroundAudioPlayer, PlayerState.BUFFERING)
////                    }
////                    Player.STATE_READY -> {
////                        if (completed) {
////                            buffering = false
////                            if (mediaNotificationManager!!.isShowing) {
////                                mediaNotificationManager!!.makeNotification(false)
////                            }
////                            ref!!.handleStateChange(foregroundAudioPlayer, PlayerState.COMPLETED)
////                        } else if (buffering) {
////                            // playing
////                            invokeDurationUpdate()
////                            buffering = false
////                            if (playWhenReady) {
////                                playing = true
////                                if (mediaNotificationManager!!.isShowing) {
////                                    mediaNotificationManager!!.makeNotification(true)
////                                }
////                                ref!!.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING)
////                                ref!!.handlePositionUpdates()
////                            } else {
////                                ref!!.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED)
////                            }
////                        } else if (playWhenReady) {
////                            // resumed
////                            playing = true
////                            if (mediaNotificationManager!!.isShowing) {
////                                mediaNotificationManager!!.makeNotification(true)
////                            }
////                            ref?.handlePositionUpdates()
////                            ref?.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING)
////                        } else if (!playWhenReady) {
////                            // paused
////                            playing = false
////                            if (mediaNotificationManager!!.isShowing) {
////                                mediaNotificationManager!!.makeNotification(false)
////                            }
////                            ref?.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED)
////                        }
////                    }
////                    Player.STATE_ENDED -> {
////
////                        // completed
////                        playing = false
////                        completed = true
////                        stopForeground(false)
////                        player.playWhenReady = false
////                        player.seekTo(0, 0)
////                    }
////                    Player.STATE_IDLE -> {
////
////                        // stopped
////                        playing = false
////                        stopped = true
////                        completed = false
////                        buffering = false
////                        ref?.handleStateChange(foregroundAudioPlayer, PlayerState.STOPPED)
////                    } // handle of released is in release method!
////                }
////            }
////
////            override fun onPlayerError(error: ExoPlaybackException) {
////                Log.e(TAG, error.message!!)
////            }
////        })
//    }
//
//    private fun invokeDurationUpdate() {
////        val currentAudioObject: AudioObject? = if (playerMode == PlayerMode.PLAYLIST) {
////            audioObjects!![player.currentWindowIndex]
////        } else {
////            audioObject
////        }
////        currentAudioObject?.duration = duration
////        invokeMediaSeekUpdate()
//    }
//
//    private fun invokeMediaSeekUpdate() {
////        mediaSession?.setPlaybackState(PlaybackStateCompat.Builder()
////                .setState(PlaybackStateCompat.STATE_PLAYING, currentPosition, playbackSpeed)
////                .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
////                .build())
//    }
//
//    private fun saveRecentSongToStorage() {
//        val description = currentPlaylistItems[player.currentWindowIndex].description
//        val position = player.currentPosition
//
//        storage.saveRecentSong(
//                description,
//                position
//        )
//    }
//
//    private inner class MediaQueueNavigator(
//            mediaSession: MediaSessionCompat
//    ) : TimelineQueueNavigator(mediaSession) {
//        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
//                currentPlaylistItems[windowIndex].description
//    }
//
//    private inner class MediaPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {
//        override fun getSupportedPrepareActions(): Long =
//                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
//                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
//                        PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
//                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
//
//        override fun onPrepare(playWhenReady: Boolean) {
//            val recentSong = storage.loadRecentSong() ?: return
//            onPrepareFromMediaId(
//                    recentSong.mediaId.orEmpty(),
//                    playWhenReady,
//                    recentSong.description.extras ?: Bundle()
//            )
//        }
//
//        override fun onPrepareFromMediaId(
//                mediaId: String,
//                playWhenReady: Boolean,
//                extras: Bundle
//        ) {
//            val itemToPlay: MediaMetadataCompat? = currentPlaylistItems.find { item ->
//                item.id == mediaId
//            }
//
//            if (itemToPlay == null) {
//                Log.w(TAG, "Content not found: MediaID=$mediaId")
//            } else {
//                val playbackStartPositionMs =
//                        extras.getLong(MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS, C.TIME_UNSET)
//
//                preparePlaylist(
//                        itemToPlay,
//                        playWhenReady,
//                        playbackStartPositionMs
//                )
//            }
//        }
//
//        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle) = Unit
//
//        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle) = Unit
//
//        override fun onCommand(
//                player: Player,
//                controlDispatcher: ControlDispatcher,
//                command: String,
//                extras: Bundle,
//                cb: ResultReceiver
//        ) = false
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
//                        Intent(applicationContext, this@ForegroundAudioPlayer.javaClass)
//                )
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
//
//        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
//            ref?.handlePlayerIndex(foregroundAudioPlayer)
//        }
//
//        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
//            when (playbackState) {
//                Player.STATE_BUFFERING -> {
//                    buffering = true
//                    mediaNotificationManager?.showNotificationForPlayer(player)
//                    ref?.handleStateChange(foregroundAudioPlayer, PlayerState.BUFFERING)
//                }
//                Player.STATE_READY -> {
//                    buffering = false
//                    mediaNotificationManager?.showNotificationForPlayer(player)
//                    if (playbackState == Player.STATE_READY) {
//                        invokeDurationUpdate()
//                        ref?.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING)
//                        ref?.handlePositionUpdates()
//                        if (!playWhenReady) {
//                            stopForeground(false)
//                            ref?.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED)
//                        }
//                    }
//                }
//                Player.STATE_IDLE -> {
//                    stopForeground(false)
//                    ref?.handleStateChange(foregroundAudioPlayer, PlayerState.STOPPED)
//                }
//                Player.STATE_ENDED -> {
//                    stopForeground(false)
//                }
//                else -> {
//                    buffering = false
//                    mediaNotificationManager?.hideNotification()
//                    ref?.handleStateChange(foregroundAudioPlayer, PlayerState.COMPLETED)
//                }
//            }
//        }
//
//        override fun onPlayerError(error: ExoPlaybackException) {
//            Log.e(TAG, "Error", error)
//        }
//    }
//
//    private inner class PlayerAnalyticsListener : AnalyticsListener {
//        override fun onAudioSessionId(eventTime: EventTime, audioSessionId: Int) {
//            ref?.handleAudioSessionIdChange(foregroundAudioPlayer, audioSessionId)
//        }
//    }
//
//    companion object {
//        const val MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS = "playback_start_position_ms"
//        private const val TAG = "ForegroundAudioPlayer"
//    }
//}