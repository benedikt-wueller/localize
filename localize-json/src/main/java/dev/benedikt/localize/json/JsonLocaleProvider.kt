package dev.benedikt.localize.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import dev.benedikt.localize.api.BaseLocaleProvider
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.bufferedReader
import kotlin.io.path.isDirectory

/**
 * Provides translation strings contained in the given [paths]. The paths can be json files or directories containing json files.
 * The translation keys can be represented using nested objects or a flat object using dot notation for it's keys.
 *
 * @author Benedikt Wüller
 */
open class JsonLocaleProvider(private vararg val paths: Path) : BaseLocaleProvider() {

    protected val gson: Gson = GsonBuilder().create()

    @ExperimentalPathApi
    override fun loadStrings(): Map<String, String> {
        val strings = mutableMapOf<String, String>()

        this.paths.forEach { path ->
            if (!path.isDirectory()) {
                val json = this.gson.fromJson(path.bufferedReader(), JsonObject::class.java)
                strings.putAll(this.loadStrings(json))
                return@forEach
            }

            Files.list(path).forEach {
                val json = this.gson.fromJson(it.bufferedReader(), JsonObject::class.java)
                strings.putAll(this.loadStrings(json))
            }
        }

        return strings
    }

    protected fun loadStrings(json: JsonObject, prefix: String? = null): Map<String, String> {
        val strings = mutableMapOf<String, String>()

        json.keySet().forEach { key ->
            val realKey = if (prefix == null) key else "$prefix.$key"
            val obj = json[key]
            if (obj.isJsonObject) {
                strings.putAll(this.loadStrings(obj.asJsonObject, realKey))
            } else {
                strings[realKey] = obj.asString
            }
        }

        return strings
    }

}
