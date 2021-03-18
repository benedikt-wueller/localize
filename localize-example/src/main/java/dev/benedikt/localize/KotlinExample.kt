package dev.benedikt.localize

import dev.benedikt.localize.json.JsonLocaleProvider
import dev.benedikt.localize.yaml.YamlLocaleProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

/**
 * @author Benedikt Wüller
 */

fun main() {
    LocalizeService.provideLocale("en_EN", YamlLocaleProvider(Paths.get("./localize-example/locales/en_EN.yaml")))
    LocalizeService.provideLocale("de_DE", JsonLocaleProvider(Paths.get("./localize-example/locales/de_DE.json")))

    LocalizeService.fallbackLocale = "en_EN"

    runBlocking {
        repeat(100) {
            delay(100)
            GlobalScope.launch {
                val time = System.currentTimeMillis()
                LocalizeService.translate("de_DE", "common.hello", "Bob")
                    .thenAccept(::println)
                    .thenAccept { println("delta: " + (System.currentTimeMillis() - time)) }
            }
        }
    }

    runBlocking {
        delay(15000)
    }
}
