package dev.benedikt.localize

import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture

/**
 * @author Benedikt Wüller
 */
object LocalizeService {

    private val localeProviders = mutableMapOf<String, BaseLocaleProvider>()
    private val locales = WeakHashMap<Any, String>()

    var fallbackLocale: String = "en_EN"
        set(value) {
            synchronized(this.localeProviders) {
                this.localeProviders[field]?.uses?.dec()
                field = value
                this.localeProviders[value]?.uses?.inc()
            }
        }

    fun putLocale(locale: String, provider: BaseLocaleProvider) {
        synchronized(this.localeProviders) {
            this.localeProviders[locale]?.let {
                provider.uses = it.uses
                it.clear()
            }
            this.localeProviders[locale] = provider
        }
    }

    fun getLocales(): Array<String> {
        synchronized(this.localeProviders) {
            return this.localeProviders.keys.sorted().toTypedArray()
        }
    }

    fun setLocale(context: Any, locale: String) {
        if (this.getLocale(context) == locale) return

        synchronized(this.localeProviders) {
            // Tell the previous provider to reduce the number of uses and potentially unload the messages.
            this.localeProviders[locale]?.uses?.dec()

            // Set the locale for the given context.
            this.locales[context] = locale

            // Tell the provider to increase the number of uses and potentially load the messages.
            val nextProvider = this.localeProviders[locale] ?: throw IllegalStateException("No provider has been registered for locale $locale.")
            nextProvider.uses += 1
        }
    }

    fun getLocale(context: Any): String {
        return this.locales[context] ?: this.fallbackLocale
    }

    fun translate(context: Any, key: String, vararg params: Any): CompletableFuture<String> {
        val locale = this.getLocale(context)
        val provider = synchronized(this.localeProviders) {
            this.localeProviders[locale] ?: throw IllegalStateException("No provider has been registered for locale $locale.")
        }

        // Try to get the translated message for this key.
        return provider.getString(key).thenCompose {
            if (it == null) {
                // If there is no translated message in this locale, try the fallback locale.
                val fallbackProvider = synchronized(this.localeProviders) {
                    this.localeProviders[this.fallbackLocale] ?: throw IllegalStateException("No provider has been registered for fallback locale $fallbackLocale.")
                }
                // Use the message defined in the fallback locale or the key itself, if no message exists.
                fallbackProvider.getString(key)
            } else {
                CompletableFuture.completedFuture(it)
            }
        }.thenApply { String.format(it ?: key, params) }
    }

}
