package com.github.senocak.controller

import com.github.senocak.logger
import com.github.senocak.service.ProgressTracker
import org.slf4j.Logger
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.JobOperator
import org.springframework.core.io.FileSystemResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api/batch/traffic-density")
class BatchController(
    private val jobLauncher: JobLauncher,
    private val importVehicleCountJob: Job,
    private val jobExplorer: JobExplorer,
    private val jobOperator: JobOperator,
    private val tracker: ProgressTracker,
) {
    private val log: Logger by logger()
    private val sdf = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss")

    @PostMapping("/download")
    fun download(@RequestParam url: String = "https://data.ibb.gov.tr/dataset/3ee6d744-5da2-40c8-9cd6-0e3e41f1928f/resource/76671ebe-2fd2-426f-b85a-e3772263f483/download/traffic_density_202412.csv"): String {
        // Download file asynchronously to see progress bar
        val destinationPath = "traffic_density_${sdf.format(Timestamp(System.currentTimeMillis()))}.csv"
        CompletableFuture.runAsync {
            downloadFileWithProgress(fileUrl = url, destinationPath = destinationPath)
        }
        return destinationPath
    }
    private fun downloadFileWithProgress(fileUrl: String, destinationPath: String) {
        try {
            val url = URL(fileUrl)
            Channels.newChannel(url.openStream()).use { readableByteChannel: ReadableByteChannel ->
                FileOutputStream(destinationPath).use { fileOutputStream: FileOutputStream ->
                    val fileSize: Long = url.openConnection().contentLengthLong
                    var bytesTransferred = 0L
                    val buffer = ByteArray(10 * 1024) // 10KB buffer

                    while (true) {
                        val bytesRead: Int = readableByteChannel.read(java.nio.ByteBuffer.wrap(buffer))
                        if (bytesRead <= 0) break

                        fileOutputStream.write(buffer, 0, bytesRead)
                        bytesTransferred += bytesRead

                        if (fileSize > 0) {
                            val percentage: Double = (bytesTransferred * 100.0) / fileSize
                            printProgress(bytesTransferred = bytesTransferred, totalSize = fileSize, percentage = percentage)
                        } else {
                            print("\rDownloaded: $bytesTransferred bytes")
                        }
                    }
                    log.info("Download completed successfully!")
                }
            }
        } catch (e: IOException) {
            log.error("Error during download: ${e.message}")
            throw e
        } catch (e: Exception) {
            log.error("Failed to download file: ${e.message}")
        }
    }
    private fun printProgress(bytesTransferred: Long, totalSize: Long, percentage: Double) {
        val width = 50
        val progress: Int = (percentage / 100 * width).toInt()
        val bar: String = buildString {
            append("[")
            repeat(progress) { append("=") }
            repeat(width - progress) { append(" ") }
            append("]")
        }
        println("$bar %6.2f%% ($bytesTransferred / $totalSize bytes)")
        System.out.flush()
    }

    @PostMapping("/run")
    fun run(@RequestParam csvName: String): String {
        log.info("Starting batch job with file: $csvName")
        val csvFile = FileSystemResource(csvName)
        if (!csvFile.exists()) {
            val error = "CSV file not found: $csvName"
            log.error(error)
            throw IllegalArgumentException(error)
        }
        val params: JobParameters = JobParametersBuilder()
            .addString("filePath", csvFile.file.absolutePath)
            .addString("JobID", System.currentTimeMillis().toString())
            .toJobParameters()
        try {
            tracker.reset() // TODO: make it specific for each job
            val jobExecution: JobExecution = jobLauncher.run(importVehicleCountJob, params)
            log.info("Job completed with status: ${jobExecution.status}")
            return "Batch job completed with status: ${jobExecution.status}"
        } catch (e: Exception) {
            log.error("Error running batch job: ${e.message}", e)
            throw e
        }
    }

    @GetMapping("/progress")
    fun getProgress(): ProgressTracker = tracker

    @GetMapping("/jobs")
    fun getAllJobExecutions(): List<Map<String, Any>> {
        return jobExplorer.jobNames.flatMap { jobName: String ->
            jobExplorer.getJobInstances(jobName, 0, Int.MAX_VALUE)
                .flatMap { jobInstance: JobInstance ->
                    jobExplorer.getJobExecutions(jobInstance).map { execution: JobExecution ->
                        mapOf(
                            "jobExecutionId" to execution.id,
                            "jobName" to execution.jobInstance.jobName,
                            "status" to execution.status.toString(),
                            "startTime" to (execution.startTime?.format(ISO_DATE_TIME) ?: ""),
                            "endTime" to (execution.endTime?.format(ISO_DATE_TIME) ?: ""),
                            "exitStatus" to execution.exitStatus.exitCode,
                            "stepExecutions" to execution.stepExecutions.map { stepExecution: StepExecution ->
                                mapOf(
                                    "stepName" to stepExecution.stepName,
                                    "status" to stepExecution.status.toString(),
                                    "readCount" to stepExecution.readCount,
                                    "writeCount" to stepExecution.writeCount,
                                    "commitCount" to stepExecution.commitCount,
                                    "rollbackCount" to stepExecution.rollbackCount
                                )
                            }
                        )
                    }
                }
        }
    }

    @GetMapping("/jobs/running")
    fun getRunningExecutions(): List<Map<String, Any>> =
        jobExplorer.findRunningJobExecutions(importVehicleCountJob.name).map { execution: JobExecution ->
            mapOf(
                "jobExecutionId" to execution.id,
                "jobName" to execution.jobInstance.jobName,
                "status" to execution.status.toString(),
                "startTime" to (execution.startTime?.format(ISO_DATE_TIME) ?: "")
            )
        }

    @PostMapping("/jobs/{executionId}/stop")
    fun stopJob(@PathVariable executionId: Long): Map<String, String> {
        val jobExecution: JobExecution = jobExplorer.getJobExecution(executionId)
            ?: throw IllegalArgumentException("Job execution not found with id: $executionId")
        return when {
            jobExecution.isRunning -> {
                jobExecution.status = org.springframework.batch.core.BatchStatus.STOPPING
                jobOperator.stop(executionId)
                mapOf(pair = "message" to "Job stop requested successfully")
            }
            else -> {
                mapOf(pair = "message" to "Job is not running")
            }
        }
    }
}
