package dev.benedikt.localize

import java.util.concurrent.CompletableFuture

/**
 * @author Benedikt Wüller
 */

/**
 * Translates the given [key] and replacing the provided [params] to the locale assigned to [this] context asynchronously.
 * See [LocalizeService.translateWithContext].
 */
fun Any.translate(key: String, vararg params: Any): CompletableFuture<String> {
    return LocalizeService.translateWithContext(this, key, *params)
}

/**
 * Translates the given [key] and replacing the provided [params] to the locale assigned to [this] context synchronously.
 * See [LocalizeService.translateSyncWithContext].
 */
fun Any.translateSync(key: String, vararg params: Any): String {
    return LocalizeService.translateSyncWithContext(this, key, *params)
}

/**
 * Sets the [locale] for [this] context.
 * See [LocalizeService.setLocale].
 */
fun Any.setLocale(locale: String) {
    LocalizeService.setLocale(this, locale)
}

/**
 * Returns the locale assigned to [this] context or the [LocalizeService.fallbackLocale] if none is set.
 * See [LocalizeService.getLocale].
 */
fun Any.getLocale(): String {
    return LocalizeService.getLocale(this)
}

/**
 * Translates the given [key] and replacing the provided [params] to the provided [locale] asynchronously.
 * See [LocalizeService.translate].
 */
fun trans(locale: String, key: String, vararg params: Any): CompletableFuture<String> {
    return LocalizeService.translate(locale, key, *params)
}

/**
 * Translates the given [key] and replacing the provided [params] to the provided [locale] synchronously.
 * See [LocalizeService.translateSync].
 */
fun transSync(locale: String, key: String, vararg params: Any): String {
    return LocalizeService.translateSync(locale, key, *params)
}
