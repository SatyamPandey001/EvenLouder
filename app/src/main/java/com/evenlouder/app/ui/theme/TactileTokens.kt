package com.evenlouder.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tactile Design Tokens for the Modern Soft Tactile / Subtle Skeuomorphic UI aesthetic.
 */
object TactileTokens {
    // Light Mode Canvas: Soft neutral sculpted surfaces
    val LightCanvas = Color(0xFFEEF1F5)
    val LightSurfaceLight = Color(0xFFF7F9FB)
    val LightSurfaceBase = Color(0xFFEEF1F5)
    val LightSurfaceDark = Color(0xFFE2E7ED)

    val LightShadowDark = Color(0x52A3B1C6)      // 6px 6px 16px rgba(163, 177, 198, 0.45)
    val LightShadowLight = Color(0xD9FFFFFF)     // -6px -6px 14px rgba(255, 255, 255, 0.85)
    val LightSpecularBorder = Color(0x99FFFFFF)  // 1px specular highlight rgba(255, 255, 255, 0.6)
    val LightAmbientBorder = Color(0x1F718096)   // Subtle bottom-right ambient rim

    // Dark Mode Canvas: Soft sculpted dark surfaces
    val DarkCanvas = Color(0xFF1E2229)
    val DarkSurfaceLight = Color(0xFF262C36)
    val DarkSurfaceBase = Color(0xFF1E2229)
    val DarkSurfaceDark = Color(0xFF171A20)

    val DarkShadowDark = Color(0x800C0E12)
    val DarkShadowLight = Color(0x2EFFFFFF)
    val DarkSpecularBorder = Color(0x3DFFFFFF)
    val DarkAmbientBorder = Color(0x40000000)

    // Recessed / Inset Tokens
    val LightInsetDark = Color(0x4DA3B1C6)
    val LightInsetLight = Color(0xCCFFFFFF)
    val DarkInsetDark = Color(0x80000000)
    val DarkInsetLight = Color(0x14FFFFFF)

    // Primary CTA Glossy Accents
    val PrimaryCtaGradientLight = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
    val PrimaryCtaGradientDark = listOf(Color(0xFF00E5FF), Color(0xFF0091EA))
    val AmberCtaGradient = listOf(Color(0xFFFFC107), Color(0xFFFF8F00))
    val GreenCtaGradient = listOf(Color(0xFF10B981), Color(0xFF059669))
}

/**
 * Draws a raised extruded physical surface with:
 *  - Dual soft shadows (top-left crisp highlight + bottom-right ambient shadow)
 *  - Directional overhead surface gradient (lighter top-left to darker bottom-right)
 *  - 1px specular top/left highlight border
 */
