package org.kalpeshbkundanani.burnmate.presentation.dashboard.charts

enum class ChartRangeOption(
    val days: Int,
    val label: String
) {
    Last7Days(days = 7, label = "7D"),
    Last14Days(days = 14, label = "14D"),
    Last30Days(days = 30, label = "30D")
}
