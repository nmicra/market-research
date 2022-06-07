package com.github.nmicra.marketresearch.service

import com.github.nmicra.marketresearch.repository.GeneralStatisticsRepository
import com.github.nmicra.marketresearch.repository.MarketRawDataRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MarketRawDataService {

    @Autowired
    lateinit var generalStatisticsRepository : GeneralStatisticsRepository

    @Autowired
    lateinit var marketRawDataRepository : MarketRawDataRepository

    @Autowired
    lateinit var yahooRawDataService : YahooRawDataService


    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun setupNewMarketRawData2(label : String){
        runBlocking {
            yahooRawDataService.tradingDataForPeriod(label, LocalDate.of(1970,1,1), LocalDate.now())
                .asFlow()
                .onEach {  marketRawDataRepository.save(it) }
                .collect()
        }
    }

//    fun setupNewMarketRawData(label : String){
//        val stock = YahooFinance.get(label)
//        val from = Calendar.getInstance().also { it.add(Calendar.YEAR, -50) }
//        val to = Calendar.getInstance()
//       /*stock.getHistory(from, to, Interval.DAILY)
//            .forEach {
//                val raw1 = MarketRaw(label = label,
//                    date = LocalDate.ofInstant(it.date.toInstant(), ZoneId.systemDefault()),
//                    open = it.open.toDouble(),
//                    high = it.high.toDouble(),
//                    low = it.low.toDouble(),
//                    close = it.close.toDouble(),
//                    volume = it.volume
//                )
//                runBlocking { marketRawDataRepository.save(raw1) }
//                println(">>> ${it}")
//            }*/
//        runBlocking {
//            stock.getHistory(from, to, Interval.DAILY)
//                .asFlow().onEach {
//                    val raw1 = MarketRaw(label = label,
//                        date = LocalDate.ofInstant(it.date.toInstant(), ZoneId.systemDefault()),
//                        open = it.open.toDouble(),
//                        high = it.high.toDouble(),
//                        low = it.low.toDouble(),
//                        close = it.close.toDouble(),
//                        volume = it.volume
//                    )
//                    marketRawDataRepository.save(raw1)
//                }.collect()
//        }
//    }

    fun updateRawData(label : String) {
        runBlocking {

            val latestDate = marketRawDataRepository.findLatestAvailableByLabel(label).single().date
            yahooRawDataService.tradingDataForPeriod(label, LocalDate.now().minusMonths(3), LocalDate.now())
                .asFlow()
                .filter { it.date.isAfter(latestDate) }
                .onEach {  marketRawDataRepository.save(it) }
                .collect()
        }

    }
}