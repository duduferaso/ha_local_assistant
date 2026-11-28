package io.github.duduferaso.halocalassistant.settings

import io.github.duduferaso.halocalassistant.home.Planta
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pacote de configuração do Home Control (formato 1): a casa inteira num arquivo. Serve para migrar do
 * Jarvis Home, como backup e para levar a casa a outro celular. Especificação: docs/pacote-configuracao.md.
 *
 * Lógica pura (sem Android): [montar] recebe os ajustes já com os valores padrão aplicados (chaves das
 * Prefs), os arquivos de voz e a planta; [ler] faz o caminho de volta. Testada em tests/jvm_pacote.
 */
object PacoteConfig {
    const val TIPO = "home-control-pacote"
    const val FORMATO = 1
    /** Formato dos arquivos de exemplos de voz (o mesmo vozes.tpl do detector pessoal). */
    const val FORMATO_VOZ = "jarvis-tpl-1"

    class Fonte(
        val ajustes: Map<String, Any?>,
        val arquivos: Map<String, ByteArray>,
        val planta: Planta.Casa?,
        val versaoApp: String,
        val criadoEm: String,
        val token: String? = null,
        val app: String = "HomeControl"
    )

    private fun b64(b: ByteArray) = java.util.Base64.getEncoder().encodeToString(b)

    private fun arr(txt: String): JSONArray = try {
        // org.json (Android) aceita o texto direto; o stub dos testes também
        val o = JSONObject("{\"a\":" + txt.ifBlank { "[]" } + "}")
        o.optJSONArray("a") ?: JSONArray()
    } catch (e: Exception) { JSONArray() }

    fun montar(f: Fonte): JSONObject {
        val a = f.ajustes
        fun b(k: String) = a[k] as? Boolean ?: false
        fun i(k: String) = (a[k] as? Number)?.toInt() ?: 0
        fun s(k: String) = a[k]?.toString() ?: ""

        val conexao = JSONObject().put("endereco", s("server_url")).put("assistente", s("pipeline_id"))
        f.token?.takeIf { it.isNotEmpty() }?.let { conexao.put("token", it) }

        val voz = JSONObject().put("formatoExemplos", FORMATO_VOZ)
        f.arquivos["personal/vozes.tpl"]?.let { voz.put("exemplos", b64(it)) }
        f.arquivos["personal/aprendidos.tpl"]?.let { voz.put("aprendidos", b64(it)) }

        val modulos = JSONObject()
            .put("luzes", JSONObject().put("ligado", b("local_lights")).put("naTela", b("show_lights"))
                .put("comandosTreinados", arr(s("trained_commands")))
                .put("corretor", JSONObject().put("ligado", b("correct_text")).put("vocabularioExtra", s("extra_vocabulary"))))
            .put("planta", JSONObject().put("ligado", f.planta != null).put("naTela", b("show_floor_plan")).apply {
                f.planta?.let { put("planta", Planta.toJson(it)) }
            })
            .put("agenda", JSONObject().put("ligado", b("calendar_enabled")).put("naTela", b("show_agenda"))
                .put("agendaNova", s("calendar_write")).put("lembretes", b("agenda_reminders")).put("antecedenciaMin", i("reminder_lead")))
            .put("listas", JSONObject().put("ligado", b("lists_enabled")).put("compras", s("shopping_list")).put("tarefas", s("tasks_list")))
            .put("clima", JSONObject().put("ligado", true).put("naTela", b("show_weather")))
            .put("timers", JSONObject().put("ligado", true))
            .put("treino", JSONObject().put("ligado", true).put("minutos", i("workout_min")).put("execucaoSeg", i("workout_work"))
                .put("descansoSeg", i("workout_rest")).put("series", i("workout_series")).put("exercicios", i("workout_count"))
                .put("listaExercicios", s("workout_exercises")))
            .put("avisos", JSONObject().put("ligado", true))

        return JSONObject()
            .put("tipo", TIPO).put("formato", FORMATO).put("criadoEm", f.criadoEm)
            .put("origem", JSONObject().put("app", f.app).put("versao", f.versaoApp))
            .put("conexao", conexao)
            .put("ativacao", JSONObject().put("frase", "Hey Tony").put("aprenderComUso", b("learn_from_success")))
            .put("voz", voz)
            .put("modulos", modulos)
            .put("som", JSONObject().put("bipe", b("beep")).put("volume", i("volume")).put("silencioMs", i("silence_ms")))
            .put("noite", JSONObject().put("ligado", b("night_mode")).put("inicio", i("night_start")).put("fim", i("night_end"))
                .put("respostasBaixas", b("quiet_at_night")).put("volume", i("night_volume")).put("falarAvisos", b("speak_at_night")))
            .put("tela", JSONObject().put("manterLigada", b("keep_screen_on")).put("brilhoMinimo", b("dim_screen"))
                .put("relogio", b("show_clock")).put("reduzirMovimento", b("reduce_motion")))
            .put("sistema", JSONObject().put("iniciarAoLigar", b("start_on_boot")).put("atualizarSozinho", b("auto_update")))
            .put("aprendizado", JSONObject().put("correcoes", arr(s("learned_fixes"))))
            .put("extras", JSONObject().put("servidor", JSONObject().put("limiteTemperatura", i("server_temp_limit")).put("sensores", s("server_sensors"))))
    }

    /** O que um pacote traz, já no formato das Prefs (só as chaves presentes no pacote). */
    class Leitura(
        val ajustes: Map<String, Any>,
        val exemplosVoz: ByteArray?,
        val aprendidosVoz: ByteArray?,
        val planta: Planta.Casa?,
        val luzesDaPlanta: Map<String, List<String>>,
        val token: String?,
        val origem: String
    )

