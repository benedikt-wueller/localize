package dev.benedikt.localize.api

import java.lang.RuntimeException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * @author Benedikt Wüller
 */

abstract class BaseLocaleProvider : LocaleProvider {

    private val executorService = Executors.newSingleThreadExecutor()

    private var isForced = false
    private var requirements = 0
    private var isLoaded = false

    private val strings = mutableMapOf<String, String>()

    override fun setForceLoad(force: Boolean) {
        executorService.submit {
            this.isForced = force
            this.updateStrings()
        }
    }

    override fun addRequired() {
        executorService.submit {
            if (this.requirements <= 0) {
                this.requirements = 0
            }
            this.requirements += 1
            this.updateStrings()
        }
    }

    override fun removeRequired() {
        executorService.submit {
            this.requirements -= 1
            if (this.requirements <= 0) {
                this.requirements = 0
            }
            this.updateStrings()
        }
    }

    private fun updateStrings() {
        val isRequired = this.isForced || this.requirements > 0
        if (isRequired && this.isLoaded) return
        if (!isRequired && !this.isLoaded) return

        if (isRequired) {
            this.load()
        } else {
            this.unload()
        }
    }

    private fun unload() {
        this.strings.clear()
        this.isLoaded = false
    }

    private fun load() {
        try {
            this.unload()
            this.strings.putAll(this.loadStrings())
            this.isLoaded = true
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun getString(key: String): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        this.executorService.submit {
            if (!this.isLoaded) {
                // TODO: unload after some time, if not required by then.
                this.load()
            }

            future.complete(this.strings[key.toLowerCase()])
        }
        return future
    }

    protected abstract fun loadStrings(): Map<String, String>

}
