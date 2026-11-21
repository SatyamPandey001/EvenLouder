package com.evenlouder.app.ui.components

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.evenlouder.app.ui.theme.DarkBackground
import com.evenlouder.app.ui.theme.NeonAmber
import com.evenlouder.app.ui.theme.NeonCyan

/**
 * Exact asset file names located in the assets directory.
 * Used directly without modification, recreation, or renaming.
 */
object EvenLouderAssets {
    const val WAVEFORM_DARK_GIF = "EvenLouder_waveform-dark.gif"
    const val WAVEFORM_TRANSPARENT_GIF = "EvenLouder_waveform-transparent.gif"
    const val WAVEFORM_ICON_PNG = "EvenLouder_waveform-icon.png"
}

/**
 * Visual variant selection for [EvenLouderLoader].
 */
enum class LoaderVariant {
    /**
     * Auto: Selects dark GIF for full-screen dark presentations, and transparent
     * GIF for inline or light backgrounds.
     */
    Auto,

    /**
     * Transparent animated waveform (renders seamlessly over light or dark themes).
     */
    Transparent,

    /**
     * Dark animated waveform (ideal for dark backgrounds and startup screen).
     */
    Dark,

    /**
     * Static PNG icon for application identity and fallback branding.
     */
    StaticIcon
}

/**
 * Centralized branding and loading component for EvenLouder.
 *
 * Configurable parameters:
 * @param modifier Custom modifier for external layout adjustments.
 * @param size Configurable dimension for the waveform animation / icon.
 * @param isFullScreen Whether to present as a full-screen overlay or compact inline widget.
 * @param background Optional background color. Defaults to transparent for inline, or DarkBackground for full-screen.
 * @param message Optional loading or status message displayed below the waveform.
 * @param messageColor Text color for the loading message.
 * @param variant Which asset variant to display ([LoaderVariant.Auto], [LoaderVariant.Transparent], [LoaderVariant.Dark], [LoaderVariant.StaticIcon]).
 * @param contentDescription Accessibility description.
 */
@Composable
fun EvenLouderLoader(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    isFullScreen: Boolean = false,
    background: Color = Color.Transparent,
    message: String? = null,
    messageColor: Color = NeonCyan,
    variant: LoaderVariant = LoaderVariant.Auto,
    contentDescription: String = "EvenLouder Loading"
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDarkBackground = background == DarkBackground || 
                           background == Color.Black || 
                           (background == Color.Transparent && isSystemDark)

    // Select asset based on variant and context
    val assetFileName = when (variant) {
        LoaderVariant.Dark -> EvenLouderAssets.WAVEFORM_DARK_GIF
        LoaderVariant.Transparent -> EvenLouderAssets.WAVEFORM_TRANSPARENT_GIF
        LoaderVariant.StaticIcon -> EvenLouderAssets.WAVEFORM_ICON_PNG
        LoaderVariant.Auto -> {
            if (isFullScreen && isDarkBackground) {
                EvenLouderAssets.WAVEFORM_DARK_GIF
            } else {
                EvenLouderAssets.WAVEFORM_TRANSPARENT_GIF
            }
        }
    }

    if (isFullScreen) {
        val effectiveBg = if (background == Color.Transparent) DarkBackground else background
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(effectiveBg)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                EvenLouderAssetView(
                    assetFileName = assetFileName,
                    modifier = Modifier.size(size)
                )

                if (!message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = message,
                        color = messageColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    } else {
        // Inline presentation
        Box(
            modifier = modifier
                .then(
                    if (background != Color.Transparent) {
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(background)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (message.isNullOrBlank()) {
                EvenLouderAssetView(
                    assetFileName = assetFileName,
                    modifier = Modifier.size(size)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EvenLouderAssetView(
                        assetFileName = assetFileName,
                        modifier = Modifier.size(size)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        color = messageColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Native hardware-accelerated asset viewer supporting animated GIFs and static PNGs
 * directly loaded from the assets directory using Android's ImageDecoder.
 */
@Composable
fun EvenLouderAssetView(
    assetFileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val drawableState = produceState<Drawable?>(initialValue = null, key1 = assetFileName) {
        value = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.assets, assetFileName)
                ImageDecoder.decodeDrawable(source)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    val drawable = drawableState.value

    DisposableEffect(drawable) {
        if (drawable is AnimatedImageDrawable) {
            drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            drawable.start()
        }
        onDispose {
            if (drawable is AnimatedImageDrawable) {
                drawable.stop()
            }
        }
    }

    if (drawable != null) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                    setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable && !drawable.isRunning) {
                        drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        drawable.start()
                    }
                }
            },
            update = { imageView ->
                imageView.setImageDrawable(drawable)
                if (drawable is AnimatedImageDrawable && !drawable.isRunning) {
                    drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    drawable.start()
                }
            },
            modifier = modifier
        )
    } else {
        // Subtle fallback during initial asset frame decoding
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = NeonCyan,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
