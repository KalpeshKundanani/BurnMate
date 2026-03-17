package org.kalpeshbkundanani.burnmate.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.WeightTrendChartState
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun WeightTrendChart(
    state: WeightTrendChartState,
    modifier: Modifier = Modifier
) {
    ChartSurface(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Weight Trend",
                    style = BurnMateTypography.labelLarge,
                    color = BurnMateColors.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = state.latestValueLabel,
                    style = BurnMateTypography.titleMedium,
                    color = BurnMateColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val points = state.points
                if (points.isEmpty()) return@Box

                val minWeight = state.minWeightKg.toFloat()
                val maxWeight = state.maxWeightKg.toFloat()
                
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)) {
                    val width = size.width
                    val height = size.height
                    
                    val xStep = if (points.size > 1) width / (points.size - 1) else width
                    val range = maxWeight - minWeight
                    val valueRange = if (range == 0f) 1f else range
                    
                    val path = Path()
                    var lastX = 0f
                    var lastY = 0f
                    
                    points.forEachIndexed { index, point ->
                        val x = index * xStep
                        val normalizedY = 1f - ((point.weightKg.toFloat() - minWeight) / valueRange)
                        val y = if (range == 0f) height / 2f else normalizedY * height
                        
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                        
                        lastX = x
                        lastY = y
                    }
                    
                    drawPath(
                        path = path,
                        color = BurnMateColors.AccentPrimary,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    
                    drawCircle(
                        color = BurnMateColors.AccentPrimary,
                        radius = 5.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                    drawCircle(
                        color = BurnMateColors.BackgroundPrimary,
                        radius = 2.dp.toPx(),
                        center = Offset(lastX, lastY)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = points.first().label,
                        style = BurnMateTypography.labelSmall,
                        color = BurnMateColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (points.size > 1) {
                        Text(
                            text = points.last().label,
                            style = BurnMateTypography.labelSmall,
                            color = BurnMateColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
