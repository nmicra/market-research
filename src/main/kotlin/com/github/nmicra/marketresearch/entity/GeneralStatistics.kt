package com.github.nmicra.marketresearch.entity

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.*


@Table("general_statistics")
data class GeneralStatistics (
    @Id
    var id: Long? = null,
    var key: String = "",
    var label: String = "",

    @JsonSerialize
    @JsonDeserialize
    var value: JsonNode? = null
    )