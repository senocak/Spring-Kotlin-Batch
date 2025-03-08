package com.github.senocak.service

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ProgressTracker {
    private val progressMap = ConcurrentHashMap<String, JobProgress>()

    data class JobProgress(
        var totalRead: Long = 0,
        var totalWritten: Long = 0,
        var lastUpdate: Long = System.currentTimeMillis(),
        var skipCount: Long = 0
    )

    fun getProgress(jobId: String): JobProgress =
        progressMap.getOrPut(key = jobId) { JobProgress() }

    fun reset(jobId: String) {
        progressMap[jobId] = JobProgress()
    }

    fun updateProgress(jobId: String, block: JobProgress.() -> Unit) {
        progressMap[jobId] = getProgress(jobId = jobId).apply(block)
    }

    fun removeProgress(jobId: String) {
        progressMap.remove(jobId)
    }

    override fun toString(): String = "ProgressTracker(jobs=${progressMap.keys.size}, progress=$progressMap)"
}
