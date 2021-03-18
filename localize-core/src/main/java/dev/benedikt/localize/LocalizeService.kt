package dev.benedikt.localize

import dev.benedikt.localize.api.LocaleProvider
import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture

/**
 * @author Benedikt Wüller
 */
object LocalizeService {

    private val localeProviders = mutableMapOf<String, LocaleProvider>()
    private val locales = WeakHashMap<Any, LocaleProviderWrapper>()

    var fallbackLocale: String? = null
        set(value) {
            if (field == value) return
            synchronized(this.localeProviders) {
                field?.let { this.getLocaleProvider(it) }?.setForceLoad(false)
                value?.let { this.getLocaleProvider(it).setForceLoad(true) }
            }
            field = value
        }

    fun provideLocale(locale: String, provider: LocaleProvider) {
        synchronized(this.localeProviders) {
            val currentProvider = this.findLocaleProvider(locale)
            if (provider == currentProvider) return

            this.localeProviders[locale] = provider

            synchronized(this.locales) {
                if (currentProvider != null) {
                    val affectedKeys = this.locales.filter { it.value.locale == locale }.map { it.key }
                    affectedKeys.forEach { key -> this.setLocale(key, locale) }
                }
            }
        }
    }

    fun getLocales(): Array<String> {
        synchronized(this.localeProviders) {
            return this.localeProviders.keys.sorted().toTypedArray()
        }
    }

    fun setLocale(context: Any, locale: String) {
        synchronized(this.locales) {
            this.locales[context] = LocaleProviderWrapper(locale, this.getLocaleProvider(locale))
        }
    }

    fun getLocale(context: Any): String {
        synchronized(this.locales) {
            return this.locales[context]?.locale ?: this.fallbackLocale ?: throw IllegalStateException("No locale has been assigned and no fallback locale has been supplied.")
        }
    }

    fun translateWithContext(context: Any, key: String, vararg params: Any): CompletableFuture<String> {
        val locale = this.getLocale(context)
        return this.translate(locale, key, params)
    }

    fun translate(locale: String, key: String, vararg params: Any): CompletableFuture<String> {
        return this.getString(locale, key).thenApply {
            // Replace {1}, {2}, ... with %1$s, %2$s, ...
            it.replace(Regex("\\{(\\d+)}"), "%$1\\\$s")
        }.thenApply {
            // Replace placeholders with parameters.
            String.format(it, *params)
        }
    }

    fun translateSync(locale: String, key: String, vararg params: Any): String {
        return this.translate(locale, key, *params).get()
    }

    private fun getString(locale: String, key: String): CompletableFuture<String> {
        val provider = this.getLocaleProvider(locale)

        return provider.getString(key).thenCompose {
            if (it == null) {
                val fallbackProvider = this.getLocaleProvider(this.fallbackLocale ?: throw IllegalStateException("No fallback locale has been supplied."))
                fallbackProvider.getString(key)
            } else {
                CompletableFuture.completedFuture(it)
            }
        }.thenApply { it ?: key }
    }

    private fun findLocaleProvider(locale: String): LocaleProvider? {
        synchronized(this.localeProviders) {
            return this.localeProviders[locale]
        }
    }

    private fun getLocaleProvider(locale: String): LocaleProvider {
        synchronized(this.localeProviders) {
            return this.localeProviders[locale] ?: throw IllegalStateException("No provider has been registered for locale $locale.")
        }
    }

}
