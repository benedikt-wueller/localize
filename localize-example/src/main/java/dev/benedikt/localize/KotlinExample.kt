package dev.benedikt.localize

import dev.benedikt.localize.json.JsonLocaleProvider
import dev.benedikt.localize.yaml.YamlLocaleProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

/**
 * @author Benedikt Wüller
 */

fun main() {
    LocalizeService.provideLocale("en_EN", YamlLocaleProvider(Paths.get("./localize-example/locales/en_EN.yaml")))
    LocalizeService.provideLocale("de_DE", JsonLocaleProvider(Paths.get("./localize-example/locales/de_DE")))

    LocalizeService.fallbackLocale = "en_EN"

    runBlocking {
        println(transSync("de_DE", "common.hello2", "Bob", "Alice"))

        val amount = 500_000
        val time = System.nanoTime()

        repeat(amount) {
            println(transSync("de_DE", "common.hello", "Bob"))
        }

        val delta = System.nanoTime() - time
        val deltaMilliseconds = (delta / 10_000.0 / amount).toInt() / 100.0
        println("$deltaMilliseconds ms per translation at $amount translations")
    }

    runBlocking {
        delay(15000)
    }
}
