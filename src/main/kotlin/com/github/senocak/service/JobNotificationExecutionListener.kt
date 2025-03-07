package com.github.senocak.service

import com.github.senocak.logger
import org.slf4j.Logger
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component
import java.util.Date

@Component
class JobNotificationExecutionListener: JobExecutionListener {
    private val log: Logger by logger()
    private var start: Long = 0

    override fun beforeJob(jobExecution: JobExecution) {
        start = System.currentTimeMillis()
        log.info("Job with id ${jobExecution.jobId} is about to start at ${Date()}")
    }

    override fun afterJob(jobExecution: JobExecution) {
        log.info("Job completed at ${Date()}, execution time in mills ${(System.currentTimeMillis() - start)}, status ${jobExecution.status}")
    }
}
