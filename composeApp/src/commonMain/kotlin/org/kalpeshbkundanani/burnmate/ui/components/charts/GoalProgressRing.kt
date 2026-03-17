package org.kalpeshbkundanani.burnmate.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.GoalProgressRingState
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun GoalProgressRing(
    state: GoalProgressRingState,
    modifier: Modifier = Modifier
) {
    ChartSurface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val diameter = size.width - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)
                    
                    drawArc(
                        color = BurnMateColors.Divider,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    if (state.progressFraction > 0) {
                        val progressColor = if (state.isGoalReached) BurnMateColors.Success else BurnMateColors.AccentPrimary
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = 360f * state.progressFraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.progressLabel,
                        style = BurnMateTypography.displaySmall,
                        color = BurnMateColors.TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Text(
                text = state.supportingLabel,
                style = BurnMateTypography.bodyMedium,
                color = BurnMateColors.TextSecondary
            )
        }
    }
}
