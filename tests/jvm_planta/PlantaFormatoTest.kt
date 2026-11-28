import io.github.duduferaso.halocalassistant.home.FloorPlan
import io.github.duduferaso.halocalassistant.home.Planta
import org.json.JSONObject
import java.io.File

// Formato da planta (planta.json): validação, ida e volta em JSON, paredes calculadas a partir dos cômodos
// e o FloorPlan em uso montado a partir dela. Usa examples/planta.json (casa genérica).
fun main(a: Array<String>) {
    var falhas = 0
    fun ok(c: Boolean, m: String) { if (!c) falhas++; println((if (c) "PASSOU " else "FALHOU ") + m) }
    val txt = File(a.getOrElse(0) { "../examples/planta.json" }).readText()
    val casa = Planta.fromJson(JSONObject(txt))
    val andar = casa.andares.single()
    ok(andar.comodos.size == 6 && Planta.validar(casa).isEmpty(), "planta de exemplo válida: " + Planta.validar(casa))
    val volta = Planta.toJson(Planta.fromJson(Planta.toJson(casa)))
    ok(volta.toString() == Planta.toJson(casa).toString(), "JSON ida e volta sem perder nada")

    // paredes: integrados sem parede, porta vira vão, contorno fechado fora da porta de entrada
    val paredes = Planta.paredes(andar)
    fun cobre(x1: Double, y1: Double, x2: Double, y2: Double) = paredes.any { w ->
        Math.abs(w[0] - w[2]) < 0.01 == (Math.abs(x1 - x2) < 0.01) &&
        (if (Math.abs(x1 - x2) < 0.01) Math.abs(w[0] - x1) < 0.01 && w[1] <= y1 + 0.01 && w[3] >= y2 - 0.01
         else Math.abs(w[1] - y1) < 0.01 && w[0] <= x1 + 0.01 && w[2] >= x2 - 0.01) }
    ok(!cobre(5.0, 0.5, 5.0, 2.5), "sala e cozinha integradas: sem parede entre elas")
    ok(cobre(0.0, 4.0, 5.0, 4.0), "sala e quarto: parede inteira")
    ok(!cobre(6.0, 3.6, 6.0, 4.2) && cobre(6.0, 4.4, 6.0, 5.0), "porta do banheiro abre só o vão")
    ok(!cobre(1.2, 0.0, 1.8, 0.0) && cobre(2.5, 0.0, 4.5, 0.0), "porta de entrada no contorno")
    ok(cobre(0.0, 0.5, 0.0, 7.5) && cobre(8.0, 0.5, 8.0, 7.5), "contorno lateral fechado")

    // validação
    val c1 = Planta.Comodo("a", "Sala", "sala", listOf(doubleArrayOf(0.0, 0.0, 3.0, 3.0)), emptyList(), null, listOf("light.x"), emptyList(), emptyList(), emptyList(), true)
    val c2 = Planta.Comodo("b", "Quarto", "quarto", listOf(doubleArrayOf(2.0, 0.0, 5.0, 3.0)), emptyList(), null, listOf("light.x"), emptyList(), emptyList(), emptyList(), true)
    val ruim = Planta.validar(Planta.Casa("x", listOf(Planta.Andar("t", "Térreo", listOf(c1, c2), listOf("a" to "z"), emptyList()))))
    ok(ruim.any { "sobrepõem" in it } && ruim.any { "dois cômodos" in it } && ruim.any { "não existe" in it }, "acusa sobreposição, luz repetida e integrado inexistente: $ruim")
    var recusou = false
    try { Planta.fromJson(JSONObject("{\"formato\":99}")) } catch (e: IllegalArgumentException) { recusou = true }
    ok(recusou, "recusa planta de formato mais novo")

    // FloorPlan em uso
    FloorPlan.load(null)
    ok(FloorPlan.isEmpty && FloorPlan.walls.isEmpty(), "sem planta: só o personagem")
    FloorPlan.load(casa)
    ok(FloorPlan.zones.size == 6 && Math.abs(FloorPlan.width - 8f) < 0.01f && Math.abs(FloorPlan.depth - 8f) < 0.01f, "planta carregada: 6 cômodos, 8 × 8 m")
    ok(FloorPlan.walls.size == paredes.size, "paredes do FloorPlan = calculadas (${paredes.size})")
    ok(FloorPlan.spokenNames("quarto") == listOf("quarto", "quarto de casal"), "nomes falados do cômodo")
    ok(FloorPlan.zones.first { it.id == "escritorio" }.keywords.contains("escritorio"), "palavras para achar a luz saem do nome")
    ok(FloorPlan.zoneAt(7f, 4f)?.id == "banheiro", "toque na planta acha o cômodo")
    ok(FloorPlan.zones.first { it.id == "corredor" }.showLabel == false, "corredor sem nome na planta")

    println(if (falhas == 0) "TODOS OS TESTES PASSARAM" else "$falhas FALHA(S)")
}
