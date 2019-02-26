package com.huma.infinispan

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.Logger.slf4jLogger
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.ext.installKoin

fun Application.main() {

    installKoin {
        slf4jLogger()
        fileProperties()
        environmentProperties()
        modules(myModule)
    }

    val service: DoSomeStuffService by inject()

    routing {
        get("/{reference}") {
            val refId: String = call.parameters["reference"]!!
            val response = service.doSomething(refId)
            call.respond(response)
        }
        get("/status") {
            call.respond(service.status())
        }
    }
}

val myModule = module {
    single(createdAtStart = true) { DoSomeStuffService() }
}

fun main(args: Array<String>) {
    embeddedServer(Netty, commandLineEnvironment(args)).start(true)
}
