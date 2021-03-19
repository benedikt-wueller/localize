package dev.benedikt.localize

import java.util.concurrent.CompletableFuture

/**
 * @author Benedikt Wüller
 */

fun Any.translate(key: String, vararg params: Any): CompletableFuture<String> {
    return LocalizeService.translateWithContext(this, key, *params)
}

fun Any.translateSync(key: String, vararg params: Any): String {
    return LocalizeService.translateSyncWithContext(this, key, *params)
}

fun Any.setLocale(locale: String) {
    LocalizeService.setLocale(this, locale)
}

fun Any.getLocale(): String {
    return LocalizeService.getLocale(this)
}

fun trans(locale: String, key: String, vararg params: Any): CompletableFuture<String> {
    return LocalizeService.translate(locale, key, *params)
}

fun transSync(locale: String, key: String, vararg params: Any): String {
    return LocalizeService.translateSync(locale, key, *params)
}
