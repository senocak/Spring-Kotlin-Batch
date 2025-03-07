package com.github.senocak.service

import com.github.senocak.logger
import org.slf4j.Logger
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.StepExecutionListener
import org.springframework.stereotype.Component

@Component
class JobStepExecutionListener: StepExecutionListener {
    private val log: Logger by logger()

    override fun beforeStep(stepExecution: StepExecution) {
        log.info("Step execution started. stepExecution: $stepExecution")
    }

    override fun afterStep(stepExecution: StepExecution): ExitStatus {
        log.info("Step execution finished. stepExecution: $stepExecution")
        return ExitStatus.COMPLETED
    }
}