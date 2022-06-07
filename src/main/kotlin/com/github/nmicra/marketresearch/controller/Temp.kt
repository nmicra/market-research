package com.github.nmicra.marketresearch.controller

import com.github.nmicra.marketresearch.entity.GeneralStatistics
import com.github.nmicra.marketresearch.repository.GeneralStatisticsRepository
import com.github.nmicra.marketresearch.utils.objectMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
class Temp {

    @Autowired
    lateinit var generalStatisticsRepository : GeneralStatisticsRepository

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("temp")
    suspend fun temp() : String {
       generalStatisticsRepository.save(GeneralStatistics(key = "avg", label = "GOLD", value = objectMapper.valueToTree(mapOf("mvAvg5" to "4.2","mvAvg7" to "3.8")) ))
        return runBlocking {
            delay(500)
            val gs = generalStatisticsRepository.findAllByKey("avg1").first()
             gs.toString()
        }
    }
}