package com.github.senocak.service

import com.github.senocak.logger
import com.github.senocak.model.TrafficDensity
import org.slf4j.Logger
import org.springframework.batch.core.SkipListener
import org.springframework.stereotype.Component

@Component
class TrafficDensitySkipListener(
    private val progressTracker: ProgressTracker
): SkipListener<TrafficDensity, TrafficDensity> {
    private val log: Logger by logger()

    override fun onSkipInProcess(item: TrafficDensity, t: Throwable) {
        log.error("Skipped item: $item due to ${t.message}")
    }
    override fun onSkipInWrite(item: TrafficDensity, t: Throwable) {
        log.warn("Skipped write for item due to ${t.message}: $item")
    }
}