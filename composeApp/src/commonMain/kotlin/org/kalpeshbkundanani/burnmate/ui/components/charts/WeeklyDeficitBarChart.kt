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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.DailyBalanceDirection
import org.kalpeshbkundanani.burnmate.presentation.dashboard.charts.WeeklyDeficitChartState
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun WeeklyDeficitBarChart(
    state: WeeklyDeficitChartState,
    modifier: Modifier = Modifier
) {
    ChartSurface(modifier = modifier) {
        Column {
            Text(
                text = "Weekly Deficit",
                style = BurnMateTypography.labelLarge,
                color = BurnMateColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val bars = state.bars
                if (bars.isEmpty()) return@Box
                
                Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)) {
                    val width = size.width
                    val height = size.height
                    
                    val maxMag = state.maxMagnitudeCalories.toFloat()
                    val valueRange = if (maxMag == 0f) 1f else maxMag * 2f
                    
                    val baselineY = height / 2f
                    val barWidth = (width / bars.size) * 0.6f
                    val spacing = (width / bars.size) * 0.4f
                    
                    drawLine(
                        color = BurnMateColors.Divider,
                        start = Offset(0f, baselineY),
                        end = Offset(width, baselineY),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    bars.forEachIndexed { index, bar ->
                        val x = index * (barWidth + spacing) + (spacing / 2)
                        val normalizedHeight = (kotlin.math.abs(bar.deltaCalories) / valueRange) * height
                        
                        val barColor = when (bar.direction) {
                            DailyBalanceDirection.Deficit -> BurnMateColors.Success
                            DailyBalanceDirection.Surplus -> BurnMateColors.Error
                            DailyBalanceDirection.Neutral -> BurnMateColors.TextSecondary
                        }
                        
                        val y = if (bar.deltaCalories < 0) {
                            baselineY
                        } else {
                            baselineY - normalizedHeight
                        }
                        
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, normalizedHeight.coerceAtLeast(2.dp.toPx())),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = bars.first().label,
                        style = BurnMateTypography.labelSmall,
                        color = BurnMateColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (bars.size > 1) {
                        Text(
                            text = bars.last().label,
                            style = BurnMateTypography.labelSmall,
                            color = BurnMateColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
