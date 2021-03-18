package dev.benedikt.localize

import dev.benedikt.localize.json.JsonLocaleProvider
import dev.benedikt.localize.yaml.YamlLocaleProvider
import java.nio.file.Paths

/**
 * @author Benedikt Wüller
 */

fun main() {
    LocalizeService.provideLocale("en_EN", YamlLocaleProvider(Paths.get("./localize-example/locales/en_EN.yaml")))
    LocalizeService.provideLocale("de_DE", JsonLocaleProvider(Paths.get("./localize-example/locales/de_DE.json")))

    LocalizeService.fallbackLocale = "en_EN"

    LocalizeService.translate("de_DE", "common.hello", "Bob").thenAccept(::println)
}
