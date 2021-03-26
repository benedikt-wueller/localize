package dev.benedikt.localize

import dev.benedikt.localize.api.LocaleProvider
import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture

/**
 * @author Benedikt Wüller
 */
object LocalizeService {

    private val localeProviders = mutableMapOf<String, LocaleProvider>()
    private val coreLocales = mutableSetOf<String>()
    private val locales = WeakHashMap<Any, LocaleProviderWrapper>()

    /**
     * Determines the fallback locale to use if no matching translation can be found for the requested locale or if no locale has been set for
     * context-based translations.
     *
     * The corresponding [LocaleProvider] has to be provided (see [provideLocale]) before setting the fallback locale.
     *
     * When set, the corresponding [LocaleProvider] will be forced to load the strings at all times (see [LocaleProvider.setForceLoad]).
     */
    var fallbackLocale: String? = null
        set(value) {
            if (field == value) return
            synchronized(this.localeProviders) {
                field?.let { this.getLocaleProvider(it) }?.setForceLoad(this.coreLocales.contains(field!!))
                value?.let { this.getLocaleProvider(it).setForceLoad(true) }
            }
            field = value
        }

    /**
     * Registers a [LocaleProvider] to use for the given [locale].
     * If another [LocaleProvider] has already been registered, it will be replaced.
     */
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

    /**
     * If [isCore] is set to `true`, this locale will be held in memory at all times unless [isCore] is set to `false` in the future.
     */
    @JvmOverloads
    fun setCoreLocale(locale: String, isCore: Boolean = true) {
        synchronized(this.localeProviders) {
            if (isCore) {
                this.coreLocales.add(locale)
                this.getLocaleProvider(locale).setForceLoad(true)
            } else {
                this.coreLocales.remove(locale)
                this.getLocaleProvider(locale).setForceLoad(this.fallbackLocale == locale)
            }
        }
    }

    /**
     * Returns all locales available.
     */
    fun getLocales(): Array<String> {
        synchronized(this.localeProviders) {
            return this.localeProviders.keys.sorted().toTypedArray()
        }
    }

    /**
     * Sets the [locale] for the given [context] (weak reference).
     * If the [context] is garbage collected, the locale assignment will be removed as well.
     */
    fun setLocale(context: Any, locale: String) {
        synchronized(this.locales) {
            this.locales[context] = LocaleProviderWrapper(locale, this.getLocaleProvider(locale))
        }
    }

    /**
     * Returns the locale assigned to this [context] or the [fallbackLocale] if none has been set.
     * @throws [IllegalStateException] if no locale has been set and the [fallbackLocale] is not set.
     */
    fun getLocale(context: Any): String {
        synchronized(this.locales) {
            return this.locales[context]?.locale ?: this.fallbackLocale ?: throw IllegalStateException("No locale has been assigned and no fallback locale has been supplied.")
        }
    }

    /**
     * Translates the given [key] and replacing the provided [params] to the locale assigned to the [context] asynchronously.
     */
    fun translateWithContext(context: Any, key: String, vararg params: Any): CompletableFuture<String> {
        val locale = this.getLocale(context)
        return this.translate(locale, key, params)
    }

    /**
     * Translates the given [key] and replacing the provided [params] to the locale assigned to the [context] synchronously.
     */
    fun translateSyncWithContext(context: Any, key: String, vararg params: Any): String {
        val locale = this.getLocale(context)
        return this.translateSync(locale, key, params)
    }

    /**
     * Translates the given [key] and replacing the provided [params] to the provided [locale] asynchronously.
     */
    fun translate(locale: String, key: String, vararg params: Any): CompletableFuture<String> {
        return this.getFormatSync(locale, key).thenApply {
            // Replace placeholders with parameters.
            String.format(it, *params)
        }
    }

    /**
     * Translates the given [key] and replacing the provided [params] to the provided [locale] synchronously.
     */
    fun translateSync(locale: String, key: String, vararg params: Any): String = String.format(this.getFormat(locale, key), *params)

    /**
     * Translates the given [key] without replacing parameters to the provided [locale] synchronously.
     */
    fun getFormat(locale: String, key: String): String = this.getStringSync(locale, key).replace(Regex("\\{(\\d+)}"), "%$1\\\$s")

    /**
     * Translates the given [key] without replacing parameters to the provided [locale] asynchronously.
     */
    fun getFormatSync(locale: String, key: String): CompletableFuture<String> {
        return this.getString(locale, key).thenApply {
            // Replace {1}, {2}, ... with %1$s, %2$s, ...
            it.replace(Regex("\\{(\\d+)}"), "%$1\\\$s")
        }
    }

    /**
     * Loads the required translation string assigned to the [key] for the given [locale] asynchronously.
     */
    private fun getString(locale: String, key: String): CompletableFuture<String> {
        val provider = this.getLocaleProvider(locale)

        return provider.getString(key).thenCompose {
            val fallbackLocale = this.fallbackLocale
            if (it == null && fallbackLocale != null) {
                val fallbackProvider = this.getLocaleProvider(fallbackLocale)
                fallbackProvider.getString(key)
            } else {
                CompletableFuture.completedFuture(it)
            }
        }.thenApply { it ?: key }
    }

    /**
     * Loads the required translation string assigned to the [key] for the given [locale] synchronously.
     */
    private fun getStringSync(locale: String, key: String): String {
        val provider = this.getLocaleProvider(locale)
        return provider.getStringSync(key) ?: this.fallbackLocale?.let {
            val fallbackProvider = this.getLocaleProvider(it)
            fallbackProvider.getStringSync(key)
        } ?: key
    }

    /**
     * Returns the [LocaleProvider] registered for the given [locale] or `null` if no [LocaleProvider] has been registered.
     */
    private fun findLocaleProvider(locale: String): LocaleProvider? {
        synchronized(this.localeProviders) {
            return this.localeProviders[locale]
        }
    }

    /**
     * Returns the [LocaleProvider] registered for the given [locale].
     * @throws [IllegalStateException] if no [LocaleProvider] has been registered.
     */
    private fun getLocaleProvider(locale: String): LocaleProvider {
        synchronized(this.localeProviders) {
            return this.localeProviders[locale] ?: throw IllegalStateException("No provider has been registered for locale $locale.")
        }
    }

}
