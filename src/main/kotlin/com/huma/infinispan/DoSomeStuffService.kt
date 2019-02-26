package com.huma.infinispan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class DoSomeStuffService {

    private val cache = Cache<String>("MyInfinispanCache")

    suspend fun doSomething(refId: String): String {
        return cache.get(refId, resolver = {
            withContext(Dispatchers.Default) {
                delay(560)
                val result = "$refId-Solved"
                cache.put(refId, "$result-Cache")
                result
            }
        })
    }

    fun status(): String {
        return cache.status()
    }

}
