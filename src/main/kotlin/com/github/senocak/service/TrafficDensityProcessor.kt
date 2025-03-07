package com.github.senocak.service

import com.github.senocak.model.TrafficDensity
import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import kotlin.Exception
import kotlin.Throws

@Component
class TrafficDensityProcessor: ItemProcessor<TrafficDensity, TrafficDensity> {
    @Throws(Exception::class)
    override fun process(trafficDensity: TrafficDensity): TrafficDensity {
        return trafficDensity
    }
}
