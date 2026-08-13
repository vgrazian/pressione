package com.pressione.iperteso.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Network connectivity monitor.
 */
object NetworkMonitor {

    fun observe(): Flow<Boolean> = flow {
        emit(checkConnectivity())
    }

    suspend fun checkConnectivity(): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
