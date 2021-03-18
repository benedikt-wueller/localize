package dev.benedikt.localize

import dev.benedikt.localize.api.LocaleProvider

/**
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
