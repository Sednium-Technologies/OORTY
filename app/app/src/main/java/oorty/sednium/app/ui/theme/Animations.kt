package oorty.sednium.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Direct ports of the four CSS @keyframes / animate-* utility classes
 * defined in the original index.html <style> block, plus the Tailwind
 * built-ins (`animate-pulse`, `animate-bounce`, `animate-spin`) the
 * components leaned on for loading/typing states.
 *
 *   .animate-fade-in   { 0.3s ease-out;                          opacity 0->1 }
 *   .animate-slide-up  { 0.3s cubic-bezier(.2,.8,.2,1);          translateY(100%)->0 }
 *   .animate-pop-up    { 0.2s cubic-bezier(.175,.885,.32,1.275); scale(.95)->1, y(10px)->0 }
 */
object SedniumMotion {

    // ---- Durations (ms), lifted verbatim from the CSS ----
    const val FAST = 150
    const val BASE = 200      // pop-up
    const val SLIDE = 300     // slide-up / drawer transform
    const val FADE = 300      // fade-in
    const val SPIN_MS = 1000  // RefreshCw spinner, one full rotation
    const val BOUNCE_MS = 1000
    const val PULSE_MS = 2000

    // ---- Easings ----
    /** cubic-bezier(0.2, 0.8, 0.2, 1) — slide-up sheets & the chat-list drawer. */
    val SlideUpEasing: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    /** cubic-bezier(0.175, 0.885, 0.32, 1.275) — overshoot "pop", used by small menus. */
    val PopUpEasing: Easing = CubicBezierEasing(0.175f, 0.885f, 0.32f, 1.275f)

    val FadeEasing: Easing = FastOutSlowInEasing
}

/** animate-fade-in: opacity 0 -> 1 over 300ms ease-out. Use as the AnimatedVisibility spec. */
fun fadeInSpec() = tween<Float>(durationMillis = SedniumMotion.FADE, easing = SedniumMotion.FadeEasing)

/** animate-slide-up: translateY(100%) -> 0 + opacity, 300ms custom cubic-bezier. */
fun slideUpSpec() = tween<IntSize>(durationMillis = SedniumMotion.SLIDE, easing = SedniumMotion.SlideUpEasing)

/** animate-pop-up: scale(.95)->1 with translateY(10dp)->0, 200ms overshoot easing. */
fun popUpSpec() = tween<Float>(durationMillis = SedniumMotion.BASE, easing = SedniumMotion.PopUpEasing)

/** Drawer slide transform-x, 300ms ease-out (ChatListDrawer translate-x-0 / -translate-x-full). */
fun drawerSlideSpec() = tween<Float>(durationMillis = SedniumMotion.SLIDE, easing = SedniumMotion.SlideUpEasing)

/**
 * The three staggered bouncing dots used for "Thinking…" / "Generating…"
 * states (Tailwind `animate-bounce` with 0ms / 150ms / 300ms delays).
 */
@Composable
fun ThinkingDots(
    modifier: Modifier = Modifier,
    dotColor: Color,
    dotSize: Dp = 5.dp,
    gap: Dp = 4.dp
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(3) { index ->
            val transition = rememberInfiniteTransition(label = "bounce-$index")
            val translateY by transition.animateFloat(
                initialValue = 0f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = SedniumMotion.BOUNCE_MS
                        0f at 0
                        -6f at (SedniumMotion.BOUNCE_MS * 0.3).toInt() using FastOutSlowInEasing
                        0f at (SedniumMotion.BOUNCE_MS * 0.6).toInt()
                        0f at SedniumMotion.BOUNCE_MS
                    },
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * 150)
                ),
                label = "bounce-y-$index"
            )
            Box(
                modifier = Modifier
                    .offset(y = (translateY + 2).dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/** Continuous rotation, mirrors Tailwind's `animate-spin` on the RefreshCw retry icon. */
@Composable
fun SpinningIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val transition = rememberInfiniteTransition(label = "spin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(SedniumMotion.SPIN_MS, easing = LinearEasing)
        ),
        label = "spin-deg"
    )
    Icon(
        imageVector = icon,
        contentDescription = "Generating",
        tint = tint,
        modifier = modifier.size(size).rotate(rotation)
    )
}

/** Pulsing opacity 1 <-> 0.5 over 2s, mirrors Tailwind `animate-pulse` on the status caption. */
@Composable
fun PulsingText(text: String, color: Color, style: TextStyle) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alphaValue by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(SedniumMotion.PULSE_MS / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-alpha"
    )
    Text(text = text, color = color.copy(alpha = alphaValue), style = style)
}

/** Animated circular loader with percentage or indeterminate arc */
@Composable
fun ModelLoadingProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    strokeWidth: Dp = 4.dp,
    color: Color = SedniumColors.Orange,
    trackColor: Color = OrangeAlpha.a15
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc-spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "arc-spin-rot"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sweep = if (progress != null) progress * 360f else 90f
        val startAngle = if (progress != null) -90f else rotation

        // Track
        drawCircle(
            color = trackColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth.toPx())
        )
        // Progress Arc
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

/** Animated SVG-style Checkmark draw path for successful load completion */
@Composable
fun AnimatedCheckmark(
    modifier: Modifier = Modifier,
    color: Color = SedniumColors.Green500,
    strokeWidth: Dp = 4.dp,
    onFinished: () -> Unit = {}
) {
    val target = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = target.value,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        finishedListener = { onFinished() },
        label = "chk-anim"
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        target.value = 1f
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.72f)
            lineTo(w * 0.76f, h * 0.32f)
        }

        val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
        pathMeasure.setPath(path, false)
        val pathLength = pathMeasure.length

        val drawPath = androidx.compose.ui.graphics.Path()
        pathMeasure.getSegment(0f, pathLength * progress, drawPath, true)

        drawPath(
            path = drawPath,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

