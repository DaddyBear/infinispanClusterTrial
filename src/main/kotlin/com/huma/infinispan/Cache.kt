package com.huma.infinispan

import org.infinispan.commons.CacheConfigurationException
import org.infinispan.configuration.cache.CacheMode
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.configuration.global.GlobalConfigurationBuilder
import org.infinispan.manager.DefaultCacheManager
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

typealias CacheKey = String

class Cache<V : Any>(private val cacheName: String) {

    private val log = LoggerFactory.getLogger(this::class.simpleName)

    companion object {
        private var cacheManager = DefaultCacheManager(
            GlobalConfigurationBuilder()
                .transport().defaultTransport()
                .clusterName("My-Infinispan-Cluster")
                .addProperty("configurationFile", "jgroups.xml")
                .build()
        )
    }

    private val backingCache = findOrBuildCache()

    fun get(key: CacheKey) = this.backingCache.advancedCache[key]

    suspend fun get(key: CacheKey, resolver: suspend () -> V?): V {
        return get(key) ?: (resolver() ?: throw IllegalStateException("Unable to resolve caching value for $key"))
    }

    /**
     * Add a value into the cache with defined lifespan.
     *
     * @param key The key to the entry item
     * @param value The entry item
     * @param lifespan The lifespan the item shall be kept in cache
     * @param timeUnit The time unit to the lifespan
     */
    fun put(key: CacheKey, value: V, lifespan: Long = 5, timeUnit: TimeUnit = TimeUnit.MINUTES) {
        this.backingCache.put(key, value, lifespan, timeUnit)
    }

    private fun findOrBuildCache(): org.infinispan.Cache<CacheKey, V> {
        val searchResult = searchCacheByName()
        if (searchResult != null) return searchResult

        return try {
            val cacheConfiguration = ConfigurationBuilder()
                .clustering().cacheMode(CacheMode.REPL_ASYNC)
                .build()
            cacheManager.defineConfiguration(cacheName, cacheConfiguration)
            log.info("Started cache with name $cacheName. Found cluster members are ${cacheManager.clusterMembers}")
            cacheManager.getCache()
        } catch (e: Exception) {
            log.error("Failure on build up cache '$cacheName'. Use basic cache w/o cluster", e)
            DefaultCacheManager(ConfigurationBuilder().build()).getCache()
        }
    }

    /**
     * Searching for a existing cache managed by cacheManager with the given name.
     *
     * @param searchedCacheName The name of the searched cache
     */
    private fun searchCacheByName(searchedCacheName: String = this.cacheName): org.infinispan.Cache<CacheKey, V>? {
        return try {
            val existingCache = cacheManager.getCache<CacheKey, V>(searchedCacheName)
            if (existingCache != null) {
                log.debug("Searched cache '$cacheName' found, return existing one")
            }
            existingCache
        } catch (e: CacheConfigurationException) {
            log.debug("Searched cache '$cacheName' doesn't exist, create a new one")
            null
        }
    }

    /**
     * @return The current status of the cache
     */
    fun status(): String {
        return "Status of cache '${backingCache.name}' (${cacheManager.nodeAddress}): ${cacheManager.cacheManagerStatus}.\n" +
                "Found cluster members: ${cacheManager.clusterMembers}\n" +
                "Running cache count: ${cacheManager.runningCacheCount}\n" +
                "Number of cached elements: ${backingCache.size}\n" +
                "Inner state: ${backingCache.status}"
    }

}
