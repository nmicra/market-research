package com.github.nmicra.marketresearch.service

import com.github.nmicra.marketresearch.analysis.TradingPeriod
import com.github.nmicra.marketresearch.analysis.toTradingMonth
import com.github.nmicra.marketresearch.analysis.toTradingWeeks
import com.github.nmicra.marketresearch.analysis.toTradingYear
import com.github.nmicra.marketresearch.entity.toTradingDay
import com.github.nmicra.marketresearch.repository.MarketRawDataRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class TradingPeriodService(private val marketRawDataRepository: MarketRawDataRepository) {

    suspend fun tradingPeriodByInterval(interval: String, label : String) : List<TradingPeriod> = when (interval) {
        "daily" -> marketRawDataRepository.findAllByLabel(label)
            .map { it.toTradingDay() }
            .toList()
            .sortedBy { it.startingDate }
        "weekly" -> marketRawDataRepository.findAllByLabel(label)
            .map { it.toTradingDay() }
            .toList()
            .sortedBy { it.startingDate }
            .toTradingWeeks()
        "monthly" -> marketRawDataRepository.findAllByLabel(label)
            .map { it.toTradingDay() }
            .toList()
            .sortedBy { it.startingDate }
            .toTradingMonth()
        "yearly" -> marketRawDataRepository.findAllByLabel(label)
            .map { it.toTradingDay() }
            .toList()
            .sortedBy { it.startingDate }
            .toTradingYear()
        else -> error("the provided interval [$interval] is not supported, use one of: daily,weekly,monthly,yearly")
    }
}