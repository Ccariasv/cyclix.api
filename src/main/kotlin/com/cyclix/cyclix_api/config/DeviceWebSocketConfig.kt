package com.cyclix.cyclix_api.config

import com.cyclix.cyclix_api.device.websocket.DeviceWebSocketAuthInterceptor
import com.cyclix.cyclix_api.device.websocket.DeviceWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class DeviceWebSocketConfig(
    private val deviceWebSocketHandler: DeviceWebSocketHandler,
    private val deviceWebSocketAuthInterceptor: DeviceWebSocketAuthInterceptor
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(deviceWebSocketHandler, "/ws/device")
            .addInterceptors(deviceWebSocketAuthInterceptor)
            .setAllowedOriginPatterns("*")
    }
}
