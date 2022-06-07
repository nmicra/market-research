package com.github.nmicra.marketresearch.repository

import com.github.nmicra.marketresearch.entity.GeneralStatistics
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GeneralStatisticsRepository : CoroutineCrudRepository<GeneralStatistics,Long> {
    suspend fun findAllByLabel(label: String) : Flow<GeneralStatistics>
    suspend fun findAllByKey(key: String) : Flow<GeneralStatistics>
    suspend fun findAllByKeyAndLabel(key: String,label: String) : Flow<GeneralStatistics>
}
