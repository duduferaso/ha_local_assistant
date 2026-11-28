import io.github.duduferaso.halocalassistant.home.FloorPlan
import io.github.duduferaso.halocalassistant.home.Planta

/**
 * Planta genérica usada pelos testes (não é a casa de ninguém): sala de estar, sala de jantar em L,
 * cozinha, corredor sem luz, banheiro e dois quartos, com os nomes falados comuns em português.
 *
 *   x: 0      2      4      7  8
 *   y0 ┌──────────────┬──────┬─┐
 *      │    estar     │jantar  │   jantar = [4,0,7,3] + [7,0,8,1.5]
 *    4 ├──────┬───────┤ ─ ─ ┌─┘
 *      │banho │corred.│cozinha│   cozinha = [4,3,8,6]
 *    6 ├──────┴───────┼───────┤
 *      │   quarto1    │quarto2│
 *    9 └──────────────┴───────┘
 */
object PlantaTeste {
    private fun c(id: String, nome: String, tipo: String, partes: List<DoubleArray>, falados: List<String>,
                  palavras: List<String>, evitar: List<String> = emptyList(), mostrar: Boolean = true) =
        Planta.Comodo(id, nome, tipo, partes, partes.map { doubleArrayOf((it[0] + it[2]) / 2, (it[1] + it[3]) / 2) }, null,
            emptyList(), falados, palavras, evitar, mostrar)

    val casa = Planta.Casa("Casa de teste", listOf(Planta.Andar("terreo", "Térreo", listOf(
        c("estar", "Sala de estar", "sala", listOf(doubleArrayOf(0.0, 0.0, 4.0, 4.0)),
            listOf("sala de estar", "estar", "sala estar"), listOf("estar", "sala", "living", "tv"), listOf("jantar", "mesa")),
        c("jantar", "Jantar", "sala", listOf(doubleArrayOf(4.0, 0.0, 7.0, 3.0), doubleArrayOf(7.0, 0.0, 8.0, 1.5)),
            listOf("sala de jantar", "jantar"), listOf("jantar", "mesa")),
        c("cozinha", "Cozinha", "cozinha", listOf(doubleArrayOf(4.0, 3.0, 8.0, 6.0)),
            listOf("cozinha", "área de serviço", "serviço", "lavanderia", "cozinha e serviço"), listOf("cozinha", "servico", "lavanderia", "area")),
        c("corredor", "Corredor", "corredor", listOf(doubleArrayOf(2.0, 4.0, 4.0, 6.0)),
            emptyList(), listOf("corredor", "hall", "circulacao"), mostrar = false),
        c("banheiro", "Banheiro", "banheiro", listOf(doubleArrayOf(0.0, 4.0, 2.0, 6.0)),
            listOf("banheiro", "banho", "lavabo", "wc"), listOf("banheiro", "banho", "wc", "lavabo")),
        c("quarto1", "Quarto 1", "quarto", listOf(doubleArrayOf(0.0, 6.0, 4.0, 9.0)),
            listOf("quarto de casal", "quarto 1", "quarto um", "quarto do casal", "suíte", "quarto principal"),
            listOf("quarto 1", "suite", "casal", "principal", "master")),
        c("quarto2", "Quarto 2", "quarto", listOf(doubleArrayOf(4.0, 6.0, 8.0, 9.0)),
            listOf("quarto de visita", "quarto 2", "quarto dois", "quarto de visitas", "quarto das visitas", "quarto visita",
                "quarto de hóspede", "quarto de hóspedes", "quarto dos hóspedes", "visita", "visitas", "hóspedes", "escritório"),
            listOf("quarto 2", "visita", "hospede", "escritorio", "bebe", "filho", "filha", "solteiro"))
    ), listOf("estar" to "jantar"), emptyList())))

    fun carregar() = FloorPlan.load(casa)
}
