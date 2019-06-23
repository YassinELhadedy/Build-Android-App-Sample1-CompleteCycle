package com.transporter.streetglide.ui.util

import android.app.Activity
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.transporter.streetglide.R
import java.io.Closeable
import java.io.IOException


class BeepManager(private val activity: Activity) : MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, Closeable {

    private var mediaPlayer: MediaPlayer? = null
    private var playBeep: Boolean = false
    private val vibrate: Boolean = true

    init {
        updatePrefs()
    }

    @Synchronized
    private fun updatePrefs() {
        playBeep = shouldBeep(activity)
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            activity.volumeControlStream = AudioManager.STREAM_MUSIC
            mediaPlayer = buildMediaPlayer(activity)
        }
    }

    @Synchronized
    fun playBeepSoundAndVibrate() {
        if (playBeep) {
            mediaPlayer?.start()
        }
        if (vibrate && Build.VERSION.SDK_INT >= 26) {
            (activity.getSystemService(VIBRATOR_SERVICE) as Vibrator)
                    .vibrate(VibrationEffect.createOneShot(150, VIBRATE_DURATION))
        }
    }

    private fun buildMediaPlayer(activity: Context): MediaPlayer? {
        val mediaPlayer = MediaPlayer()
        val audioAttr = AudioAttributes.Builder().build()
        mediaPlayer.setAudioAttributes(audioAttr)
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnErrorListener(this)
        return try {
            activity.resources.openRawResourceFd(R.raw.beep).use { file ->
                mediaPlayer.setDataSource(file.fileDescriptor, file.startOffset, file.length)
            }
            mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME)
            mediaPlayer.prepare()
            mediaPlayer
        } catch (ioe: IOException) {
            Log.w(TAG, ioe)
            mediaPlayer.release()
            null
        }
    }

    override fun onCompletion(mp: MediaPlayer) {
        // When the beep has finished playing, rewind to queue up another one.
        mp.seekTo(0)
    }

    @Synchronized
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
            // we are finished, so put up an appropriate error toast if required
            // and finish
            activity.finish()
        } else {
            // possibly media player error, so release and recreate
            mp.release()
            mediaPlayer = null
            updatePrefs()
        }
        return true
    }

    @Synchronized
    override fun close() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {

        private val TAG = BeepManager::class.java.simpleName
        private const val BEEP_VOLUME = 0.10f
        private const val VIBRATE_DURATION = 200

        private fun shouldBeep(activity: Context): Boolean {
            // See if sound settings overrides this
            val audioService = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audioService.ringerMode == AudioManager.RINGER_MODE_NORMAL
        }
    }
}