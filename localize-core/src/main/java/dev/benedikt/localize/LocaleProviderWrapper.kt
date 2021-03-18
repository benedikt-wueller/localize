package dev.benedikt.localize

import dev.benedikt.localize.api.LocaleProvider

/**
 * @author Benedikt Wüller
 */
data class LocaleProviderWrapper(val locale: String, val localeProvider: LocaleProvider) {

    init {
        this.localeProvider.addRequired()
    }

    protected fun finalize() {
        this.localeProvider.removeRequired()
    }

}
