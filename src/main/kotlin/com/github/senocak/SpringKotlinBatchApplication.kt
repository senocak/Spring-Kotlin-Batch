package com.github.senocak

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.github.senocak"])
class SpringKotlinBatchApplication

fun main(args: Array<String>) {
    runApplication<SpringKotlinBatchApplication>(*args)
}

fun <R : Any> R.logger(): Lazy<Logger> = lazy {
    LoggerFactory.getLogger((if (javaClass.kotlin.isCompanion) javaClass.enclosingClass else javaClass).name)
}