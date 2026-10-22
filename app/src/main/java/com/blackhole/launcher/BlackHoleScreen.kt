package com.blackhole.launcher

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class Star(val x: Float, val y: Float, val size: Float, val alpha: Float)
data class Shockwave(var radius: Float, var alpha: Float, val maxRadius: Float = 300f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackHoleScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val physicsIcons = remember { mutableStateListOf<PhysicsIcon>() }
    val shockwaves = remember { mutableStateListOf<Shockwave>() }

    var isSwallowing by remember { mutableStateOf(false) }
    var showDrawer by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var diskRotation by remember { mutableFloatStateOf(0f) }

    // Fecha y hora en tiempo real
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    // Estrellas de fondo estáticas
    val stars = remember {
        List(90) {
            Star(
                x = (0..1000).random().toFloat() / 1000f,
                y = (0..1000).random().toFloat() / 1000f,
                size = (1..3).random().toFloat(),
                alpha = (3..9).random().toFloat() / 10f
            )
        }
    }

    // Cargar apps instaladas
    LaunchedEffect(Unit) {
        val apps = AppManager.loadInstalledApps(context)
        installedApps = apps
    }

    // Reloj continuo
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "PE"))
        while (isActive) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }

    // Inicializar posiciones de las físicas de los iconos una vez conocidas las dimensiones
    var screenWidth by remember { mutableFloatStateOf(1080f) }
    var screenHeight by remember { mutableFloatStateOf(2160f) }

    fun resetIconsPositions() {
        val cx = screenWidth / 2f
        val cy = screenHeight / 2f
        val count = installedApps.size
        physicsIcons.clear()

        // Asignar en múltiples órbitas elípticas estables
        installedApps.forEachIndexed { i, app ->
            val orbitLevel = (i % 5) + 1
            val baseRadius = 220f + (orbitLevel * 95f)
            val angle = (2 * Math.PI.toFloat() / count) * i

            val pIcon = PhysicsIcon(app = app)
            pIcon.reset(cx, cy, baseRadius, angle)
            physicsIcons.add(pIcon)
        }
    }

    LaunchedEffect(installedApps, screenWidth, screenHeight) {
        if (installedApps.isNotEmpty() && screenWidth > 0 && screenHeight > 0) {
            resetIconsPositions()
        }
    }

    // Trigger de Haptic Feedback
    fun triggerHaptic(strong: Boolean = false) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (strong) {
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createOneShot(18, 120)
                }
                vibrator?.vibrate(effect)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } catch (_: Exception) {}
    }

    // Expulsión Big Bang
    fun triggerBigBang() {
        triggerHaptic(strong = true)
        val cx = screenWidth / 2f
        val cy = screenHeight / 2f

        shockwaves.add(Shockwave(radius = 60f, alpha = 1f, maxRadius = screenWidth * 0.8f))
        shockwaves.add(Shockwave(radius = 120f, alpha = 0.8f, maxRadius = screenWidth * 1.2f))

        physicsIcons.forEachIndexed { i, icon ->
            val targetRadius = 220f + ((i % 5 + 1) * 95f)
            val angle = (2 * Math.PI.toFloat() / physicsIcons.size) * i
            icon.reset(cx, cy, targetRadius, angle)
            icon.radialSpeed = 25f + (Math.random().toFloat() * 15f)
        }
        isSwallowing = false
    }

    // Bucle de Física Cuántica / Relatividad a 60 FPS
    LaunchedEffect(Unit) {
        val eventHorizonRadius = 90f // Radio de la singularidad
        while (isActive) {
            val cx = screenWidth / 2f
            val cy = screenHeight / 2f

            diskRotation = (diskRotation + 0.8f) % 360f

            // Actualizar ondas gravitacionales (shockwaves)
            val shockwaveIter = shockwaves.iterator()
            while (shockwaveIter.hasNext()) {
                val sw = shockwaveIter.next()
                sw.radius += 12f
                sw.alpha -= 0.03f
                if (sw.alpha <= 0f || sw.radius >= sw.maxRadius) {
                    shockwaveIter.remove()
                }
            }

            // Actualizar físicas de cada icono
            for (icon in physicsIcons) {
                if (icon.isDragging) continue

                if (!icon.isSwallowed) {
                    // Órbita normal o aceleración hacia el agujero negro
                    if (isSwallowing) {
                        // Ley de atracción gravitacional inversa al cuadrado
                        val gravityFactor = max(2.5f, 450f / max(1f, icon.radius))
                        icon.radius -= gravityFactor

                        // Conservación del momento angular: gira más rápido cuanto más cerca
                        val spinAcceleration = 0.035f + (0.08f * (1f - (icon.radius / 650f).coerceIn(0f, 1f)))
                        icon.angle += spinAcceleration

                        // Efecto de espaguetificación y encogimiento al cruzar el límite
                        if (icon.radius < 240f) {
                            val ratio = ((icon.radius - eventHorizonRadius) / (240f - eventHorizonRadius)).coerceIn(0.05f, 1f)
                            icon.scale = ratio
                            icon.alpha = ratio.coerceIn(0.1f, 1f)
                        }

                        // Cruzó el Horizonte de Sucesos (Singularidad absoluta)
                        if (icon.radius <= eventHorizonRadius) {
                            icon.isSwallowed = true
                            icon.swallowedTime = System.currentTimeMillis()
                            triggerHaptic(strong = false)
                            // Generar pulso de onda gravitacional
                            if (shockwaves.size < 6) {
                                shockwaves.add(Shockwave(radius = eventHorizonRadius, alpha = 0.9f))
                            }
                        }
                    } else {
                        // Órbita estable
                        if (icon.radialSpeed > 0f) {
                            icon.radialSpeed *= 0.92f
                            if (icon.radialSpeed < 0.2f) icon.radialSpeed = 0f
                        }
                        icon.angle += icon.angularSpeed

                        // Volver suavemente a radio base si fue alterado
                        if (icon.radialSpeed == 0f && abs(icon.radius - icon.baseRadius) > 2f) {
                            icon.radius += (icon.baseRadius - icon.radius) * 0.05f
                        }
                    }

                    // Calcular coordenadas Cartesianas
                    icon.x = cx + icon.radius * cos(icon.angle)
                    icon.y = cy + icon.radius * sin(icon.angle)
                }
            }

            delay(16) // ~60 FPS
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val distToCenter = sqrt((offset.x - cx).pow(2) + (offset.y - cy).pow(2))

                        // Tocar el agujero negro activa/desactiva la succión
                        if (distToCenter <= 120f) {
                            isSwallowing = !isSwallowing
                            triggerHaptic(strong = isSwallowing)
                            return@detectTapGestures
                        }

                        // Detectar toque en icono de app para abrirla
                        val clickedIcon = physicsIcons.find { icon ->
                            !icon.isSwallowed &&
                            sqrt((offset.x - icon.x).pow(2) + (offset.y - icon.y).pow(2)) <= (44f * icon.scale)
                        }

                        if (clickedIcon != null) {
                            triggerHaptic(strong = false)
                            AppManager.launchApp(context, clickedIcon.app)
                        }
                    },
                    onDoubleTap = { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val distToCenter = sqrt((offset.x - cx).pow(2) + (offset.y - cy).pow(2))
                        if (distToCenter <= 160f) {
                            triggerBigBang()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var draggedIcon: PhysicsIcon? = null
                detectDragGestures(
                    onDragStart = { offset ->
                        draggedIcon = physicsIcons.find { icon ->
                            !icon.isSwallowed &&
                            sqrt((offset.x - icon.x).pow(2) + (offset.y - icon.y).pow(2)) <= 50f
                        }
                        draggedIcon?.isDragging = true
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggedIcon?.let { icon ->
                            icon.x += dragAmount.x
                            icon.y += dragAmount.y
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            icon.radius = sqrt((icon.x - cx).pow(2) + (icon.y - cy).pow(2))
                            icon.angle = atan2(icon.y - cy, icon.x - cx)
                        }
                    },
                    onDragEnd = {
                        draggedIcon?.let { icon ->
                            icon.isDragging = false
                            icon.baseRadius = icon.radius
                        }
                        draggedIcon = null
                    },
                    onDragCancel = {
                        draggedIcon?.isDragging = false
                        draggedIcon = null
                    }
                )
            }
    ) {
        // LIENZO CANVAS DE RENDERIZADO CÓSMICO
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenWidth = size.width
            screenHeight = size.height
            val cx = size.width / 2f
            val cy = size.height / 2f

            // 1. Estrellas de fondo
            stars.forEach { star ->
                val sx = star.x * size.width
                val sy = star.y * size.height
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha),
                    radius = star.size,
                    center = Offset(sx, sy)
                )
            }

            // 2. Ondas gravitacionales (Shockwaves)
            shockwaves.forEach { sw ->
                drawCircle(
                    color = Color(0xFF06B6D4).copy(alpha = sw.alpha.coerceIn(0f, 1f)),
                    radius = sw.radius,
                    center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
            }

            // 3. Disco de Acreción Cósmico (Gargantúa)
            rotate(diskRotation, pivot = Offset(cx, cy)) {
                // Halo exterior rojo/naranja
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x00EA580C),
                            Color(0x55EA580C),
                            Color(0x99F59E0B),
                            Color(0xCCFBBF24),
                            Color(0x00FBBF24)
                        ),
                        center = Offset(cx, cy),
                        radius = 210f
                    ),
                    radius = 210f,
                    center = Offset(cx, cy)
                )

                // Disco brillante plasma
                drawOval(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x88EA580C),
                            Color(0xFFF59E0B),
                            Color(0xFF38BDF8),
                            Color(0xFFF59E0B),
                            Color(0x88EA580C)
                        )
                    ),
                    topLeft = Offset(cx - 170f, cy - 40f),
                    size = androidx.compose.ui.geometry.Size(340f, 80f)
                )
            }

            // 4. Anillo de Fotones (Luz doblada por relatividad)
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = 98f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 94f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // 5. Singularidad / Horizonte de Sucesos (Negro Absoluto)
            drawCircle(
                color = Color.Black,
                radius = 92f,
                center = Offset(cx, cy)
            )

            // 6. Dibujar Iconos de las Apps Orbitando o Cayendo
            physicsIcons.forEach { pIcon ->
                if (!pIcon.isSwallowed && pIcon.app.icon != null) {
                    val iconBitmap = pIcon.app.icon.asImageBitmap()
                    val iconSize = (56.dp.toPx() * pIcon.scale).coerceAtLeast(8f)

                    scale(pIcon.scale, pivot = Offset(pIcon.x, pIcon.y)) {
                        drawImage(
                            image = iconBitmap,
                            dstOffset = IntOffset(
                                (pIcon.x - iconSize / 2).toInt(),
                                (pIcon.y - iconSize / 2).toInt()
                            ),
                            dstSize = IntSize(iconSize.toInt(), iconSize.toInt()),
                            alpha = pIcon.alpha
                        )
                    }
                }
            }
        }

        // HUD SUPERIOR: RELOJ Y ESTADO CÓSMICO
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = 1.sp
            )
            Text(
                text = currentDate,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF38BDF8).copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Indicador de estado del agujero negro
            val swallowedCount = physicsIcons.count { it.isSwallowed }
            val totalCount = physicsIcons.size
            Surface(
                color = if (isSwallowing) Color(0x33EF4444) else Color(0x3310B981),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSwallowing) Color(0xFFEF4444) else Color(0xFF10B981)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isSwallowing) "🕳️ Tragar Apps: ACTIVO" else "🌌 Órbita Estable",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "• ($swallowedCount/$totalCount absorbidas)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // HUD INFERIOR: CONTROLES CÓSMICOS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón: Activar/Pausar Vórtice
            FilledTonalButton(
                onClick = {
                    isSwallowing = !isSwallowing
                    triggerHaptic(strong = isSwallowing)
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isSwallowing) Color(0xFFEA580C) else Color(0xFF1F2937),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isSwallowing) Icons.Default.Pause else Icons.Default.Cyclone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSwallowing) "Pausar" else "Tragar Apps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Botón: Big Bang (Restaurar)
            FilledTonalButton(
                onClick = { triggerBigBang() },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Big Bang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Botón: Todas las apps (Drawer)
            IconButton(
                onClick = { showDrawer = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF374151))
            ) {
                Icon(Icons.Default.Apps, contentDescription = "Todas las Apps", tint = Color.White)
            }

            // Botón: Ajustes / Elegir Launcher
            IconButton(
                onClick = { AppManager.openHomeSettings(context) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF374151))
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
            }
        }

        // CAJÓN DE TODAS LAS APLICACIONES (DRAWER VERTICAL)
        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xF0030712)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Todas las Aplicaciones",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar aplicación...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredApps = installedApps.filter {
                        it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showDrawer = false
                                        AppManager.launchApp(context, app)
                                    },
                                color = Color(0xFF111827),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    if (app.icon != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = app.icon.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(42.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .background(Color.DarkGray, CircleShape)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = app.label,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = app.packageName,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
