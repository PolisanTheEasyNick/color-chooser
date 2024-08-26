package dev.polisan.colorchooser.presentation

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import kotlin.random.Random

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun hmacSha256(secret: String, data: ByteArray): ByteArray {
    val secretKey = secret.toByteArray(Charsets.UTF_8)
    val keySpec = SecretKeySpec(secretKey, "HmacSHA256")
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(keySpec)
    return mac.doFinal(data)
}

suspend fun sendTcpPacket(host: String, port: Int, data: ByteArray, toWait: Boolean = false): ByteArray? {
    val socket = Socket(host, port)
    val outputStream: OutputStream = socket.getOutputStream()
    val inputStream: InputStream = BufferedInputStream(socket.getInputStream())

    return try {
        outputStream.write(data)
        outputStream.flush()
        println("Data sent to $host:$port")
        if(toWait) {
            val response = inputStream.readBytes()
            return response
        }
        null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        outputStream.close()
        inputStream.close()
        socket.close()
    }
}
fun generateHeader(isRequestColor: Boolean = false): ByteArray {
    val currentTimeStamp = System.currentTimeMillis() / 1000
    println("Current time: $currentTimeStamp")

    val nonce = Random.nextLong().toULong()

    val timestampBytes = ByteBuffer.allocate(8).putLong(currentTimeStamp).array()
    val nonceBytes = ByteBuffer.allocate(8).putLong(nonce.toLong()).array()
    var hexString = nonceBytes.joinToString(" ") { String.format("%02X", it) }
    println("Nonce: $hexString")

    val version = if (isRequestColor) 3.toByte() else 1.toByte();

    var HEADER = timestampBytes + nonceBytes + byteArrayOf(version)
    if(isRequestColor) HEADER += byteArrayOf(1.toByte())
    return HEADER
}

fun sendColor(color: Color) {
    //using RGB protocol ver 1.0
    val HEADER = generateHeader()
    println("Red value in color: ${color.red * 255}")
    println("color hex: ${color.toHexCode()}")

    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    val PAYLOAD = byteArrayOf(red.toByte(), green.toByte(), blue.toByte())

    val headerWithPayload = HEADER + PAYLOAD

    var hexString = headerWithPayload.joinToString(" ") { String.format("%02X", it) }
    println("HEADER with PAYLOAD: $hexString")
    val secret = "SHARED_SECRET"
    val hmacResult = hmacSha256(secret, headerWithPayload)
    hexString = hmacResult.joinToString(" ") { String.format("%02X", it) }
    println("HMAC-SHA256: $hexString")

    val tcpPackage = HEADER + hmacResult + PAYLOAD
    GlobalScope.launch {
        sendTcpPacket("192.168.0.4", 3384, tcpPackage)
    }
}

suspend fun receiveColor(): ByteArray? {
    val HEADER = generateHeader(true)
    var hexString = HEADER.joinToString(" ") { String.format("%02X", it) }
    println("HEADER: $hexString")
    val secret = "SHARED_SECRET"
    val hmacResult = hmacSha256(secret, HEADER)
    hexString = hmacResult.joinToString(" ") { String.format("%02X", it) }
    println("HMAC-SHA256: $hexString")

    val tcpPackage = HEADER + hmacResult
    return withContext(Dispatchers.IO) {
        try {
            var colors = sendTcpPacket("192.168.0.4", 3384, tcpPackage, true)
            colors
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
