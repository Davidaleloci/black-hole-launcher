package com.blackhole.launcher

data class PhysicsIcon(
    val app: AppInfo,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var angle: Float = 0f,
    var radius: Float = 350f,
    var baseRadius: Float = 350f,
    var angularSpeed: Float = 0.015f,
    var radialSpeed: Float = 0f,
    var scale: Float = 1.0f,
    var alpha: Float = 1.0f,
    var rotation: Float = 0f,
    var isSwallowed: Boolean = false,
    var swallowedTime: Long = 0L,
    var isDragging: Boolean = false
) {
    fun reset(centerX: Float, centerY: Float, targetRadius: Float, targetAngle: Float) {
        radius = targetRadius
        baseRadius = targetRadius
        angle = targetAngle
        angularSpeed = (0.008f + (Math.random().toFloat() * 0.015f)) * if (Math.random() > 0.5) 1f else -1f
        radialSpeed = 0f
        scale = 1.0f
        alpha = 1.0f
        isSwallowed = false
        isDragging = false
        x = centerX + radius * kotlin.math.cos(angle)
        y = centerY + radius * kotlin.math.sin(angle)
    }
}
