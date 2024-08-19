package dev.polisan.colorchooser.presentation

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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

fun sendTcpPacket(host: String, port: Int, data: ByteArray) {
    // Create a socket connection to the specified host and port
    val socket = Socket(host, port)
    // Get the output stream of the socket
    val outputStream: OutputStream = socket.getOutputStream()

    try {
        // Send the byte array through the output stream
        outputStream.write(data)
        outputStream.flush()
        println("Data sent to $host:$port")
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        // Close the output stream and the socket
        outputStream.close()
        socket.close()
    }
}

fun sendColor(color: Color) {
    //using RGB protocol ver 1.0
    val currentTimeStamp = System.currentTimeMillis() / 1000
    println("Current time: $currentTimeStamp")

    val nonce = Random.nextLong().toULong()
    println("Red value in color: ${color.red * 255}")
    println("color hex: ${color.toHexCode()}")

    val timestampBytes = ByteBuffer.allocate(8).putLong(currentTimeStamp).array()
    val nonceBytes = ByteBuffer.allocate(8).putLong(nonce.toLong()).array()

    val version = 1.toByte()

    val HEADER = timestampBytes + nonceBytes + byteArrayOf(version)

    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    val PAYLOAD = byteArrayOf(red.toByte(), green.toByte(), blue.toByte())

    val headerWithPayload = HEADER + PAYLOAD

    var hexString = headerWithPayload.joinToString(" ") { String.format("%02X", it) }
    println("HEADER with PAYLOAD: $hexString")
    val secret = "SHARED_KEY"
    val hmacResult = hmacSha256(secret, headerWithPayload)
    hexString = hmacResult.joinToString("") { String.format("%02X", it) }
    println("HMAC-SHA256: $hexString")

    val tcpPackage = HEADER + hmacResult + PAYLOAD
    GlobalScope.launch {
        sendTcpPacket("192.168.0.5", 3384, tcpPackage)
    }

}

