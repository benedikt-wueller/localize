package dev.benedikt.localize

import dev.benedikt.localize.api.LocaleProvider
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * @author Benedikt Wüller
 */

abstract class BaseLocaleProvider : LocaleProvider {

    private val executorService = Executors.newSingleThreadExecutor()

    private val messages = mutableMapOf<String, String>()

    override var uses = 0
        set(value) {
            synchronized(this.uses) {
                field = this.processUses(field, value)
            }
        }

    private fun processUses(field: Int, value: Int): Int {
        if (field <= 0 && value > 0) {
            this.load()
            return value
        }

        if (field > 0 && value <= 0) {
            this.unload()
            return 0
        }

        return value
    }

    override fun getString(key: String): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        this.executorService.submit {
            synchronized(this.messages) {
                future.complete(this.messages[key])
            }
        }
        return future
    }

    override fun clear() {
        this.uses = 0
    }

    private fun load() {
        this.executorService.submit {
            synchronized(this.messages) {
                this.messages.clear()
                this.messages.putAll(this.loadStrings())
            }
        }
    }

    private fun unload() {
        this.executorService.submit {
            synchronized(this.messages) {
                this.messages.clear()
            }
        }
    }

    protected abstract fun loadStrings(): Map<String, String>

}
