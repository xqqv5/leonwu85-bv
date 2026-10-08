package dev.aaa1115910.bv.mobile.component.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun rememberMediaMuteState(enabled: Boolean): State<Boolean> {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val muted = remember(audioManager, enabled) {
        mutableStateOf(enabled && audioManager.isMediaMuted())
    }

    DisposableEffect(context, audioManager, lifecycleOwner, enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        fun refresh() {
            muted.value = audioManager.isMediaMuted()
        }

        // These system broadcasts cover hardware keys, player gestures, mute and output changes.
        // Read AudioManager rather than trusting the broadcast's extras.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
            addAction("android.media.STREAM_DEVICES_CHANGED_ACTION")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refresh()

        onDispose {
            context.unregisterReceiver(receiver)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return muted
}

private fun AudioManager.isMediaMuted(): Boolean =
    getStreamVolume(AudioManager.STREAM_MUSIC) == 0 || isStreamMute(AudioManager.STREAM_MUSIC)
