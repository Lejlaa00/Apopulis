package ui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import ui.ui.theme.AppColors

@Composable
fun SidebarWrapper(
    currentScreen: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    var sidebarVisible by remember { mutableStateOf(true) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .background(AppColors.BgDarkest)
                .width(if (sidebarVisible) 220.dp else 40.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = { sidebarVisible = !sidebarVisible }) {
                    Icon(
                        imageVector = if (sidebarVisible) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                        contentDescription = "Toggle sidebar",
                        tint = AppColors.Icon
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            SidebarItem("Home", "home", Icons.Default.Home, currentScreen, onNavigate, sidebarVisible)
            SidebarItem("News List", "newsList", Icons.Default.List, currentScreen, onNavigate, sidebarVisible)

            Divider(
                color = AppColors.Divider,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SidebarItem("Add News", "addNews", Icons.Default.Add, currentScreen, onNavigate, sidebarVisible)
            SidebarItem("Scraper", "scraper", Icons.Default.Refresh, currentScreen, onNavigate, sidebarVisible)
            SidebarItem("Generate Data", "generator", Icons.Default.Build, currentScreen, onNavigate, sidebarVisible)

        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BgDarkest)
                .padding(24.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SidebarItem(
    label: String,
    screenKey: String,
    icon: ImageVector,
    currentScreen: String,
    onNavigate: (String) -> Unit,
    visible: Boolean
) {
    val isSelected = screenKey == currentScreen
    val backgroundColor = if (isSelected) AppColors.ActiveBg else Color.Transparent
    val textColor = if (isSelected) AppColors.TextWhite else AppColors.TextMuted
    val iconSize = if (visible) 23.dp else 23.dp
    val rowModifier = Modifier
        .fillMaxWidth()
        .background(backgroundColor)
        .clickable { onNavigate(screenKey) }
        .padding(vertical = 12.dp, horizontal = if (visible) 16.dp else 0.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (visible) Arrangement.Start else Arrangement.Center,
        modifier = rowModifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AppColors.Icon,
            modifier = Modifier.size(iconSize)
        )

        if (visible) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, color = textColor)
        }
    }

}

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    SidebarWrapper(currentScreen = "home", onNavigate = onNavigate) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Welcome to News App")
        }
    }
}