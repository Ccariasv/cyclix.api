package com.cyclix.cyclix_api.device.websocket

import com.cyclix.cyclix_api.device.service.DeviceWebSocketSessionRegistry
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class DeviceWebSocketAuthInterceptor(
    @Value("\${app.device.api-key}") private val deviceApiKey: String
) : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val servletRequest = (request as? ServletServerHttpRequest)?.servletRequest
            ?: return reject(response, HttpStatus.BAD_REQUEST)

        val bikeId = extractBikeId(servletRequest) ?: return reject(response, HttpStatus.BAD_REQUEST)
        val providedApiKey = servletRequest.getHeader("X-Device-Api-Key")
            ?: servletRequest.getParameter("apiKey")

        if (deviceApiKey.isBlank() || providedApiKey.isNullOrBlank() || providedApiKey != deviceApiKey) {
            return reject(response, HttpStatus.UNAUTHORIZED)
        }

        attributes[DeviceWebSocketSessionRegistry.BIKE_ID_ATTRIBUTE] = bikeId
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) = Unit

    private fun extractBikeId(request: HttpServletRequest): Long? {
        val rawBikeId = request.getParameter("bikeId") ?: return null
        return rawBikeId.toLongOrNull()?.takeIf { it > 0 }
    }

    private fun reject(response: ServerHttpResponse, status: HttpStatus): Boolean {
        response.setStatusCode(status)
        return false
    }
}
