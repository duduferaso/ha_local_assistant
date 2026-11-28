import io.github.duduferaso.halocalassistant.home.Planta
import io.github.duduferaso.halocalassistant.settings.PacoteConfig
import org.json.JSONObject

// Pacote de configuração para o app novo (Home Control): tudo o que é modular sai no formato novo.
fun main(a: Array<String>) {
    var falhas = 0
    fun ok(c: Boolean, m: String) { if (!c) falhas++; println((if (c) "PASSOU " else "FALHOU ") + m) }

    val ajustes = mapOf<String, Any?>(
        "server_url" to "http://ha.local:8123", "pipeline_id" to "", "local_lights" to true, "show_lights" to true,
        "trained_commands" to "[{\"text\":\"olha so da cozinha\",\"on\":true,\"lights\":[\"light.cozinha\"]}]",
        "correct_text" to true, "extra_vocabulary" to "Luz da Sala", "show_floor_plan" to true,
        "calendar_enabled" to false, "show_agenda" to false, "calendar_write" to "calendar.casa", "agenda_reminders" to true,
        "reminder_lead" to 10, "lists_enabled" to true, "shopping_list" to "todo.compras", "tasks_list" to "",
        "show_weather" to true, "workout_min" to 30, "workout_work" to 40, "workout_rest" to 20, "workout_series" to 5,
        "workout_count" to 6, "workout_exercises" to "Agachamento\nFlexão", "learn_from_success" to true, "beep" to true,
        "volume" to 90, "silence_ms" to 900, "night_mode" to true, "night_start" to 23, "night_end" to 6,
        "quiet_at_night" to true, "night_volume" to 35, "speak_at_night" to false, "keep_screen_on" to true,
        "dim_screen" to false, "show_clock" to true, "reduce_motion" to false, "start_on_boot" to true,
        "auto_update" to true, "learned_fixes" to "[]", "server_temp_limit" to 75, "server_sensors" to "")
    val voz = ByteArray(300) { (it * 7).toByte() }
    val planta = Planta.fromJson(JSONObject(java.io.File(a.getOrElse(0) { "../examples/planta.json" }).readText()))
    val sem = PacoteConfig.montar(PacoteConfig.Fonte(ajustes, mapOf("personal/vozes.tpl" to voz), planta, "1.16.0", "2026-11-27T19:00:00-03:00"))
    val txt = sem.toString()
    val o = JSONObject(txt)

    ok(o.optString("tipo") == "home-control-pacote" && o.optInt("formato") == 1, "tipo e formato do pacote")
    ok(!o.optJSONObject("conexao")!!.has("token") && !txt.contains("\"token\""), "sem token quando não pedido")
    val com = PacoteConfig.montar(PacoteConfig.Fonte(ajustes, emptyMap(), null, "1.16.0", "x", token = "abc123"))
    ok(com.optJSONObject("conexao")!!.optString("token") == "abc123", "com token quando pedido")

    val v = o.optJSONObject("voz")!!
    ok(java.util.Base64.getDecoder().decode(v.optString("exemplos")).contentEquals(voz), "exemplos de voz vão inteiros (base64)")
    ok(!v.has("aprendidos"), "sem aprendidos quando não existem")

    val m = o.optJSONObject("modulos")!!
    val nomes = listOf("luzes", "planta", "agenda", "listas", "clima", "timers", "treino", "avisos")
    ok(nomes.all { m.optJSONObject(it)?.has("ligado") == true }, "todos os módulos com a chave de ligar: $nomes")
    ok(m.optJSONObject("agenda")!!.optBoolean("ligado", true) == false && m.optJSONObject("agenda")!!.optString("agendaNova") == "calendar.casa",
        "agenda desligada continua desligada, com os ajustes")
    ok(m.optJSONObject("luzes")!!.optJSONArray("comandosTreinados")!!.length() == 1, "comandos treinados vão como lista")
    ok(m.optJSONObject("treino")!!.optInt("execucaoSeg") == 40 && m.optJSONObject("treino")!!.optString("listaExercicios").contains("Flexão"), "ajustes do treino")
    val pl = m.optJSONObject("planta")!!.optJSONObject("planta")!!
    val volta = Planta.fromJson(pl)
    ok(volta.andares.single().comodos.size == 6 && Planta.validar(volta).isEmpty(), "planta dentro do pacote e válida")

    // caminho de volta (importar): os ajustes voltam iguais
    val l = PacoteConfig.ler(JSONObject(com.toString()))
    ok(l.token == "abc123", "importa o token quando veio")
    val lida = PacoteConfig.ler(o)
    val diferentes = ajustes.filter { (k, v) -> lida.ajustes.containsKey(k) && lida.ajustes[k].toString() != v.toString() && k != "trained_commands" && k != "learned_fixes" }
    ok(diferentes.isEmpty(), "ajustes voltam iguais na importação: $diferentes")
    val faltando = ajustes.keys.filter { it !in lida.ajustes && it != "show_lights_x" }
    ok(faltando.isEmpty(), "todos os ajustes voltam: faltam $faltando")
    ok(lida.exemplosVoz!!.contentEquals(voz) && lida.aprendidosVoz == null, "voz volta inteira")
    ok(lida.planta != null && lida.token == null, "planta volta; token não, porque não foi")
    val comLuz = PacoteConfig.montar(PacoteConfig.Fonte(ajustes, emptyMap(), Planta.Casa(planta.nome, listOf(planta.andares[0].let { an ->
        Planta.Andar(an.id, an.nome, an.comodos.map { c -> if (c.id == "cozinha") Planta.Comodo(c.id, c.nome, c.tipo, c.partes, c.luzes, c.rotulo,
            listOf("light.cozinha"), c.nomesFalados, c.palavrasLuz, c.evitarPalavras, c.mostrarNome) else c }, an.integrados, an.portas) })), "1", "x"))
    ok(PacoteConfig.ler(comLuz).luzesDaPlanta == mapOf("cozinha" to listOf("light.cozinha")), "luzes de cada cômodo voltam para o mapa da tela")
    var recusouVoz = false
    try { PacoteConfig.ler(JSONObject(o.toString().replace("jarvis-tpl-1", "outro-formato"))) } catch (e: IllegalArgumentException) { recusouVoz = true }
    ok(recusouVoz, "recusa formato de voz desconhecido")
    ok(o.optJSONObject("noite")!!.optInt("inicio") == 23 && o.optJSONObject("som")!!.optInt("volume") == 90, "noite e som")
    ok(o.optJSONObject("ativacao")!!.optString("frase") == "Hey Tony", "frase de ativação")

    PacoteConfig.conferir(o)
    var recusou = 0
    for (t in listOf("{\"app\":\"JarvisHome\",\"format\":1}", "{\"tipo\":\"home-control-pacote\",\"formato\":9}")) {
        try { PacoteConfig.conferir(JSONObject(t)) } catch (e: IllegalArgumentException) { recusou++ }
    }
    ok(recusou == 2, "recusa cópia antiga e pacote de formato mais novo")

    println(if (falhas == 0) "TODOS OS TESTES PASSARAM" else "$falhas FALHA(S)")
}