    /** Caminho de volta de [montar]. Lança IllegalArgumentException com mensagem clara se não for um pacote. */
    fun ler(o: JSONObject): Leitura {
        conferir(o)
        val aj = LinkedHashMap<String, Any>()
        fun b(obj: JSONObject?, campo: String, chave: String) { if (obj != null && obj.has(campo)) aj[chave] = obj.optBoolean(campo) }
        fun i(obj: JSONObject?, campo: String, chave: String) { if (obj != null && obj.has(campo)) aj[chave] = obj.optInt(campo) }
        fun s(obj: JSONObject?, campo: String, chave: String) { if (obj != null && obj.has(campo)) aj[chave] = obj.optString(campo) }
        fun lista(obj: JSONObject?, campo: String, chave: String) { obj?.optJSONArray(campo)?.let { aj[chave] = it.toString() } }

        val con = o.optJSONObject("conexao")
        s(con, "endereco", "server_url"); s(con, "assistente", "pipeline_id")
        b(o.optJSONObject("ativacao"), "aprenderComUso", "learn_from_success")
        val m = o.optJSONObject("modulos")
        val luzes = m?.optJSONObject("luzes")
        b(luzes, "ligado", "local_lights"); b(luzes, "naTela", "show_lights"); lista(luzes, "comandosTreinados", "trained_commands")
        val cor = luzes?.optJSONObject("corretor")
        b(cor, "ligado", "correct_text"); s(cor, "vocabularioExtra", "extra_vocabulary")
        b(m?.optJSONObject("planta"), "naTela", "show_floor_plan")
        val ag = m?.optJSONObject("agenda")
        b(ag, "ligado", "calendar_enabled"); b(ag, "naTela", "show_agenda"); s(ag, "agendaNova", "calendar_write")
        b(ag, "lembretes", "agenda_reminders"); i(ag, "antecedenciaMin", "reminder_lead")
        val li = m?.optJSONObject("listas")
        b(li, "ligado", "lists_enabled"); s(li, "compras", "shopping_list"); s(li, "tarefas", "tasks_list")
        b(m?.optJSONObject("clima"), "naTela", "show_weather")
        val tr = m?.optJSONObject("treino")
        i(tr, "minutos", "workout_min"); i(tr, "execucaoSeg", "workout_work"); i(tr, "descansoSeg", "workout_rest")
        i(tr, "series", "workout_series"); i(tr, "exercicios", "workout_count"); s(tr, "listaExercicios", "workout_exercises")
        val som = o.optJSONObject("som")
        b(som, "bipe", "beep"); i(som, "volume", "volume"); i(som, "silencioMs", "silence_ms")
        val no = o.optJSONObject("noite")
        b(no, "ligado", "night_mode"); i(no, "inicio", "night_start"); i(no, "fim", "night_end")
        b(no, "respostasBaixas", "quiet_at_night"); i(no, "volume", "night_volume"); b(no, "falarAvisos", "speak_at_night")
        val te = o.optJSONObject("tela")
        b(te, "manterLigada", "keep_screen_on"); b(te, "brilhoMinimo", "dim_screen"); b(te, "relogio", "show_clock")
        b(te, "reduzirMovimento", "reduce_motion")
        val si = o.optJSONObject("sistema")
        b(si, "iniciarAoLigar", "start_on_boot"); b(si, "atualizarSozinho", "auto_update")
        lista(o.optJSONObject("aprendizado"), "correcoes", "learned_fixes")
        val sv = o.optJSONObject("extras")?.optJSONObject("servidor")
        i(sv, "limiteTemperatura", "server_temp_limit"); s(sv, "sensores", "server_sensors")

        val voz = o.optJSONObject("voz")
        if (voz != null && voz.has("exemplos") && voz.optString("formatoExemplos", FORMATO_VOZ) != FORMATO_VOZ)
            throw IllegalArgumentException("formato de voz desconhecido: " + voz.optString("formatoExemplos"))
        fun bytes(campo: String): ByteArray? = voz?.optString(campo)?.takeIf { it.isNotEmpty() }?.let {
            try { java.util.Base64.getDecoder().decode(it) } catch (e: IllegalArgumentException) { throw IllegalArgumentException("exemplos de voz corrompidos no pacote") }
        }

        val plantaJson = m?.optJSONObject("planta")?.takeIf { it.optBoolean("ligado", true) }?.optJSONObject("planta")
        val planta = plantaJson?.let { Planta.fromJson(it) }
        val luzesDaPlanta = planta?.andares?.firstOrNull()?.comodos.orEmpty()
            .filter { it.entidades.isNotEmpty() }.associate { it.id to it.entidades }

        return Leitura(aj, bytes("exemplos"), bytes("aprendidos"), planta, luzesDaPlanta,
            con?.optString("token")?.takeIf { it.isNotEmpty() },
            (o.optJSONObject("origem")?.optString("app") ?: "?") + " " + (o.optJSONObject("origem")?.optString("versao") ?: ""))
    }

    /** Confere se o texto é um pacote que este formato entende. Lança com mensagem clara se não for. */
    fun conferir(o: JSONObject) {
        require(o.optString("tipo") == TIPO) { "o arquivo não é um pacote do Home Control" }
        val f = o.optInt("formato", 0)
        require(f in 1..FORMATO) { "pacote feito por uma versão mais nova; atualize o app" }
    }
}
