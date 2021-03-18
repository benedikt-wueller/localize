package dev.benedikt.localize.api

import java.util.concurrent.CompletableFuture

/**
 * @author Benedikt Wüller
 */
interface LocaleProvider {

    var uses: Int

    fun getString(key: String): CompletableFuture<String?>

    fun clear()

}
