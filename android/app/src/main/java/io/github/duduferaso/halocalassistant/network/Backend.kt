package io.github.duduferaso.halocalassistant.network

import io.github.duduferaso.halocalassistant.domain.ConnectionState
import io.github.duduferaso.halocalassistant.settings.Prefs
import io.github.duduferaso.halocalassistant.wakeword.WakeWords
import org.json.JSONObject
import java.io.File

/**
 * Para onde o terminal manda a voz. Duas implementações:
 *  - HaDirectBackend: fala direto com o WebSocket do Home Assistant (padrão);
 *  - JarvisApi: passa pelo orquestrador opcional no servidor.
 * Ambas entregam os mesmos eventos ao serviço: transcription, response, audio, error.
 */
interface Backend {
    fun health(): ConnectionState
    fun newSession(): Session
    fun download(path: String, dest: File)
}

interface Session {
    val cancelled: Boolean
    fun push(pcm: ByteArray)
    fun finish()
    fun cancel()
    /** Bloqueante. Lança exceção se não alcançar o servidor. */
    fun run(onEvent: (JSONObject) -> Unit)
}

object Backends {
    fun create(prefs: Prefs): Backend =
        if (prefs.directMode) HaDirectBackend(
            prefs.serverUrl, prefs.token, prefs.pipelineId,
            wakeNames = WakeWords.names(prefs),
            extraVocabulary = prefs.extraVocabulary.lines().map { it.trim() }.filter { it.length >= 3 },
            correctText = prefs.correctText,
            calendarEnabled = prefs.calendarEnabled,
            calendarWrite = prefs.calendarWrite,
            localLights = prefs.localLights,
            trained = io.github.duduferaso.halocalassistant.commands.LightCommands.decode(prefs.trainedCommands),
            workoutPlan = prefs.workoutPlan(),
            learnedFixes = if (prefs.learnFromSuccess)
                io.github.duduferaso.halocalassistant.commands.Learning.asMap(io.github.duduferaso.halocalassistant.commands.Learning.decode(prefs.learnedFixes)) else emptyMap(),
            listsEnabled = prefs.listsEnabled,
            shoppingList = prefs.shoppingList,
            tasksList = prefs.tasksList,
            floorMap = prefs.floorMap
        )
        else JarvisApi(prefs)
}
