package io.github.duduferaso.halocalassistant.home

import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer

/**
 * Planta da casa em uso, em metros, origem no canto de cima à esquerda.
 *  - Cômodos = áreas de luz (um ou mais retângulos).
 *  - Paredes = calculadas a partir dos cômodos (ver Planta.paredes), com os vãos das portas.
 * A planta vem do arquivo planta.json de cada casa (PlantaStore); sem planta, a lista fica vazia
 * e a tela mostra só o personagem. Cada cômodo controla uma ou mais luzes do Home Assistant
 * (escolhidas no app ou encontradas pelo nome). Lógica pura, testável na JVM.
 */
object FloorPlan {

    class Zone(
        val id: String,
        val name: String,
        val rects: List<FloatArray>,          // x1, y1, x2, y2
        val lightPoints: List<FloatArray>,    // x, y
        val label: FloatArray,                // x, y
        val keywords: List<String>,           // para achar a luz pelo nome
        val avoid: List<String> = emptyList(),
        val showLabel: Boolean = true,
        val spoken: List<String> = emptyList()
    ) {
        fun contains(x: Float, y: Float) = rects.any { x >= it[0] && x <= it[2] && y >= it[1] && y <= it[3] }
    }

    @Volatile var zones: List<Zone> = emptyList(); private set
    @Volatile var walls: List<FloatArray> = emptyList(); private set
    @Volatile var width = 0f; private set
    @Volatile var depth = 0f; private set
    /** A planta carregada (primeiro andar), para exportar e editar. */
    @Volatile var casa: Planta.Casa? = null; private set

    val isEmpty get() = zones.isEmpty()

    private val STOP = setOf("de", "do", "da", "dos", "das", "e", "o", "a")

    /** Troca a planta em uso. Andares além do primeiro ficam para a fase 2. */
    fun load(nova: Planta.Casa?) {
        val andar = nova?.andares?.firstOrNull()
        if (andar == null || andar.comodos.isEmpty()) {
            zones = emptyList(); walls = emptyList(); width = 0f; depth = 0f; casa = nova; return
        }
        zones = andar.comodos.map { c ->
            val rects = c.partes.map { p -> FloatArray(4) { i -> p[i].toFloat() } }
            val maior = rects.maxByOrNull { (it[2] - it[0]) * (it[3] - it[1]) }!!
            val label = c.rotulo?.let { floatArrayOf(it[0].toFloat(), it[1].toFloat()) }
                ?: floatArrayOf((maior[0] + maior[2]) / 2, (maior[1] + maior[3]) / 2)
            val spoken = c.nomesFalados.ifEmpty { listOf(c.nome.lowercase()) }
            val keys = c.palavrasLuz.ifEmpty {
                (listOf(c.nome) + spoken).flatMap { norm(it).split(' ') }.filter { it.length > 2 && it !in STOP }.distinct()
            }
            Zone(c.id, c.nome, rects, c.luzes.map { floatArrayOf(it[0].toFloat(), it[1].toFloat()) }, label,
                keys, c.evitarPalavras, c.mostrarNome, spoken)
        }
        walls = Planta.paredes(andar).map { w -> FloatArray(4) { i -> w[i].toFloat() } }
        width = zones.maxOf { z -> z.rects.maxOf { it[2] } }
        depth = zones.maxOf { z -> z.rects.maxOf { it[3] } }
        casa = nova
    }

    fun zoneAt(x: Float, y: Float): Zone? = zones.firstOrNull { it.contains(x, y) }

    // ------------------------------------------------------------------ luzes de cada cômodo

    /** Jeitos de falar cada cômodo por voz (o primeiro é o nome usado na resposta). */
    fun spokenNames(zoneId: String): List<String> = zones.firstOrNull { it.id == zoneId }?.spoken.orEmpty()

    class Relay(val entityId: String, val name: String, val on: Boolean, val available: Boolean)

    private fun norm(s: String) = io.github.duduferaso.halocalassistant.network.TextCorrector.normalize(s)

    /** Acha a luz de cada cômodo pelo nome (cada relé vai para um cômodo só). */
    fun autoMatch(relays: List<Relay>): Map<String, List<String>> {
        val out = LinkedHashMap<String, List<String>>()
        val used = HashSet<String>()
        fun text(r: Relay) = " " + norm(r.name) + " " + norm(r.entityId.substringAfter('.')) + " "
        // primeiro os nomes específicos, depois os genéricos ("quarto" sozinho vai para o Quarto 1)
        for (z in zones) {
            val best = relays.filter { it.entityId !in used }.map { rl ->
                val t = text(rl)
                val score = z.keywords.count { k -> t.contains(" $k ") || t.contains(" $k") } -
                    z.avoid.count { a -> t.contains(a) } * 5
                rl to score
            }.filter { it.second > 0 }.sortedByDescending { it.second }
            val pick = best.firstOrNull()?.first ?: continue
            out[z.id] = listOf(pick.entityId)
            used.add(pick.entityId)
        }
        if (out["quarto1"] == null) {
            relays.firstOrNull { it.entityId !in used && text(it).contains(" quarto") }?.let {
                out["quarto1"] = listOf(it.entityId); used.add(it.entityId)
            }
        }
        if (out["quarto2"] == null) {
            relays.firstOrNull { it.entityId !in used && text(it).contains(" quarto") }?.let {
                out["quarto2"] = listOf(it.entityId); used.add(it.entityId)
            }
        }
        return out
    }

    /** Escolhas feitas no app (têm prioridade); cômodo sem escolha usa a busca pelo nome. */
    fun mapping(saved: Map<String, List<String>>, relays: List<Relay>): Map<String, List<String>> {
        val auto = autoMatch(relays.filter { r -> saved.values.none { r.entityId in it } })
        val out = LinkedHashMap<String, List<String>>()
        for (z in zones) out[z.id] = saved[z.id] ?: auto[z.id] ?: emptyList()
        return out
    }

    /** null = cômodo sem luz; true/false = alguma luz acesa. */
    fun isOn(zone: Zone, map: Map<String, List<String>>, relays: List<Relay>): Boolean? {
        val ids = map[zone.id].orEmpty()
        val mine = relays.filter { it.entityId in ids }
        if (mine.isEmpty()) return null
        return mine.any { it.on }
    }

    fun encode(m: Map<String, List<String>>): String {
        val o = JSONObject()
        for ((k, v) in m) { val a = JSONArray(); v.forEach { a.put(it) }; o.put(k, a) }
        return o.toString()
    }

    fun decode(s: String): Map<String, List<String>> = try {
        val o = JSONObject(s.ifBlank { "{}" })
        val out = LinkedHashMap<String, List<String>>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val a = o.optJSONArray(k) ?: continue
            out[k] = (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() }
        }
        out
    } catch (e: Exception) { emptyMap() }
}
