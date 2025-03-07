package com.github.senocak.controller

import com.github.senocak.logger
import org.slf4j.Logger
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.text.SimpleDateFormat

@RestController
@RequestMapping("/api/batch/traffic-density")
class BatchController(
    private val restTemplate: RestTemplate,
    private val jobLauncher: JobLauncher,
    private val importVehicleCountJob: Job,
    private val jobExplorer: JobExplorer,
    @Qualifier("applicationTaskExecutor") private val asyncTaskExecutor: AsyncTaskExecutor,
) {
    private val log: Logger by logger()
    private val sdf = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss")

    @PostMapping("/download")
    fun download(@RequestParam url: String = "https://data.ibb.gov.tr/dataset/3ee6d744-5da2-40c8-9cd6-0e3e41f1928f/resource/76671ebe-2fd2-426f-b85a-e3772263f483/download/traffic_density_202412.csv"): String {
        // simulate the user downloading a CSV file of contacts from this controller endpoint
        asyncTaskExecutor.execute {
            downloadFileToResourcesWithProgress(url, "traffic_density_${sdf.format(Timestamp(System.currentTimeMillis()))}.csv")
        }
        return "File downloading started"
    }
    fun downloadFileToResourcesWithProgress(url: String, fileName: String) {
        val resourcesDir = File("src/main/resources")
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs()
        }

        val outputFile = File(resourcesDir, fileName)
        log.info("Downloading file to: ${outputFile.absolutePath}")

        val headers = HttpHeaders().apply {
            accept = listOf(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
        }

        val entity = HttpEntity<Void>(headers)

        val response: ResponseEntity<Resource> = try {
            restTemplate.exchange(url, HttpMethod.GET, entity, Resource::class.java)
        } catch (ex: Exception) {
            log.error("Failed to initiate file download from $url", ex)
            throw ex
        }

        if (!response.statusCode.is2xxSuccessful) {
            log.error("Received non-success response: ${response.statusCode}")
            throw RuntimeException("Failed to download file, status code: ${response.statusCode}")
        }

        val contentLength = response.headers.contentLength
        log.info("Connection established. File size: ${formatSize(contentLength)}")

        val resource = response.body ?: throw RuntimeException("No file received from $url")

        resource.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                downloadWithProgress(input, output, contentLength)
            }
        }

        log.info("\nFile successfully downloaded to ${outputFile.absolutePath}")
    }

    private fun downloadWithProgress(input: java.io.InputStream, output: FileOutputStream, totalBytes: Long) {
        val buffer = ByteArray(8192)
        var bytesRead: Long = 0
        var lastLoggedProgress = -1

        while (true) {
            val read = input.read(buffer)
            if (read == -1) break

            output.write(buffer, 0, read)
            bytesRead += read

            if (totalBytes > 0) {
                val progress = (bytesRead * 100 / totalBytes).toInt()
                if (progress != lastLoggedProgress) {
                    logProgressBar(progress)
                    lastLoggedProgress = progress
                }
            }
        }
    }

    private fun logProgressBar(progress: Int) {
        val barLength = 30
        val filledLength = (progress * barLength / 100.0).toInt()
        val bar: String = "█".repeat(filledLength) + "-".repeat(barLength - filledLength)
        log.info("Downloading: [$bar] $progress%")
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown size"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return "%.2f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    @PostMapping("/run")
    fun run(): String {
        // simulate the user uploading a CSV file of contacts to this controller endpoint
        val csvFile = ClassPathResource("traffic_density_202412.csv")
        val params: JobParameters = JobParametersBuilder()
            .addString("filePath", csvFile.file.absolutePath)
            .addString("JobID", System.currentTimeMillis().toString())
            .toJobParameters()
        jobLauncher.run(importVehicleCountJob, params)
        return "Batch job has been invoked"
    }
}