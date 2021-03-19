package dev.benedikt.localize

import dev.benedikt.localize.api.LocaleProvider

/**
 * This wrapper encapsulates the functionality of automatically registering the provider requirement on construction
 * and unregistering it on garbage collection.
 *
 * @author Benedikt Wüller
 */
data class LocaleProviderWrapper(val locale: String, val localeProvider: LocaleProvider) {

    init {
        this.localeProvider.addRequirement()
    }

    protected fun finalize() {
        this.localeProvider.removeRequirement()
    }

}
