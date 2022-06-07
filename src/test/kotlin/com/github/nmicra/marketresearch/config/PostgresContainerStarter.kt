package com.github.nmicra.marketresearch.config

import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.PostgreSQLContainer

@ContextConfiguration(initializers = [PostgresContainerStarter.Initializer::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class PostgresContainerStarter {

    companion object {
        private val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:13.5").apply {
            withDatabaseName("mrktresearch")
            withUsername("mcadmin")
            withPassword("mcadmin")
        }
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            postgreSQLContainer.start()
            val values = TestPropertyValues.of(
                "spring.datasource.url=${postgreSQLContainer.jdbcUrl}",
                "spring.datasource.username=${postgreSQLContainer.username}",
                "spring.datasource.password=${postgreSQLContainer.password}",
                "spring.liquibase.url=${postgreSQLContainer.jdbcUrl}",
                "spring.liquibase.user=${postgreSQLContainer.username}",
                "spring.liquibase.password=${postgreSQLContainer.password}",
                "spring.r2dbc.url=${postgreSQLContainer.jdbcUrl}",
                "spring.r2dbc.user=${postgreSQLContainer.username}",
                "spring.r2dbc.password=${postgreSQLContainer.password}"
            )
            values.applyTo(configurableApplicationContext)
        }
    }

}
