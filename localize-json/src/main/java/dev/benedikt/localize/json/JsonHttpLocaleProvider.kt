package dev.benedikt.localize.json

import com.google.gson.JsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.stream.Collectors
import kotlin.io.path.ExperimentalPathApi

/**
 * @author Benedikt Wüller
 */
class JsonHttpLocaleProvider(private vararg val urls: String) : JsonLocaleProvider() {

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

            val json = this.gson.fromJson(response, JsonObject::class.java)
            strings.putAll(this.loadStrings(json))
        }
        return strings
    }

}
