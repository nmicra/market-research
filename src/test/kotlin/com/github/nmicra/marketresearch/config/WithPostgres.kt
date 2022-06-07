package com.github.nmicra.marketresearch.config

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration

abstract class WithPostgres {

    companion object {
        private lateinit var instance: KDockerComposeContainer

        class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

        private fun defineDockerCompose() = KDockerComposeContainer(File("src/test/resources/compose-test.yml"))
            .withExposedService("authdbfortests", 5432)
            .waitingFor("authdbfortests",
                Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10))
            )

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            instance = defineDockerCompose()
            instance.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            instance.stop()
        }
    }
}