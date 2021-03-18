package dev.benedikt.localize.api

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CompletableFuture

/**
 * @author Benedikt Wüller
 */

abstract class BaseLocaleProvider @JvmOverloads constructor(private val unloadInterval: Long = 2 * 60 * 1000L) : LocaleProvider {

    private val mutex = Mutex()

    private var isForced = false
    private var requirements = 0
    private var isLoaded = false

    private val strings = mutableMapOf<String, String>()

    private var unloadJob: Job? = null

    override fun setForceLoad(force: Boolean) {
        GlobalScope.launch {
            mutex.withLock {
                isForced = force
                updateStrings()
            }
        }
    }

    override fun addRequirement() {
        GlobalScope.launch {
            mutex.withLock {
                if (requirements <= 0) {
                    requirements = 0
                }
                requirements += 1
                updateStrings()
            }
        }
    }

    override fun removeRequirement() {
        GlobalScope.launch {
            mutex.withLock {
                requirements -= 1
                if (requirements <= 0) {
                    requirements = 0
                }
                updateStrings()
            }
        }
    }

    private fun isRequired() = this.isForced || this.requirements > 0

    private fun updateStrings() = GlobalScope.launch {
        mutex.withLock {
            val isRequired = isRequired()
            if (isRequired && isLoaded) return@withLock
            if (!isRequired && !isLoaded) return@withLock

            if (isRequired) {
                load()
            } else {
                scheduleUnload()
            }
        }
    }

    private fun unload() = GlobalScope.launch {
        mutex.withLock {
            strings.clear()
            isLoaded = false
        }
    }

    private fun load() = GlobalScope.launch {
        mutex.withLock {
            if (isLoaded) {
                if (!isRequired()) {
                    // Start the unload countdown from the beginning.
                    unloadJob?.cancel()
                    unloadJob = null
                    scheduleUnload()
                }
                return@withLock
            }

            isLoaded = true
            strings.clear()
            strings.putAll(loadStrings())
        }
    }

    private fun scheduleUnload() {
        if (this.unloadJob != null) return
        this.unloadJob = GlobalScope.launch {
            delay(this@BaseLocaleProvider.unloadInterval)
            mutex.withLock { unload() }
        }
    }

    override fun getString(key: String): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        GlobalScope.launch {
            mutex.withLock {
                load()
                future.complete(strings[key.toLowerCase()])
            }
        }
        return future
    }

    protected abstract fun loadStrings(): Map<String, String>

    protected fun finalize() {
        this.unloadJob?.cancel()
        this.unload()
    }

}
