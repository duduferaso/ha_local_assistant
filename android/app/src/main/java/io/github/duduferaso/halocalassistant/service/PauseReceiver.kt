package io.github.duduferaso.halocalassistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.duduferaso.halocalassistant.domain.JLog
import io.github.duduferaso.halocalassistant.settings.Prefs

/**
 * O Jarvis Home (app antigo) pede para este app pausar quando volta a ser usado.
 * Os dois convivem no mesmo celular, um ativo por vez (o microfone é de um só).
 */
class PauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != JarvisForegroundService.BROADCAST_PAUSE_ME) return
        val prefs = Prefs(context)
        if (prefs.pausedForOther) return
        JLog.i("Jarvis Home pediu a pausa total")
        try {
            JarvisForegroundService.send(context, JarvisForegroundService.ACTION_PAUSE_TOTAL)
        } catch (t: Throwable) {
            // serviço parado: basta marcar; ele já inicia em pausa
            prefs.pausedForOther = true
            prefs.assistantEnabled = false
        }
    }
}
