package dev.benedikt.localize.api

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * @author Benedikt Wüller
 */

abstract class BaseLocaleProvider @JvmOverloads constructor(private val unloadInterval: Long = 2000L) : LocaleProvider {

    private val executorService = Executors.newSingleThreadExecutor()

    private var isForced = false
    private var requirements = 0

    private var isLoaded = false
    private var unloadJob: Job? = null

    private val strings = mutableMapOf<String, String>()

    override fun setForceLoad(force: Boolean) {
        this.executorService.submit {
            isForced = force
            updateStrings()
        }
    }

    override fun addRequirement() {
        this.executorService.submit {
            if (requirements <= 0) {
                requirements = 0
            }
            requirements += 1
            updateStrings()
        }
    }

    override fun removeRequirement() {
        this.executorService.submit {
            requirements -= 1
            if (requirements <= 0) {
                requirements = 0
            }
            updateStrings()
        }
    }

    override fun getString(key: String): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        this.executorService.submit {
            future.complete(this.getStringSync(key))
        }
        return future
    }

    override fun getStringSync(key: String): String? {
        this.load()
        return this.strings[key.toLowerCase()]
    }

    private fun isRequired() = this.isForced || this.requirements > 0

    private fun updateStrings() {
        this.executorService.submit {
            val isRequired = isRequired()
            if (isRequired && isLoaded) return@submit
            if (!isRequired && !isLoaded) return@submit

            if (isRequired) {
                load()
            } else {
                scheduleUnload()
            }
        }
    }

    private fun unload() {
        strings.clear()
        isLoaded = false
    }

    private fun load() {
        if (isLoaded) {
            if (!isRequired()) {
                // Start the unload countdown from the beginning.
                unloadJob?.cancel()
                unloadJob = null
                scheduleUnload()
            }
            return
        }

        isLoaded = true
        strings.clear()
        strings.putAll(loadStrings())
    }

    private fun scheduleUnload() {
        if (this.unloadJob != null) return
        this.unloadJob = GlobalScope.launch {
            delay(unloadInterval)
            executorService.submit(::unload)
        }
    }

    protected abstract fun loadStrings(): Map<String, String>

    protected fun finalize() {
        this.unloadJob?.cancel()
        this.unload()
    }

}
