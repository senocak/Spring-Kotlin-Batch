package com.github.senocak.config

import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class BatchTestConfig {
    @Bean
    fun jobLauncherTestUtils(): JobLauncherTestUtils {
        return JobLauncherTestUtils()
    }
}
