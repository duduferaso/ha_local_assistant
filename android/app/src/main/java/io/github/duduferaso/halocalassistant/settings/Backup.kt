package io.github.duduferaso.halocalassistant.settings

import android.content.Context
import io.github.duduferaso.halocalassistant.domain.JLog
import io.github.duduferaso.halocalassistant.home.FloorPlan
import io.github.duduferaso.halocalassistant.home.Planta
import io.github.duduferaso.halocalassistant.home.PlantaStore
import io.github.duduferaso.halocalassistant.wakeword.PersonalStore
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pacote de configuração (docs/pacote-configuracao.md): exporta e importa a casa inteira —
 * conexão, voz, funções, planta e ajustes. Importa também o pacote exportado pelo Jarvis Home.
 * O token do Home Assistant só vai se a pessoa pedir.
 */
object Backup {

    /** Arquivos de treino (dentro de filesDir) que vão no pacote. */
    private val FILES = listOf("personal/vozes.tpl", "personal/aprendidos.tpl")

    class Summary(val ajustes: Int, val voz: Boolean, val planta: Boolean, val token: Boolean, val origem: String)

    fun pacoteFileName(): String = "home-control-pacote-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date()) + ".json"

    fun exportPacote(context: Context, includeToken: Boolean): String {
        val app = context.applicationContext
        val p = Prefs(app)
        val ajustes = mapOf<String, Any?>(
            "server_url" to p.serverUrl, "pipeline_id" to p.pipelineId,
            "local_lights" to p.localLights, "show_lights" to p.showLights, "trained_commands" to p.trainedCommands,
            "correct_text" to p.correctText, "extra_vocabulary" to p.extraVocabulary, "show_floor_plan" to p.showFloorPlan,
            "calendar_enabled" to p.calendarEnabled, "show_agenda" to p.showAgenda, "calendar_write" to p.calendarWrite,
            "agenda_reminders" to p.agendaReminders, "reminder_lead" to p.reminderLeadMin,
            "lists_enabled" to p.listsEnabled, "shopping_list" to p.shoppingList, "tasks_list" to p.tasksList,
            "show_weather" to p.showWeather,
            "workout_min" to p.workoutMin, "workout_work" to p.workoutWork, "workout_rest" to p.workoutRest,
            "workout_series" to p.workoutSeries, "workout_count" to p.workoutCount, "workout_exercises" to p.workoutExercises,
            "learn_from_success" to p.learnFromSuccess, "beep" to p.beep, "volume" to p.volume, "silence_ms" to p.silenceMs,
            "night_mode" to p.nightMode, "night_start" to p.nightStart, "night_end" to p.nightEnd,
            "quiet_at_night" to p.quietAtNight, "night_volume" to p.nightVolume, "speak_at_night" to p.speakAtNight,
            "keep_screen_on" to p.keepScreenOn, "dim_screen" to p.dimScreen, "show_clock" to p.showClock,
            "reduce_motion" to p.reduceMotion, "start_on_boot" to p.startOnBoot, "auto_update" to p.autoUpdate,
            "learned_fixes" to p.learnedFixes, "server_temp_limit" to p.serverTempLimit, "server_sensors" to p.serverSensors
        )
        val arquivos = HashMap<String, ByteArray>()
        for (name in FILES) {
            val f = File(app.filesDir, name)
            if (f.exists()) arquivos[name] = f.readBytes()
        }
        // a planta guarda as luzes escolhidas na tela (mapa salvo nas Prefs)
        val escolhidas = FloorPlan.decode(p.floorMap)
        val planta = FloorPlan.casa?.let { casa ->
            Planta.Casa(casa.nome, casa.andares.mapIndexed { i, a ->
                if (i > 0) a else Planta.Andar(a.id, a.nome, a.comodos.map { c ->
                    val ents = escolhidas[c.id] ?: c.entidades
                    Planta.Comodo(c.id, c.nome, c.tipo, c.partes, c.luzes, c.rotulo, ents, c.nomesFalados,
                        c.palavrasLuz, c.evitarPalavras, c.mostrarNome)
                }, a.integrados, a.portas)
            })
        }
        val version = try { app.packageManager.getPackageInfo(app.packageName, 0).versionName } catch (e: Exception) { "?" }
        val criado = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())
        val o = PacoteConfig.montar(PacoteConfig.Fonte(ajustes, arquivos, planta, version ?: "?", criado,
            if (includeToken) p.token else null))
        JLog.i("Pacote exportado" + if (includeToken) " (com token)" else "")
        return o.toString(1)
    }

    /** Importa um pacote (do Home Control ou do Jarvis Home). Lança com mensagem clara se não for um pacote. */
    fun importPacote(context: Context, text: String): Summary {
        val app = context.applicationContext
        val root = try { JSONObject(text) } catch (e: Exception) { throw IllegalArgumentException("o arquivo não é um pacote do Home Control") }
        val l = PacoteConfig.ler(root)
        // confere a voz e a planta ANTES de gravar qualquer coisa
        l.planta?.let { val erros = Planta.validar(it); require(erros.isEmpty()) { "planta com problemas: " + erros.joinToString("; ") } }
        l.exemplosVoz?.let { PersonalStore.importBytes(app, it, learned = false) }
        if (l.aprendidosVoz != null) PersonalStore.importBytes(app, l.aprendidosVoz, learned = true) else PersonalStore.clearLearned(app)

        val ed = app.getSharedPreferences("jarvis", Context.MODE_PRIVATE).edit()
        for ((k, v) in l.ajustes) when (v) {
            is Boolean -> ed.putBoolean(k, v)
            is Int -> ed.putInt(k, v)
            is String -> ed.putString(k, v)
        }
        ed.commit()
        val p = Prefs(app)
        if (l.planta != null) {
            PlantaStore.save(app, l.planta)
            p.floorMap = FloorPlan.encode(l.luzesDaPlanta)
        }
        if (l.token != null) p.token = l.token
        p.pausedForOther = false
        JLog.i("Pacote importado (${l.origem}): ${l.ajustes.size} ajustes" +
            (if (l.exemplosVoz != null) ", voz" else "") + (if (l.planta != null) ", planta" else "") + (if (l.token != null) ", token" else ""))
        return Summary(l.ajustes.size, l.exemplosVoz != null, l.planta != null, l.token != null, l.origem)
    }
}
