package com.github.senocak.batch

import com.github.senocak.config.BatchConfig
import com.github.senocak.model.TrafficDensity
import com.github.senocak.service.TrafficDensitySkipListener
import com.github.senocak.logger
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@Import(BatchConfig::class)
class TrafficDensityBatchTest {

    @Autowired
    private lateinit var jobLauncherTestUtils: JobLauncherTestUtils

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var trafficDensitySkipListener: TrafficDensitySkipListener

    private val log: org.slf4j.Logger by logger()
    private val skippedItemsFile = java.io.File("skipped_traffic_density.csv")

    @BeforeEach
    fun setup() {
        if (skippedItemsFile.exists()) {
            skippedItemsFile.delete()
        }
    }

    @AfterEach
    fun cleanup() {
        if (skippedItemsFile.exists()) {
            skippedItemsFile.delete()
        }
    }

    @Test
    fun `test traffic density import with skip`() {
        log.info("[DEBUG_LOG] Starting skip test")
        // Given
        val testFile = javaClass.classLoader.getResource("test_traffic_density.csv")?.file
            ?: throw IllegalStateException("Test file not found")

        log.info("Using test file: $testFile")

        val jobParameters = JobParametersBuilder()
            .addString("filePath", testFile)
            .addLong("time", System.currentTimeMillis())
            .toJobParameters()

        // When
        val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        // Then
        log.info("[DEBUG_LOG] Job completed with status: ${jobExecution.status}")
        assertEquals(BatchStatus.COMPLETED, jobExecution.status)

        // Get the step execution
        val stepExecution = jobExecution.stepExecutions.first()
        log.info("[DEBUG_LOG] Step execution details:")
        log.info("[DEBUG_LOG] Read count: ${stepExecution.readCount}")
        log.info("[DEBUG_LOG] Write count: ${stepExecution.writeCount}")
        log.info("[DEBUG_LOG] Process skip count: ${stepExecution.processSkipCount}")
        log.info("[DEBUG_LOG] Read skip count: ${stepExecution.readSkipCount}")
        log.info("[DEBUG_LOG] Write skip count: ${stepExecution.writeSkipCount}")
        log.info("[DEBUG_LOG] Commit count: ${stepExecution.commitCount}")
        log.info("[DEBUG_LOG] Rollback count: ${stepExecution.rollbackCount}")

        // Verify skip counts
        assertEquals(1, stepExecution.processSkipCount, "Expected exactly one process skip")
        assertEquals(0, stepExecution.readSkipCount, "Expected no read skips")
        assertEquals(0, stepExecution.writeSkipCount, "Expected no write skips")
        assertEquals(3, stepExecution.readCount, "Expected to read 3 items")
        assertEquals(2, stepExecution.writeCount, "Expected to write 2 items (3 read - 1 skipped)")

        // Verify data in database
        val results = entityManager.createQuery(
            "SELECT COUNT(v) FROM TrafficDensity v",
            Long::class.java
        ).singleResult

        // We expect 2 records (3 in file - 1 skipped)
        assertEquals(2L, results, "Expected 2 records after skipping one")

        // Verify non-skipped record exists
        val sampleRecord = entityManager.createQuery(
            "SELECT v FROM TrafficDensity v WHERE v.geohash = :geohash",
            TrafficDensity::class.java
        )
        .setParameter("geohash", "txk9jr")
        .resultList
        .firstOrNull()

        assertNotNull(sampleRecord, "Sample record not found")
        assertEquals(45, sampleRecord?.averageSpeed)
        assertEquals(120, sampleRecord?.numberOfVehicles)

        // Verify skipped record doesn't exist
        val skippedRecord = entityManager.createQuery(
            "SELECT COUNT(v) FROM TrafficDensity v WHERE v.geohash = :geohash",
            Long::class.java
        )
        .setParameter("geohash", "sxk9jr")
        .singleResult

        assertEquals(0L, skippedRecord, "Skipped record should not exist in database")

        // Verify skipped items file
        assertTrue(skippedItemsFile.exists(), "Skipped items file should exist")
        val skippedFileContent = skippedItemsFile.readLines()

        // Verify header
        assertTrue(skippedFileContent.isNotEmpty(), "Skipped items file should not be empty")
        assertEquals("DATE_TIME,LATITUDE,LONGITUDE,GEOHASH,MINIMUM_SPEED,MAXIMUM_SPEED,AVERAGE_SPEED,NUMBER_OF_VEHICLES",
            skippedFileContent[0], "Header should match expected format")

        // Verify skipped record content
        assertTrue(skippedFileContent.size > 1, "Should have at least one skipped record")
        assertTrue(skippedFileContent.any { it.contains("sxk9jr") }, "Should contain the skipped record with geohash sxk9jr")

        // Cleanup
        skippedItemsFile.delete()
    }
}
