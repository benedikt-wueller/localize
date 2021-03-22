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

abstract class BaseLocaleProvider @JvmOverloads constructor(private val unloadInterval: Long = 60 * 1000L) : LocaleProvider {

    private val executorService = Executors.newFixedThreadPool(1) {
        val thread = Executors.defaultThreadFactory().newThread(it)
        thread.isDaemon = true
        return@newFixedThreadPool thread
    }

    private var isForced = false
    private var requirements = 0

    private var isLoaded = false
    private var unloadJob: Job? = null

    private val strings = mutableMapOf<String, String>()

    /**
     * Determines whether the strings should be loaded at all times (i.e. never be unloaded).
     */
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
        synchronized(this.strings) {
            this.load()
            return this.strings[key.toLowerCase()]
        }
    }

    /**
     * Returns whether strings are currently required and should be loaded.
     */
    private fun isRequired() = this.isForced || this.requirements > 0

    /**
     * Makes sure the strings are loaded asynchronously if required and unloaded if no longer required.
     */
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

    /**
     * Unloads the cached strings.
     */
    private fun unload() {
        synchronized(this.strings) {
            strings.clear()
            isLoaded = false
        }
    }

    /**
     * Loads the strings synchronously.
     * If the strings are not required and scheduled for unloading, this will reset/restart the unload task.
     */
    private fun load() {
        synchronized(this.strings) {
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
    }

    /**
     * Schedules the strings to be unloaded after the given [unloadInterval] ms.
     * If the strings are already schedule to be unloaded, nothing will happen.
     */
    private fun scheduleUnload() {
        if (this.unloadJob != null) return
        this.unloadJob = GlobalScope.launch {
            delay(unloadInterval)
            executorService.submit(::unload)
        }
    }

    /**
     * Loads all strings synchronously.
     */
    protected abstract fun loadStrings(): Map<String, String>

    protected fun finalize() {
        this.unloadJob?.cancel()
        this.unload()
    }

}
