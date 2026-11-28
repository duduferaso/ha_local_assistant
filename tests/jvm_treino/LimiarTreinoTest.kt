import io.github.duduferaso.halocalassistant.wakeword.PersonalTrainer
import io.github.duduferaso.halocalassistant.wakeword.TemplateSet
import java.util.Random

// Limiar automático do treino de voz (lição da 1.15.6: treinando com TV o limiar ia a ~0,22, 0,01 abaixo
// da TV, e o app passou a ativar sozinho). Regras: nunca acima de 0,20 e sempre 0,03 abaixo da fala/TV
// mais parecida, mesmo abaixo de 0,14 (piso absoluto de 0,08). Sinais sintéticos, para rodar no CI sem gravações de ninguém.
fun main() {
    var falhas = 0
    fun ok(c: Boolean, m: String) { if (!c) falhas++; println((if (c) "PASSOU " else "FALHOU ") + m) }
    val rnd = Random(7)
    fun unit(v: FloatArray): FloatArray { val n = Math.sqrt(v.sumOf { (it * it).toDouble() }).toFloat(); return FloatArray(v.size) { v[it] / n } }
    val base = Array(60) { unit(FloatArray(40) { rnd.nextGaussian().toFloat() }) }
    fun variante(ruido: Float) = Array(base.size) { i -> unit(FloatArray(40) { base[i][it] + ruido * rnd.nextGaussian().toFloat() }) }
    fun outro() = Array(60) { unit(FloatArray(40) { rnd.nextGaussian().toFloat() }) }

    for ((nome, ruidoChamada, negativos) in listOf(
        Triple("chamadas parecidas, TV bem diferente", 0.15f, List(20) { outro() }),
        Triple("chamadas espalhadas (longe), TV diferente", 0.6f, List(20) { outro() }),
        Triple("TV parecida com a chamada", 0.05f, List(20) { variante(0.16f) }),
        Triple("TV quase igual à chamada", 0.05f, List(20) { variante(0.11f) }))) {
        val set = TemplateSet(MutableList(8) { variante(ruidoChamada) }, TemplateSet.DEFAULT_THRESHOLD)
        val r = PersonalTrainer.choose(set, emptyList(), negativos)
        val seguro = maxOf(PersonalTrainer.MIN_AUTO, r.bestNegative - PersonalTrainer.MARGIN)
        ok(r.threshold <= PersonalTrainer.MAX_AUTO + 1e-6f && r.threshold <= seguro + 1e-6f && r.threshold >= PersonalTrainer.MIN_AUTO - 1e-6f &&
            (r.bestNegative < PersonalTrainer.MIN_AUTO + PersonalTrainer.MARGIN || r.falseHits == 0),
            "%s: limiar %.3f (TV mais parecida %.3f, pior chamada %.3f)".format(nome, r.threshold, r.bestNegative, r.worstPositive))
    }
    // o caso que quebrou a 1.15.6: TV a ~0,22 → o limiar não pode passar de 0,19
    val setTv = TemplateSet(MutableList(8) { variante(0.05f) }, TemplateSet.DEFAULT_THRESHOLD)
    val tv = List(20) { variante(0.23f) }
    val r = PersonalTrainer.choose(setTv, emptyList(), tv)
    println("   (TV do caso real: mais parecida %.3f → limiar %.3f)".format(r.bestNegative, r.threshold))
    ok(r.bestNegative > 0.16f && r.bestNegative < 0.26f, "cenário montado com a TV entre 0,16 e 0,26")
    ok(r.threshold <= r.bestNegative - 0.03f + 1e-6f, "folga de 0,03 abaixo da TV")
    ok(r.falseHits == 0, "nenhuma fala da TV abaixo do limiar")
    println(if (falhas == 0) "TODOS OS TESTES PASSARAM" else "$falhas FALHA(S)")
}
