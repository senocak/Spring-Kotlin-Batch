package com.github.senocak.service

import com.github.senocak.logger
import org.slf4j.Logger
import org.springframework.batch.core.ChunkListener
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.stereotype.Component

@Component
class ChunkProgressListener(
    private val tracker: ProgressTracker
): ChunkListener {
    private val log: Logger by logger()

    override fun afterChunk(context: ChunkContext) {
        val stepExecution: StepExecution = context.stepContext.stepExecution
        val jobId: String = stepExecution.jobExecution.jobParameters.getString("JobID")
            ?: stepExecution.jobExecution.id.toString()
        log.info("Chunk completed: ${context.isComplete}. StepExecution: $stepExecution")
        tracker.updateProgress(jobId = jobId) {
            totalRead = stepExecution.readCount
            totalWritten = stepExecution.writeCount
            skipCount = stepExecution.skipCount
            lastUpdate = System.currentTimeMillis()
        }
        log.info("Chunk completed for job $jobId: ${tracker.getProgress(jobId).totalWritten} items written")
    }

    override fun afterChunkError(context: ChunkContext) {
        val stepExecution: StepExecution = context.stepContext.stepExecution
        val jobId: String = stepExecution.jobExecution.jobParameters.getString("JobID")
            ?: stepExecution.jobExecution.id.toString()

        // Check if this is a skip rather than an error
        if (context.getAttribute("skip") != null || stepExecution.skipCount > 0) {
            log.info("Skip occurred in chunk for job $jobId, continuing processing")
        } else {
            log.warn("Error in chunk for job $jobId, StepExecution: $stepExecution")
        }
        // Update progress even in case of error to maintain accurate counts
        tracker.updateProgress(jobId = jobId) {
            totalRead = stepExecution.readCount
            totalWritten = stepExecution.writeCount
            skipCount = stepExecution.skipCount
            lastUpdate = System.currentTimeMillis()
        }
    }
}
