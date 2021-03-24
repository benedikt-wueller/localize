package dev.benedikt.localize.yaml

import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.stream.Collectors
import kotlin.io.path.ExperimentalPathApi

/**
 * @author Benedikt Wüller
 */
class YamlHttpLocaleProvider(private vararg val urls: String) : YamlLocaleProvider() {

    @ExperimentalPathApi
    override fun loadStrings(): Map<String, String> {
        val strings = mutableMapOf<String, String>()
        this.urls.forEach {
            val url = URL(it)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val response: String
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                response = reader.lines().collect(Collectors.joining("\n"))
            }

            val yaml = Yaml()
            val result = yaml.load<Map<String, Any>>(response)
            strings.putAll(this.loadStrings(result))
        }
        return strings
    }

}
