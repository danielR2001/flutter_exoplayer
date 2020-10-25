package danielr2001.audioplayer.audioplayers

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.AnalyticsListener.EventTime
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import danielr2001.audioplayer.AudioPlayerPlugin
import danielr2001.audioplayer.enums.NotificationActionCallbackMode
import danielr2001.audioplayer.enums.NotificationActionName
import danielr2001.audioplayer.enums.PlayerMode
import danielr2001.audioplayer.enums.PlayerState
import danielr2001.audioplayer.interfaces.AudioPlayer
import danielr2001.audioplayer.models.AudioObject
import danielr2001.audioplayer.notifications.MediaNotificationManager
import danielr2001.audioplayer.utils.MediaUtils

class ForegroundAudioPlayer : Service(), AudioPlayer {
    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: ForegroundAudioPlayer
            get() = this@ForegroundAudioPlayer
    }

    private var foregroundAudioPlayer: ForegroundAudioPlayer? = null
    private var mediaNotificationManager: MediaNotificationManager? = null
    private var ref: AudioPlayerPlugin? = null
    private var mediaSession: MediaSessionCompat? = null
    private var playerId: String? = null

    // player attributes
    private var speed = 1f
    private var volume = 1f
    private var repeatMode = false
    private var respectAudioFocus = false
    private var playerMode: PlayerMode? = null

    // player states
    private var initialized = false
    private var buffering = false
    private var playing = false
    private var stopped = false
    private var released = true
    private var completed = false

    private var audioObjects = arrayListOf<AudioObject>()

    private val uAmpAudioAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    private val playerListener = PlayerEventListener()
    private val playerAnalyticsListenerListener = PlayerAnalyticsListener()

    // ExoPlayer
    private val player: SimpleExoPlayer by lazy {
        SimpleExoPlayer.Builder(this).setTrackSelector(DefaultTrackSelector(this)).build().apply {
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
            addAnalyticsListener(playerAnalyticsListenerListener)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("ForegroundService", "ID: $playerId => onCreate")
        mediaSession = MediaSessionCompat(this, "playback")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("ForegroundService", "ID: $playerId => onStartCommand")
        if (intent?.action != null) {
            val currentAudioObject: AudioObject? = if (playerMode == PlayerMode.PLAYLIST) {
                audioObjects[player.currentWindowIndex]
            } else {
                audioObjects.firstOrNull()
            }
            when (intent.action) {
                MediaNotificationManager.PREVIOUS_ACTION -> if (currentAudioObject!!.notificationActionCallbackMode == NotificationActionCallbackMode.DEFAULT) {
                    previous()
                } else {
                    ref?.handleNotificationActionCallback(foregroundAudioPlayer, NotificationActionName.PREVIOUS)
                }
                MediaNotificationManager.PLAY_ACTION -> if (currentAudioObject!!.notificationActionCallbackMode == NotificationActionCallbackMode.DEFAULT) {
                    if (!stopped) {
                        resume()
                    } else {
                        if (playerMode == PlayerMode.PLAYLIST) {
                            playAll(audioObjects, 0, 0)
                        } else {
                            play(audioObjects.first(), 0)
                        }
                    }
                } else {
                    ref?.handleNotificationActionCallback(foregroundAudioPlayer, NotificationActionName.PLAY)
                }
                MediaNotificationManager.PAUSE_ACTION -> if (currentAudioObject!!.notificationActionCallbackMode == NotificationActionCallbackMode.DEFAULT) {
                    pause()
                } else {
                    ref?.handleNotificationActionCallback(foregroundAudioPlayer, NotificationActionName.PAUSE)
                }
                MediaNotificationManager.NEXT_ACTION -> if (currentAudioObject!!.notificationActionCallbackMode == NotificationActionCallbackMode.DEFAULT) {
                    next()
                } else {
                    ref?.handleNotificationActionCallback(foregroundAudioPlayer, NotificationActionName.NEXT)
                }
                MediaNotificationManager.CLOSE_ACTION -> if (currentAudioObject!!.notificationActionCallbackMode == NotificationActionCallbackMode.DEFAULT) {
                    release()
                } else {
                    ref?.handleNotificationActionCallback(foregroundAudioPlayer, NotificationActionName.CLOSE)
                }
                MediaNotificationManager.CUSTOM1_ACTION -> ref?.handleNotificationActionCallback(foregroundAudioPlayer, NotificationActionName.CUSTOM1)
                MediaNotificationManager.CUSTOM2_ACTION -> ref?.handleNotificationActionCallback(foregroundAudioPlayer, NotificationActionName.CUSTOM2)
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Log.i("ForegroundService", "ID: $playerId => onTaskRemoved")
        release()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.i("ForegroundService", "ID: $playerId => onDestroy")
        super.onDestroy()
        mediaSession?.run {
            isActive = false
            release()
        }
        release()
    }

    override fun setAudioObjects(audioObjects: ArrayList<AudioObject>) {
        if (this.audioObjects.isNotEmpty()) {
            for (i in this.audioObjects.indices) {
                audioObjects[i].url = this.audioObjects[i].url
                audioObjects[i].isLocal = this.audioObjects[i].isLocal
            }
            this.audioObjects = audioObjects.clone() as ArrayList<AudioObject>
            mediaNotificationManager?.makeNotification(this.audioObjects[player.currentWindowIndex], playing)
        }
    }

    override fun setAudioObject(audioObject: AudioObject) {
        if (this.audioObjects.isNotEmpty()) {
            audioObject.url = this.audioObjects.first().url
            audioObject.isLocal = this.audioObjects.first().isLocal
            this.audioObjects[0] = audioObject
            mediaNotificationManager?.makeNotification(this.audioObjects.firstOrNull(), playing)
        }
    }

    override fun setSpecificAudioObject(audioObject: AudioObject, index: Int) {
        if (audioObjects.isNotEmpty()) {
            audioObject.url = audioObjects[index].url
            audioObject.isLocal = audioObjects[index].isLocal
            audioObjects[index] = audioObject
            if (currentPlayingAudioIndex == index) mediaNotificationManager?.makeNotification(audioObjects[player.currentWindowIndex], playing)
        }
    }

    override fun initAudioPlayer(ref: AudioPlayerPlugin, activity: Activity, playerId: String) {
        initialized = true
        this.playerId = playerId
        this.ref = ref
        mediaNotificationManager = MediaNotificationManager(this, mediaSession, activity)
        foregroundAudioPlayer = this
    }

    override fun initExoPlayer(index: Int, position: Int) {
        player.setForegroundMode(true)
        // playlist/single audio load
        if (playerMode == PlayerMode.PLAYLIST) {
            val concatenatingMediaSource = ConcatenatingMediaSource()
            for (audioObject in audioObjects) {
                val mediaSource: MediaSource = ProgressiveMediaSource.Factory(MediaUtils.createDataSourceFactory(this))
                        .createMediaSource(Uri.parse(audioObject.url))
                concatenatingMediaSource.addMediaSource(mediaSource)
            }
            player.prepare(concatenatingMediaSource)
            if (index != 0) {
                player.seekTo(index, position.toLong())
            }
        } else {
            val mediaSource: MediaSource = ProgressiveMediaSource.Factory(MediaUtils.createDataSourceFactory(this))
                    .createMediaSource(Uri.parse(audioObjects.first().url))
            player.prepare(mediaSource)
            player.seekTo(0, position.toLong())
        }
        // handle audio focus
        if (respectAudioFocus) { // ! TODO catch duck pause!
            player.setAudioAttributes(uAmpAudioAttributes, true)
        }
        // set repeat mode
        if (repeatMode) {
            player.repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    override fun play(audioObject: AudioObject, position: Int) {
        if (completed) {
            resume()
        } else {
            released = false
            this.audioObjects = arrayListOf(audioObject)
            initExoPlayer(0, position)
            initEventListeners()
            player.playWhenReady = true
        }
    }

    override fun playAll(audioObjects: ArrayList<AudioObject>, index: Int, position: Int) {
        if (completed || stopped) {
            resume()
        } else {
            released = false
            this.audioObjects = audioObjects
            initExoPlayer(index, position)
            initEventListeners()
            player.playWhenReady = true
        }
    }

    override fun next() {
        if (!released) {
            player.next()
            resume()
        }
    }

    override fun previous() {
        if (!released) {
            player.previous()
            resume()
        }
    }

    override fun pause() {
        if (!released && playing) {
            stopForeground(false)
            player.playWhenReady = false
        }
    }

    override fun resume() {
        if (!released && !playing) {
            if (!stopped) {
                completed = false
            } else {
                stopped = false
                initExoPlayer(0, 0)
                initEventListeners()
            }
            player.playWhenReady = true
        }
    }

    override fun stop() {
        if (!released) {
            mediaNotificationManager!!.setIsShowing(false)
            stopForeground(true)
            player.stop(true)
        }
    }

    override fun release() {
        if (!released) {
            if (playing) {
                stopForeground(true)
                mediaNotificationManager!!.setIsShowing(false)
            }
            initialized = false
            buffering = false
            playing = false
            stopped = false
            released = true
            completed = false
            audioObjects.clear()

            player.removeListener(playerListener)
            player.removeAnalyticsListener(playerAnalyticsListenerListener)
            player.release()
            ref?.handleStateChange(this, PlayerState.RELEASED)
            stopSelf()
        }
    }

    override fun seekPosition(position: Int) {
        if (!released) {
            player.seekTo(player.currentWindowIndex, position.toLong())
        }
    }

    override fun seekIndex(index: Int) {
        if (!released) {
            player.seekTo(index, 0)
        }
    }

    override fun isPlaying(): Boolean {
        return playing
    }

    override fun isBackground(): Boolean {
        return false
    }

    override fun isPlayerInitialized(): Boolean {
        return initialized
    }

    override fun isPlayerReleased(): Boolean {
        return released
    }

    override fun isPlayerCompleted(): Boolean {
        return completed
    }

    override fun getPlayerId(): String {
        return playerId!!
    }

    override fun getDuration(): Long {
        return if (!released) {
            player.duration
        } else {
            -1
        }
    }

    override fun getCurrentPosition(): Long {
        return if (!released) {
            player.currentPosition
        } else {
            -1
        }
    }

    override fun getCurrentPlayingAudioIndex(): Int {
        return player.currentWindowIndex
    }

    override fun getVolume(): Float {
        return player.volume
    }

    override fun getPlaybackSpeed(): Float {
        return speed
    }

    override fun setPlayerAttributes(repeatMode: Boolean, respectAudioFocus: Boolean, playerMode: PlayerMode) {
        this.repeatMode = repeatMode
        this.respectAudioFocus = respectAudioFocus
        this.playerMode = playerMode
    }

    override fun setVolume(volume: Float) {
        if (!released && this.volume != volume) {
            this.volume = volume
            player.volume = volume
        }
    }

    override fun setRepeatMode(repeatMode: Boolean) {
        if (!released && this.repeatMode != repeatMode) {
            this.repeatMode = repeatMode
            if (this.repeatMode) {
                player.repeatMode = Player.REPEAT_MODE_ALL
            } else {
                player.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        if (!released && this.speed != speed) {
            this.speed = speed
            val param = PlaybackParameters(speed)
            player.setPlaybackParameters(param)
        }
    }

    private fun initEventListeners() {
        mediaSession?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onSeekTo(pos: Long) {
                seekPosition(pos.toInt())
            }
        })
    }

    private fun invokeDurationUpdate() {
        val currentAudioObject: AudioObject? = if (playerMode == PlayerMode.PLAYLIST) {
            audioObjects[player.currentWindowIndex]
        } else {
            audioObjects.firstOrNull()
        }
        currentAudioObject?.duration = duration
        invokeMediaSeekUpdate()
    }

    private fun invokeMediaSeekUpdate() {
        mediaSession?.setPlaybackState(PlaybackStateCompat.Builder()
                .setState(player.playbackState, currentPosition, if (player.playbackState == PlaybackStateCompat.STATE_PLAYING) playbackSpeed else 0f)
                .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                .build())
    }

    /**
     * Listen for events from ExoPlayer.
     */
    private inner class PlayerEventListener : Player.EventListener {

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
            if (mediaNotificationManager != null && (mediaNotificationManager!!.isShowing || !mediaNotificationManager!!.isInitialized)) {
                if (playerMode == PlayerMode.PLAYLIST) {
                    mediaNotificationManager?.makeNotification(audioObjects[player.currentWindowIndex],
                            true)
                } else {
                    mediaNotificationManager?.makeNotification(audioObjects.firstOrNull(), true)
                }
            } else {
                mediaNotificationManager?.setIsInitialized(false) // the player was stopped.
            }
            invokeDurationUpdate()
            ref?.handlePlayerIndex(foregroundAudioPlayer)
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    // buffering
                    buffering = true
                    ref?.handleStateChange(foregroundAudioPlayer, PlayerState.BUFFERING)
                }
                Player.STATE_READY -> {
                    invokeDurationUpdate()
                    if (completed) {
                        buffering = false
                        if (mediaNotificationManager!!.isShowing) {
                            mediaNotificationManager?.makeNotification(false)
                        }
                        ref?.handleStateChange(foregroundAudioPlayer, PlayerState.COMPLETED)
                    } else if (buffering) {
                        // playing
                        buffering = false
                        if (playWhenReady) {
                            playing = true
                            if (mediaNotificationManager!!.isShowing) {
                                mediaNotificationManager!!.makeNotification(true)
                            }
                            ref?.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING)
                            ref?.handlePositionUpdates()
                        } else {
                            ref?.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED)
                        }
                    } else if (playWhenReady) {
                        // resumed
                        playing = true
                        if (mediaNotificationManager!!.isShowing) {
                            mediaNotificationManager!!.makeNotification(true)
                        }
                        ref?.handlePositionUpdates()
                        ref?.handleStateChange(foregroundAudioPlayer, PlayerState.PLAYING)
                    } else if (!playWhenReady) {
                        // paused
                        playing = false
                        if (mediaNotificationManager!!.isShowing) {
                            mediaNotificationManager!!.makeNotification(false)
                        }
                        ref?.handleStateChange(foregroundAudioPlayer, PlayerState.PAUSED)
                    }
                }
                Player.STATE_ENDED -> {
                    // completed
                    invokeDurationUpdate()
                    playing = false
                    completed = true
                    stopForeground(false)
                    player.playWhenReady = false
                    player.seekTo(0, 0)
                }
                Player.STATE_IDLE -> {
                    // stopped
                    invokeDurationUpdate()
                    playing = false
                    stopped = true
                    completed = false
                    buffering = false
                    ref?.handleStateChange(foregroundAudioPlayer, PlayerState.STOPPED)
                } // handle of released is in release method!
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            Log.e(TAG, error.message, error)
        }
    }

    private inner class PlayerAnalyticsListener : AnalyticsListener {
        override fun onAudioSessionId(eventTime: EventTime, audioSessionId: Int) {
            ref?.handleAudioSessionIdChange(foregroundAudioPlayer, audioSessionId)
        }
    }

    companion object {
        private const val TAG = "ForegroundAudioPlayer"
    }
}