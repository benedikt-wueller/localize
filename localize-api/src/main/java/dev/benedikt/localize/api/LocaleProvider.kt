package dev.benedikt.localize.api

import java.util.concurrent.CompletableFuture

/**
 * @author Benedikt Wüller
 */
interface LocaleProvider {

    /**
     * If [force] is `true`, tells the provider to load the strings even if not required.
     * If [force] is `false`, the provider will unload the strings, when they are not required anymore.
     *
     * In general, strings are required if they have been accessed lately or are somehow assigned to an object (context).
     *
     * This method should not be called manually as the [dev.benedikt.localize.LocalizeService] may override it's state.
     */
    fun setForceLoad(force: Boolean)

    /**
     * Tells the provider that one more source is requesting strings in the future.
     * If no sources requested the strings previously and the provider is not forced to load (see [setForceLoad]), they will be loaded.
     *
     * If you call this method manually, make sure to remove the requirement with [removeRequirement] at some point.
     */
    fun addRequirement()

    /**
     * Tells the provider that one less source is requesting strings in the future.
     * If no source requests the strings anymore, they will be unloaded, if the provider is not forced to load (see [setForceLoad]).
     *
     * Only call this method manually, if you previously called [addRequirement] manually as well.
     */
    fun removeRequirement()

    /**
     * Returns a future completing with the string corresponding to the given [key] if it exists.
     * The strings will be loaded, if they are currently unloaded. If they are loaded, the result will be returned immediately.
     */
    fun getString(key: String): CompletableFuture<String?>

    /**
     * Returns the string corresponding to the given [key] if it exists synchronously.
     * The strings will be loaded, if they are currently unloaded.
     */
    fun getStringSync(key: String): String? = this.getString(key).get()

}
