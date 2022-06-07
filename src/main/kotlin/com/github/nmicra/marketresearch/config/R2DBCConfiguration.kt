package com.github.nmicra.marketresearch.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.github.nmicra.marketresearch.utils.objectMapper
import io.r2dbc.postgresql.codec.Json
import io.r2dbc.spi.ConnectionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.PostgresDialect
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.io.IOException


@EnableR2dbcRepositories
@Configuration
@EnableTransactionManagement
class R2DBCConfiguration {

    @Bean
    fun transactionManager(connectionFactory: ConnectionFactory):
            ReactiveTransactionManager {
        return R2dbcTransactionManager(connectionFactory)
    }

    @Bean
    @Override
    fun r2dbcCustomConversions(): R2dbcCustomConversions {
        val converters: MutableList<Converter<*, *>> = ArrayList()
        converters.add(JsonToJsonNodeConverter())
        converters.add(JsonNodeToJsonConverter())
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters)
    }
}

@ReadingConverter
class JsonToJsonNodeConverter() : Converter<Json, JsonNode> {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun convert(json: Json): JsonNode {
        try {
            return objectMapper.readTree(json.asString())
        } catch (e: IOException) {
            logger.error("Problem while parsing JSON: $json", e)
        }
        return objectMapper.createObjectNode()
    }
}

@WritingConverter
class JsonNodeToJsonConverter() :  Converter<JsonNode, Json> {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun convert(source: JsonNode): Json {
        try {
            return Json.of(objectMapper.writeValueAsString(source))
        } catch (e: JsonProcessingException) {
            logger.error("Error occurred while serializing map to JSON: $source", e)
        }
        return Json.of("")
    }
}