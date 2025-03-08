package com.github.senocak.service

import com.github.senocak.logger
import com.github.senocak.model.TrafficDensity
import org.slf4j.Logger
import org.springframework.batch.core.JobExecution
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

    private fun getJobId(): String {
        // Access the current job execution context through Spring's ExecutionContext
        val jobExecution: JobExecution = org.springframework.batch.core.scope.context.JobSynchronizationManager.getContext()?.jobExecution
            ?: throw IllegalStateException("No job execution context found")
        return jobExecution.jobParameters.getString("JobID")
            ?: throw IllegalStateException("JobID parameter is missing")
    }

    override fun onSkipInProcess(item: TrafficDensity, t: Throwable) {
        log.error("Skip Listener - onSkipInProcess called for item: $item, Exception: ${t.message}", t)
        val jobId: String = getJobId()
        progressTracker.updateProgress(jobId = jobId) {
            totalRead++
        }
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
        val jobId: String = getJobId()
        progressTracker.updateProgress(jobId = jobId) {
            totalWritten--
        }
        try {
            val chunk = Chunk<TrafficDensity>()
            chunk.add(item)
            skippedItemsWriter.write(chunk)
        } catch (e: Exception) {
            log.error("Failed to write skipped item to CSV: ${e.message}", e)
        }
    }
}
