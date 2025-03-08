package com.github.senocak.controller

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.web.bind.annotation.PathVariable
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import com.github.senocak.service.ProgressTracker
import org.springframework.batch.core.Job

@ExtendWith(MockitoExtension::class)
class BatchControllerTest {

    @Mock
    private lateinit var jobLauncher: JobLauncher

    @Mock
    private lateinit var importVehicleCountJob: Job

    @Mock
    private lateinit var jobExplorer: JobExplorer

    @Mock
    private lateinit var tracker: ProgressTracker

    @Mock
    private lateinit var jobOperator: org.springframework.batch.core.launch.JobOperator

    @InjectMocks
    private lateinit var batchController: BatchController

    @Test
    fun `should get all job executions`() {
        // Given
        val jobInstance = JobInstance(1L, "testJob")
        val jobParameters = JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters()
        val jobExecution = JobExecution(1L, jobParameters)
        jobExecution.jobInstance = jobInstance
        jobExecution.status = BatchStatus.STARTING

        `when`(jobExplorer.jobNames).thenReturn(listOf("testJob"))
        `when`(jobExplorer.getJobInstances("testJob", 0, Int.MAX_VALUE)).thenReturn(listOf(jobInstance))
        `when`(jobExplorer.getJobExecutions(jobInstance)).thenReturn(listOf(jobExecution))

        // When
        val result = batchController.getAllJobExecutions()

        // Then
        assertThat(result).isNotEmpty
        assertThat(result[0]["jobName"]).isEqualTo("testJob")
        assertThat(result[0]["status"]).isEqualTo("STARTING")
    }

    @Test
    fun `should get running executions`() {
        // Given
        val jobInstance = JobInstance(1L, "testJob")
        val jobParameters = JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters()
        val jobExecution = JobExecution(1L, jobParameters)
        jobExecution.jobInstance = jobInstance
        jobExecution.status = BatchStatus.STARTED

        `when`(importVehicleCountJob.name).thenReturn("importVehicleCountJob")
        `when`(jobExplorer.findRunningJobExecutions("importVehicleCountJob")).thenReturn(setOf(jobExecution))

        // When
        val result = batchController.getRunningExecutions()

        // Then
        assertThat(result).isNotEmpty
        assertThat(result[0]["status"]).isEqualTo("STARTED")
    }

    @Test
    fun `should stop job execution`() {
        // Given
        val jobInstance = JobInstance(1L, "testJob")
        val jobParameters = JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters()
        val jobExecution = JobExecution(1L, jobParameters)
        jobExecution.jobInstance = jobInstance
        jobExecution.status = BatchStatus.STARTED

        `when`(jobExplorer.getJobExecution(1L)).thenReturn(jobExecution)
        `when`(jobOperator.stop(1L)).thenReturn(true)

        // When
        val result = batchController.stopJob(1L)

        // Then
        assertThat(result["message"]).isEqualTo("Job stop requested successfully")
        assertThat(jobExecution.status).isEqualTo(BatchStatus.STOPPING)
        org.mockito.Mockito.verify(jobOperator).stop(1L)
    }

    @Test
    fun `should get job-specific progress`() {
        // Given
        val jobId = "test-job-123"
        val jobProgress = ProgressTracker.JobProgress(
            totalRead = 100,
            totalWritten = 90,
            skipCount = 10,
            lastUpdate = System.currentTimeMillis()
        )
        `when`(tracker.getProgress(jobId)).thenReturn(jobProgress)

        // When
        val result = batchController.getProgress(jobId)

        // Then
        assertThat(result).isEqualTo(jobProgress)
        assertThat(result.totalRead).isEqualTo(100)
        assertThat(result.totalWritten).isEqualTo(90)
        assertThat(result.skipCount).isEqualTo(10)
    }

    @Test
    fun `should run job with job-specific tracking`() {
        // Given
        val testFile = javaClass.classLoader.getResource("test_traffic_density.csv")?.file
            ?: throw IllegalStateException("Test file not found")
        val jobExecution = JobExecution(1L)
        jobExecution.status = BatchStatus.COMPLETED

        `when`(jobLauncher.run(org.mockito.ArgumentMatchers.eq(importVehicleCountJob),
            org.mockito.ArgumentMatchers.argThat { params: JobParameters ->
                params.getString("JobID") != null && params.getString("filePath") != null
            }
        )).thenReturn(jobExecution)

        // When
        val result = batchController.run(testFile)

        // Then
        assertThat(result).contains("COMPLETED")
        org.mockito.Mockito.verify(tracker).reset(org.mockito.ArgumentMatchers.anyString())
    }
}
