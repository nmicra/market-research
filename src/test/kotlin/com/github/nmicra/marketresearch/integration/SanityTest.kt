package com.github.nmicra.marketresearch.integration

import com.github.nmicra.marketresearch.MarketResearchApplication
import com.github.nmicra.marketresearch.config.WithPostgres
import com.github.nmicra.marketresearch.entity.GeneralStatistics
import com.github.nmicra.marketresearch.entity.MarketRaw
import com.github.nmicra.marketresearch.repository.GeneralStatisticsRepository
import com.github.nmicra.marketresearch.repository.MarketRawDataRepository
import com.github.nmicra.marketresearch.service.MarketRawDataService
import com.github.nmicra.marketresearch.utils.objectMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [MarketResearchApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class SanityTest : WithPostgres() {
//class SanityTest : PostgresContainerStarter() {
    @Autowired
    lateinit var generalStatisticsRepository : GeneralStatisticsRepository

    @Autowired
    lateinit var marketRawDataRepository : MarketRawDataRepository

    @Autowired
    lateinit var marketRawDataService : MarketRawDataService

    @Test
    fun saveGeneralStatistics(){
        val tempStatistics = GeneralStatistics(key = "avg", label = "GOLD", value = objectMapper.valueToTree(mapOf("mvAvg5" to "4.2","mvAvg7" to "3.8")) )
        runBlocking {
            generalStatisticsRepository.save(tempStatistics)
            delay(500)
            generalStatisticsRepository.findAllByKey("avg").collect()
            val gs = generalStatisticsRepository.findAllByKey("avg").single()
            println(gs)
        }
    }

    @Test
    fun setupFrog(){
        marketRawDataService.setupNewMarketRawData("frog")
        val frogList : MutableList<MarketRaw> = mutableListOf()
        runBlocking {
            marketRawDataRepository.findAllByLabel("frog").toList(frogList)
        }
        assert(frogList.size > 100)
    }

}