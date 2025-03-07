package com.github.senocak.controller

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.core.io.ClassPathResource
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/batch")
class BatchController(
    private val jobLauncher: JobLauncher,
    private val importVehicleCountJob: Job,
    private val jobExplorer: JobExplorer
) {
    @PostMapping("/traffic-density")
    fun importVehicleCounts(): String {
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