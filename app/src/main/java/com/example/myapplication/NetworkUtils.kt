package com.example.myapplication

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object NetworkUtils {
    
    data class CheckResult(val isOnline: Boolean, val latency: Long)

    suspend fun checkStatusWithLatency(ip: String): CheckResult {
        var latency = -1L
        val isOnline = try {
            val address = InetAddress.getByName(ip)
            val startTime = System.currentTimeMillis()
            
            val reachable = if (address.isReachable(2000)) {
                latency = System.currentTimeMillis() - startTime
                true
            } else {
                val portsToTest = listOf(80, 443, 53, 8080)
                var found = false
                for (port in portsToTest) {
                    val portStartTime = System.currentTimeMillis()
                    if (isPortOpen(ip, port)) {
                        latency = System.currentTimeMillis() - portStartTime
                        found = true
                        break
                    }
                }
                found
            }
            reachable
        } catch (e: Exception) {
            false
        }
        return CheckResult(isOnline, if (isOnline) latency else -1L)
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int = 1500): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun scanPorts(ip: String, callback: (List<Int>) -> Unit) {
        val commonPorts = listOf(21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 3306, 3389, 5432, 8080)
        val openPorts = mutableListOf<Int>()
        val executor = Executors.newFixedThreadPool(10)

        commonPorts.forEach { port ->
            executor.execute {
                if (isPortOpen(ip, port, 500)) {
                    synchronized(openPorts) {
                        openPorts.add(port)
                    }
                }
            }
        }

        executor.shutdown()
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        callback(openPorts.sorted())
    }

    fun getLocalSubnet(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val lp = cm.getLinkProperties(activeNetwork)
        
        lp?.linkAddresses?.forEach { linkAddress ->
            val address = linkAddress.address
            if (!address.isLoopbackAddress && address.address.size == 4) { // IPv4
                val ip = address.hostAddress ?: ""
                if (ip.isNotEmpty()) {
                    return ip.substringBeforeLast(".")
                }
            }
        }
        return "192.168.0" // Fallback
    }

    fun scanSubnet(subnetPrefix: String, onProgress: (Int) -> Unit, onComplete: (List<String>) -> Unit) {
        val foundIps = mutableListOf<String>()
        val progress = AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(30)

        for (i in 1..254) {
            val ip = "$subnetPrefix.$i"
            executor.execute {
                try {
                    val address = InetAddress.getByName(ip)
                    if (address.isReachable(500)) {
                        synchronized(foundIps) { foundIps.add(ip) }
                    } else {
                        if (isPortOpen(ip, 80, 200)) {
                            synchronized(foundIps) { foundIps.add(ip) }
                        }
                    }
                } catch (e: Exception) {} finally {
                    onProgress(progress.incrementAndGet())
                }
            }
        }

        executor.shutdown()
        Thread {
            try {
                executor.awaitTermination(40, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            onComplete(foundIps.sorted())
        }.start()
    }

    fun sendWakeOnLan(macAddress: String) {
        try {
            val macBytes = getMacBytes(macAddress)
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0..5) {
                bytes[i] = 0xff.toByte()
            }
            var i = 6
            while (i < bytes.size) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
                i += macBytes.size
            }

            val address = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(bytes, bytes.size, address, 9)
            val socket = DatagramSocket()
            socket.send(packet)
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMacBytes(macStr: String): ByteArray {
        val bytes = ByteArray(6)
        val hex = macStr.split(":", "-")
        if (hex.size != 6) throw IllegalArgumentException("Invalid MAC address.")
        for (i in 0..5) {
            bytes[i] = hex[i].toInt(16).toByte()
        }
        return bytes
    }
}
