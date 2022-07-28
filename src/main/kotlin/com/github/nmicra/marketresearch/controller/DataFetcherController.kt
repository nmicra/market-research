package com.github.nmicra.marketresearch.controller

import com.github.nmicra.marketresearch.service.MarketRawDataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class DataFetcherController {

    @Autowired
    lateinit var marketRawDataService : MarketRawDataService


    /**
     * brings & saves the raw data
     * for a given label
     */
    @GetMapping("/raw/setup/{label}")
    fun rawSetup(@PathVariable label : String) : String {
        marketRawDataService.setupNewMarketRawData(label)
        return "done ${System.currentTimeMillis()}"
    }

    @GetMapping("/raw/update/{label}")
    fun rawUpdate(@PathVariable label : String) : String {
        marketRawDataService.updateRawData(label)
        return "done ${System.currentTimeMillis()}"
    }
}