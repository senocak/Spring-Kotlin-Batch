package com.github.senocak.service

import com.github.senocak.logger
import com.github.senocak.model.TrafficDensity
import org.slf4j.Logger
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component

@Component
class TrafficDensityProcessor: ItemProcessor<TrafficDensity, TrafficDensity> {
    private val log: Logger by logger()
    private val avgSpeedThreshold = 50 // Example threshold

    override fun process(trafficDensity: TrafficDensity): TrafficDensity {
        if (trafficDensity.geohash == "sxk9jr") {
            log.info("About to skip item with geohash=sxk9jr: $trafficDensity")
            throw IllegalArgumentException("Invalid date")
        }
        log.info("Processing traffic density: $trafficDensity")
        if (trafficDensity.averageSpeed < avgSpeedThreshold && trafficDensity.numberOfVehicles > 100) {
            log.warn("Possible congestion detected at ${trafficDensity.geohash}: speed=${trafficDensity.averageSpeed}, vehicles=${trafficDensity.numberOfVehicles}")
        }
        return trafficDensity
    }
}
