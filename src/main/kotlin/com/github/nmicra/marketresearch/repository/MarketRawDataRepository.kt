package com.github.nmicra.marketresearch.repository

import com.github.nmicra.marketresearch.entity.GeneralStatistics
import com.github.nmicra.marketresearch.entity.MarketRaw
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MarketRawDataRepository : CoroutineCrudRepository<MarketRaw,Long> {
    suspend fun findAllByLabel(label: String) : Flow<MarketRaw>

        @Query("""
            SELECT T1.*
            FROM market_raw T1
            WHERE T1.label = :providedLabel 
                    AND T1.date = (SELECT max(T2.date)
                           FROM market_raw T2 WHERE T2.label = :providedLabel)
    """)
    suspend fun findLatestAvailableByLabel(@Param("providedLabel") providedLabel : String) : Flow<MarketRaw>
    //@Param("pathLength") pathLength : Int

//    @Query("""
//            SELECT T1
//            FROM GoldRaw T1
//            WHERE T1.date = (SELECT max(T2.date)
//                           FROM GoldRaw T2)
//    """)
//    fun findLatestAvailable() : GoldRaw

}
