package com.github.senocak.service

import com.github.senocak.logger
import com.github.senocak.model.TrafficDensity
import org.slf4j.Logger
import org.springframework.batch.core.SkipListener
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.support.SynchronizedItemStreamWriter
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class TrafficDensitySkipListener(
    private val progressTracker: ProgressTracker,
    @Lazy private val skippedItemsWriter: SynchronizedItemStreamWriter<TrafficDensity>
): SkipListener<TrafficDensity, TrafficDensity> {
    private val log: Logger by logger()

    override fun onSkipInRead(t: Throwable) {
        log.error("Skip Listener - onSkipInRead called with exception: ${t.message}", t)
    }

    override fun onSkipInProcess(item: TrafficDensity, t: Throwable) {
        log.error("Skip Listener - onSkipInProcess called for item: $item, Exception: ${t.message}", t)
        progressTracker.totalRead++
        try {
            val chunk = Chunk<TrafficDensity>()
            chunk.add(item)
            skippedItemsWriter.write(chunk)
        } catch (e: Exception) {
            log.error("Failed to write skipped item to CSV: ${e.message}", e)
        }
    }

    override fun onSkipInWrite(item: TrafficDensity, t: Throwable) {
        log.error("Skip Listener - onSkipInWrite called for item: $item, Exception: ${t.message}")
        progressTracker.totalWritten--
        try {
            val chunk = Chunk<TrafficDensity>()
            chunk.add(item)
            skippedItemsWriter.write(chunk)
        } catch (e: Exception) {
            log.error("Failed to write skipped item to CSV: ${e.message}", e)
        }
    }
}
