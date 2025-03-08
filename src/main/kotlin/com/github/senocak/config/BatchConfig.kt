package com.github.senocak.config

import com.github.senocak.model.TrafficDensity
import com.github.senocak.service.TrafficDensityProcessor
import com.github.senocak.service.JobNotificationExecutionListener
import com.github.senocak.service.TrafficDensitySkipListener
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.ChunkListener
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.StepExecutionListener
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.support.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.skip.SkipPolicy
import org.springframework.batch.item.database.JpaItemWriter
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.batch.item.support.SynchronizedItemStreamWriter
import org.springframework.batch.item.support.builder.SynchronizedItemStreamWriterBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.client.RestTemplate

@Configuration
//@EnableBatchProcessing // Spring Boot 3.x, this annotation disables auto-configuration, including schema initialization.
class BatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val trafficDensityProcessor: TrafficDensityProcessor,
    private val jobNotificationExecutionListener: JobNotificationExecutionListener,
    private val chunkListener: ChunkListener,
    private val trafficDensitySkipListener: TrafficDensitySkipListener,
    private val stepExecutionListener: StepExecutionListener
) {
    @Bean
    fun writer(): JpaItemWriter<TrafficDensity> =
        JpaItemWriterBuilder<TrafficDensity>()
            .entityManagerFactory(entityManagerFactory)
            .build()

    @Bean
    @StepScope
    fun reader(@Value("#{jobParameters['filePath']}") path: String?): FlatFileItemReader<TrafficDensity> =
        FlatFileItemReaderBuilder<TrafficDensity>()
            .name("trafficDensityReader")
            .resource(FileSystemResource(path ?: "traffic_density_202412.csv"))
            .linesToSkip(1) // skip header row
            .delimited()
            .delimiter(",")
            .names("DATE_TIME","LATITUDE","LONGITUDE","GEOHASH","MINIMUM_SPEED","MAXIMUM_SPEED","AVERAGE_SPEED","NUMBER_OF_VEHICLES")
            .fieldSetMapper { fieldSet: FieldSet ->
                TrafficDensity(
                    dateTime = fieldSet.readString("DATE_TIME"),
                    latitude = fieldSet.readString("LATITUDE"),
                    longitude = fieldSet.readString("LONGITUDE"),
                    geohash = fieldSet.readString("GEOHASH"),
                    minimumSpeed = fieldSet.readInt("MINIMUM_SPEED"),
                    maximumSpeed = fieldSet.readInt("MAXIMUM_SPEED"),
                    averageSpeed = fieldSet.readInt("AVERAGE_SPEED"),
                    numberOfVehicles = fieldSet.readInt("NUMBER_OF_VEHICLES"),
                )
            }
            //.targetType(VehicleCount::class.java) // no need to set this, it's inferred from the fieldSetMapper
            //.lineMapper(VehicleCountLineMapper())
            .build()

//    @Bean
//    fun multiResourceReader(@Value("#{jobParameters['filePath']}") path: String?): MultiResourceItemReader<TrafficDensity> =
//        MultiResourceItemReaderBuilder<TrafficDensity>()
//            .name("multiTrafficDensityReader")
//            .resources(
//                FileSystemResource(path ?: "traffic_density_202412.csv"),
//                FileSystemResource("traffic_density_202411.csv")
//            )
//            .delegate(reader(null))
//            .build()

    @Bean
    fun skipPolicy(): SkipPolicy = SkipPolicy { t: Throwable, skipCount: Long ->
        skipCount < 1_000 // Allow up to 1000 skips
        // or true // Always skip on exception, customize as needed
    }

    @Bean
    fun importTrafficDensityStep(): Step =
        StepBuilder("importTrafficDensityStep", jobRepository)
            .chunk<TrafficDensity, TrafficDensity>(10_000, transactionManager)
            .reader(reader(null)) // null path just for type resolution
            //.reader(multiResourceReader(path = null)) // for multiple files
            .processor(trafficDensityProcessor) // if you move it down, skip listener won't work
            .writer(writer())
            .faultTolerant()
            .skip(Exception::class.java)
            //.skipLimit(1000) // Allow up to 1000 skips
            .skipPolicy(skipPolicy())
            .processorNonTransactional()
            .listener(chunkListener) // Add the chunk listener here
            .listener(trafficDensitySkipListener as org.springframework.batch.core.SkipListener<TrafficDensity, TrafficDensity>)
            .listener(stepExecutionListener)
            .stream(skippedItemsWriter(filePath = "skipped_traffic_density.csv")) // Write skipped items to a file
            .build()

    @Bean
    fun importTrafficDensityJob(): Job =
        JobBuilder("importTrafficDensityJob", jobRepository)
            .incrementer(RunIdIncrementer()) // Optional: Ensures unique job runs
            .start(importTrafficDensityStep())
            .listener(jobNotificationExecutionListener)
            .build()

    @Bean
    fun skippedItemsWriter(@Value("\${spring.batch.skipped-items-file:skipped_traffic_density.csv}") filePath: String): SynchronizedItemStreamWriter<TrafficDensity> =
        SynchronizedItemStreamWriterBuilder<TrafficDensity>()
            .delegate(
                FlatFileItemWriterBuilder<TrafficDensity>()
                    .name("skippedItemsWriter")
                    .resource(FileSystemResource(filePath))
                    .delimited()
                    .delimiter(",")
                    .names("dateTime", "latitude", "longitude", "geohash", "minimumSpeed", "maximumSpeed", "averageSpeed", "numberOfVehicles")
                    .headerCallback { writer -> writer.write("DATE_TIME,LATITUDE,LONGITUDE,GEOHASH,MINIMUM_SPEED,MAXIMUM_SPEED,AVERAGE_SPEED,NUMBER_OF_VEHICLES") }
                    .build()
            )
            .build()

    @Bean
    fun restTemplate(): RestTemplate {
        val factory = org.springframework.http.client.SimpleClientHttpRequestFactory()
        factory.setChunkSize(8192)
        factory.setConnectTimeout(java.time.Duration.ofMinutes(5))
        factory.setReadTimeout(java.time.Duration.ofMinutes(5))
        return RestTemplate(factory)
    }

    @Bean
    fun taskExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 5
            maxPoolSize = 10
            queueCapacity = 25
            initialize()
        }
}
