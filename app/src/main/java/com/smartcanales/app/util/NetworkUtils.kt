package com.smartcanales.app.util

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

	fun localIpv4Addresses(): List<String> {
		val result = mutableListOf<String>()
		try {
			val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
			for (netif in interfaces) {
				if (!netif.isUp || netif.isLoopback) continue
				val addresses = netif.inetAddresses
				while (addresses.hasMoreElements()) {
					val addr = addresses.nextElement()
					if (addr is Inet4Address && !addr.isLoopbackAddress) {
						result += addr.hostAddress
					}
				}
			}
		} catch (_: Exception) {
			// ignore
		}
		return result.distinct()
	}

	fun preferredLocalIp(): String? {
		val all = localIpv4Addresses()
		return all.firstOrNull { it.startsWith("192.168.") }
			?: all.firstOrNull { it.startsWith("10.") }
			?: all.firstOrNull()
	}
}
