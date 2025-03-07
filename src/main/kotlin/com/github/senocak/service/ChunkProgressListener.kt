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
        log.info("Chunk completed: ${context.isComplete}. stepExecution: $stepExecution")
        tracker.totalRead = stepExecution.readCount
        tracker.totalWritten = stepExecution.writeCount
        tracker.skipCount = stepExecution.skipCount
        tracker.lastUpdate = System.currentTimeMillis()
        log.info("Chunk completed: ${tracker.totalWritten} items written")
    }

    override fun afterChunkError(context: ChunkContext) {
        log.warn("Error in chunk: ${context.stepContext.stepExecution}")
    }
}