fun Modifier.tactileRaised(
    shapeRadius: Dp = 20.dp,
    elevation: Dp = 4.dp,
    isDarkTheme: Boolean = true,
    pressed: Boolean = false,
    surfaceGradient: List<Color>? = null
): Modifier = composed {
    val lightShadow = if (isDarkTheme) TactileTokens.DarkShadowLight else TactileTokens.LightShadowLight
    val darkShadow = if (isDarkTheme) TactileTokens.DarkShadowDark else TactileTokens.LightShadowDark
    val specular = if (isDarkTheme) TactileTokens.DarkSpecularBorder else TactileTokens.LightSpecularBorder
    val ambientBorder = if (isDarkTheme) TactileTokens.DarkAmbientBorder else TactileTokens.LightAmbientBorder

    val gradient = surfaceGradient ?: if (isDarkTheme) {
        listOf(TactileTokens.DarkSurfaceLight, TactileTokens.DarkSurfaceBase, TactileTokens.DarkSurfaceDark)
    } else {
        listOf(TactileTokens.LightSurfaceLight, TactileTokens.LightSurfaceBase, TactileTokens.LightSurfaceDark)
    }

    drawBehind {
        val radiusPx = shapeRadius.toPx()
        val corner = CornerRadius(radiusPx, radiusPx)
        val w = size.width
        val h = size.height

        val offsetDist = if (pressed) elevation.toPx() * 0.25f else elevation.toPx()
        val blurRadius = if (pressed) offsetDist * 1.5f else offsetDist * 2.5f

        drawIntoCanvas { canvas ->
            if (!pressed && offsetDist > 0f) {
                // Top-Left Light Highlight Shadow
                val lightPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = lightShadow.toArgb()
                    if (blurRadius > 0f) {
                        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                    }
                }
                canvas.nativeCanvas.drawRoundRect(
                    -offsetDist, -offsetDist,
                    w - offsetDist, h - offsetDist,
                    radiusPx, radiusPx, lightPaint
                )

                // Bottom-Right Ambient Dark Shadow
                val darkPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = darkShadow.toArgb()
                    if (blurRadius > 0f) {
                        maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                    }
                }
                canvas.nativeCanvas.drawRoundRect(
                    offsetDist, offsetDist,
                    w + offsetDist, h + offsetDist,
                    radiusPx, radiusPx, darkPaint
                )
            }
        }

        // Tactile Surface Gradient
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = gradient,
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            size = size,
            cornerRadius = corner
        )

        // 1px Specular Top/Left Rim & Ambient Bottom/Right Rim
        drawRoundRect(
            brush = Brush.linearGradient(
                0.0f to specular,
                0.45f to specular.copy(alpha = specular.alpha * 0.5f),
                0.55f to ambientBorder.copy(alpha = ambientBorder.alpha * 0.3f),
                1.0f to ambientBorder,
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            size = size,
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * Draws a recessed / inset physical groove for slider tracks, input fields,
 * search boxes, and active pressed toggles.
 */
fun Modifier.tactileInset(
    shapeRadius: Dp = 12.dp,
    isDarkTheme: Boolean = true,
    depth: Dp = 3.dp,
    backgroundColor: Color? = null
): Modifier = composed {
    val darkInset = if (isDarkTheme) TactileTokens.DarkInsetDark else TactileTokens.LightInsetDark
    val lightInset = if (isDarkTheme) TactileTokens.DarkInsetLight else TactileTokens.LightInsetLight
    val baseBg = backgroundColor ?: if (isDarkTheme) TactileTokens.DarkSurfaceDark else TactileTokens.LightSurfaceDark

    drawBehind {
        val radiusPx = shapeRadius.toPx()
        val corner = CornerRadius(radiusPx, radiusPx)
        val w = size.width
        val h = size.height

        // Fill sunken base
        drawRoundRect(
            color = baseBg,
            size = size,
            cornerRadius = corner
        )

        val depthPx = depth.toPx()

        // Top-Left Inset Shadow (Dark)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(darkInset, Color.Transparent),
                start = Offset(0f, 0f),
                end = Offset(depthPx * 2.5f, depthPx * 2.5f)
            ),
            size = size,
            cornerRadius = corner
        )

        // Bottom-Right Inset Highlight (Light)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, lightInset),
                start = Offset(w - depthPx * 2.5f, h - depthPx * 2.5f),
                end = Offset(w, h)
            ),
            size = size,
            cornerRadius = corner
        )

        // Subtle sunken rim stroke
        drawRoundRect(
            brush = Brush.linearGradient(
                0.0f to darkInset.copy(alpha = 0.5f),
                1.0f to lightInset.copy(alpha = 0.4f),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            size = size,
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * Interactive tactile clickable modifier with physical press feedback:
 * Subtle scale down to 0.98 and transition to pressed tactile state.
 */
fun Modifier.tactileClickable(
    shapeRadius: Dp = 16.dp,
    isDarkTheme: Boolean = true,
    elevation: Dp = 4.dp,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "tactileScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .tactileRaised(
            shapeRadius = shapeRadius,
            elevation = elevation,
            isDarkTheme = isDarkTheme,
            pressed = isPressed
        )
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(shapeRadius))
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Primary CTA Button styling:
 * Rich glossy gradient accent, soft colored glow shadow, specular top/left rim,
 * and physical press feedback.
 */
fun Modifier.tactileCta(
    gradient: List<Color>,
    glowColor: Color = gradient.first(),
    shapeRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    isPressed: Boolean = false
): Modifier = composed {
    drawBehind {
        val radiusPx = shapeRadius.toPx()
        val corner = CornerRadius(radiusPx, radiusPx)
        val w = size.width
        val h = size.height

        val offsetDist = if (isPressed) elevation.toPx() * 0.3f else elevation.toPx()
        val blurRadius = if (isPressed) offsetDist * 1.5f else offsetDist * 2.8f

        drawIntoCanvas { canvas ->
            // Soft colored glow shadow
            val glowPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = glowColor.copy(alpha = if (isPressed) 0.25f else 0.45f).toArgb()
                if (blurRadius > 0f) {
                    maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.nativeCanvas.drawRoundRect(
                0f, offsetDist * 0.8f,
                w, h + offsetDist * 1.2f,
                radiusPx, radiusPx, glowPaint
            )
        }

        // Rich glossy gradient surface
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = gradient,
                startY = 0f,
                endY = h
            ),
            size = size,
            cornerRadius = corner
        )

        // Specular highlight on upper half for glossy skeuomorphic sheen
        val highlightPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = w,
                    bottom = h * 0.52f,
                    topLeftCornerRadius = corner,
                    topRightCornerRadius = corner,
                    bottomLeftCornerRadius = CornerRadius(radiusPx * 0.4f, radiusPx * 0.4f),
                    bottomRightCornerRadius = CornerRadius(radiusPx * 0.4f, radiusPx * 0.4f)
                )
            )
        }
        drawPath(
            path = highlightPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.03f)),
                startY = 0f,
                endY = h * 0.52f
            )
        )

        // Crisp 1px specular top border
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                startY = 0f,
                endY = h
            ),
            size = size,
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
