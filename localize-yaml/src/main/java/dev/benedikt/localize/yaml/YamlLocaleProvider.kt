package dev.benedikt.localize.yaml

import dev.benedikt.localize.api.BaseLocaleProvider
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author Benedikt Wüller
 */
class YamlLocaleProvider(private val path: Path) : BaseLocaleProvider() {

    override fun loadStrings(): Map<String, String> {
        val yaml = Yaml()
        val result = yaml.load<Map<String, Any>>(Files.newBufferedReader(this.path))
        return this.loadStrings(result)
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
