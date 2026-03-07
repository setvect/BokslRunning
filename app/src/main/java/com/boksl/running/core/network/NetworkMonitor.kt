package com.boksl.running.core.network

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    fun observeIsOnline(): Flow<Boolean>
}
