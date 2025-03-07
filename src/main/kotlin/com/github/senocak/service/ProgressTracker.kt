package com.github.senocak.service

import org.springframework.stereotype.Component

@Component
class ProgressTracker {
    var totalRead: Long = 0
    var totalWritten: Long = 0
    var lastUpdate: Long = System.currentTimeMillis()
    var skipCount: Long = 0

    fun reset() {
        totalRead = 0
        totalWritten = 0
        lastUpdate = System.currentTimeMillis()
        skipCount = 0
    }

    override fun toString(): String = "ProgressTracker(totalRead=$totalRead, totalWritten=$totalWritten, lastUpdate=$lastUpdate, skipCount=$skipCount)"
}