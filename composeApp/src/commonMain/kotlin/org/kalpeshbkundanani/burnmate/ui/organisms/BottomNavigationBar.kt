package org.kalpeshbkundanani.burnmate.ui.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

enum class NavigationTab {
    HOME, ACTIVITY, STATS, PROFILE
}

@Composable
fun BottomNavigationBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BurnMateColors.SurfaceGlass)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        NavIcon(
            icon = Icons.Filled.Home,
            contentDescription = "Home",
            isSelected = currentTab == NavigationTab.HOME,
            onClick = { onTabSelected(NavigationTab.HOME) }
        )
        NavIcon(
            icon = Icons.Filled.History,
            contentDescription = "Activity",
            isSelected = currentTab == NavigationTab.ACTIVITY,
            onClick = { onTabSelected(NavigationTab.ACTIVITY) }
        )
        NavIcon(
            icon = Icons.Filled.Insights,
            contentDescription = "Stats",
            isSelected = currentTab == NavigationTab.STATS,
            onClick = { onTabSelected(NavigationTab.STATS) }
        )
        NavIcon(
            icon = Icons.Filled.AccountCircle,
            contentDescription = "Profile",
            isSelected = currentTab == NavigationTab.PROFILE,
            onClick = { onTabSelected(NavigationTab.PROFILE) }
        )
    }
}

@Composable
private fun NavIcon(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconToggleButton(
        checked = isSelected,
        onCheckedChange = { onClick() },
        colors = IconButtonDefaults.iconToggleButtonColors(
            contentColor = BurnMateColors.TextSecondary,
            checkedContentColor = BurnMateColors.AccentPrimary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}
