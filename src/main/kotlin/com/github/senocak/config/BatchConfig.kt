package com.github.senocak.config

import com.github.senocak.model.TrafficDensity
import com.github.senocak.service.TrafficDensityProcessor
import com.github.senocak.service.JobCompletionNotificationListener
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.database.JpaItemWriter
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder
import org.springframework.batch.item.file.transform.FieldSet
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.transaction.PlatformTransactionManager

@Configuration
@EnableBatchProcessing
class BatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val trafficDensityProcessor: TrafficDensityProcessor,
    private val jobCompletionNotificationListener: JobCompletionNotificationListener,
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

    @Bean
    fun importTrafficDensityStep(): Step =
        StepBuilder("importTrafficDensityStep", jobRepository)
            .chunk<TrafficDensity, TrafficDensity>(10_000, transactionManager)
            .reader(reader(null)) // null path just for type resolution
            .writer(writer())
            .processor(trafficDensityProcessor)
            .build()

    @Bean
    fun importCustomerJob(): Job =
        JobBuilder("importCustomerJob", jobRepository)
            .start(importTrafficDensityStep())
            .listener(jobCompletionNotificationListener)
            .build()

}
