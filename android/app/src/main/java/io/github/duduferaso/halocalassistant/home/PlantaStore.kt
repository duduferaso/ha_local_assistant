package io.github.duduferaso.halocalassistant.home

import android.content.Context
import io.github.duduferaso.halocalassistant.domain.JLog
import org.json.JSONObject
import java.io.File

/** Guarda a planta da casa em files/planta.json e mantém o FloorPlan em uso atualizado. */
object PlantaStore {
    private const val FILE = "planta.json"
    @Volatile private var loaded = false

    fun ensureLoaded(context: Context) { if (!loaded) reload(context) }

    fun reload(context: Context) {
        val f = File(context.applicationContext.filesDir, FILE)
        val casa = try {
            if (f.exists()) Planta.fromJson(JSONObject(f.readText())) else null
        } catch (t: Throwable) {
            JLog.e("Planta ilegível; seguindo sem planta", t); null
        }
        FloorPlan.load(casa)
        loaded = true
        JLog.i(if (casa == null) "Sem planta da casa (só o personagem)" else "Planta carregada: ${FloorPlan.zones.size} cômodos")
    }

    /** Grava a planta (depois de validar) e passa a usá-la. Lança com a lista de problemas se for inválida. */
    fun save(context: Context, casa: Planta.Casa) {
        val erros = Planta.validar(casa)
        require(erros.isEmpty()) { erros.joinToString("; ") }
        val f = File(context.applicationContext.filesDir, FILE)
        val tmp = File(f.parentFile, "planta.tmp")
        tmp.writeText(Planta.toJson(casa).toString(1))
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        FloorPlan.load(casa)
        loaded = true
    }

    fun clear(context: Context) {
        File(context.applicationContext.filesDir, FILE).delete()
        FloorPlan.load(null)
    }
}
