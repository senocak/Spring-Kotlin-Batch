package com.github.senocak.batch

import com.github.senocak.config.BatchConfig
import com.github.senocak.model.TrafficDensity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.batch.test.context.SpringBatchTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@Import(BatchConfig::class)
class TrafficDensityBatchTest {

    @Autowired
    private lateinit var jobLauncherTestUtils: JobLauncherTestUtils

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `test vehicle count import job`() {
        // Given
        val jobParameters = JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .toJobParameters()

        // When
        val jobExecution = jobLauncherTestUtils.launchJob(jobParameters)

        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.status)

        // Verify data in database
        val results = entityManager.createQuery(
            "SELECT COUNT(v) FROM TrafficDensity v",
            Long::class.java
        ).singleResult

        assertTrue(results > 0, "No records were imported")

        // Verify sample record
        val sampleRecord = entityManager.createQuery(
            "SELECT v FROM TrafficDensity v WHERE v.sensorName = :sensorName AND v.date = :date",
            TrafficDensity::class.java
        )
        .setParameter("sensorName", "TEM Karanfilköy")
        .setParameter("date", java.time.LocalDate.parse("2023-01-01"))
        .resultList
        .firstOrNull()

        assertNotNull(sampleRecord, "Sample record not found")
        assertEquals(93583, sampleRecord?.vehicleCount)
    }
}
