package dev.benedikt.localize.yaml

import dev.benedikt.localize.api.BaseLocaleProvider
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isDirectory

/**
 * Provides translation strings contained in the given [paths]. The paths can be yaml files or directories containing yaml files.
 * The translation keys can be represented using nested objects or a flat object using dot notation for it's keys.
 *
 * @author Benedikt Wüller
 */
class YamlLocaleProvider(private vararg val paths: Path) : BaseLocaleProvider() {

    @ExperimentalPathApi
    override fun loadStrings(): Map<String, String> {
        val strings = mutableMapOf<String, String>()

        this.paths.forEach { path ->
            if (!path.isDirectory()) {
                val yaml = Yaml()
                val result = yaml.load<Map<String, Any>>(Files.newBufferedReader(path))
                strings.putAll(this.loadStrings(result))
                return@forEach
            }

            Files.list(path).forEach {
                val yaml = Yaml()
                val result = yaml.load<Map<String, Any>>(Files.newBufferedReader(it))
                strings.putAll(this.loadStrings(result))
            }
        }

        return strings
    }

    private fun loadStrings(map: Map<String, Any>, prefix: String? = null): Map<String, String> {
        val strings = mutableMapOf<String, String>()

        map.forEach { (key, value) ->
            val realKey = if (prefix == null) key else "$prefix.$key"
            if (value is Map<*, *>) {
                strings.putAll(this.loadStrings(value as Map<String, Any>, realKey))
            } else {
                strings[realKey] = value.toString()
            }
        }

        return strings
    }

}
