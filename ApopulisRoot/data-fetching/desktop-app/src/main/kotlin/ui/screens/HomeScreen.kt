package ui.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import ui.ui.theme.AppColors
import java.time.LocalDate

@Composable
fun SidebarWrapper(
    currentScreen: String,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    var sidebarVisible by remember { mutableStateOf(true) }
    val logoBitmap = remember { loadImageBitmapFromResource("logo.png") }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .background(AppColors.BgDarkest)
                .width(if (sidebarVisible) 220.dp else 40.dp)
                .fillMaxHeight()
        ) {
            if (sidebarVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        logoBitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .height(80.dp)
                                    .width(180.dp)
                            )
                        }

                        IconButton(onClick = { sidebarVisible = false }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Hide sidebar",
                                tint = AppColors.Icon
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { sidebarVisible = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Show sidebar",
                            tint = AppColors.Icon
                        )
                    }
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

        Divider(
            color = AppColors.HoverBg, // ili neka specifična nijansa sive
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp) // tanje nego 1.dp
        )

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

fun loadImageBitmapFromResource(path: String): ImageBitmap? {
    return try {
        val stream = object {}.javaClass.classLoader.getResourceAsStream(path)
        val bytes = stream?.readBytes() ?: return null
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        println("Failed to load image '$path': ${e.message}")
        null
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
    val iconSize = 23.dp
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Daily Summary",
                style = MaterialTheme.typography.h5,
                color = AppColors.TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Date: ${LocalDate.now()}",
                style = MaterialTheme.typography.body2,
                color = AppColors.TextMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                backgroundColor = AppColors.BgLight,
                elevation = 6.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Summary of today's news will appear here...",
                        style = MaterialTheme.typography.body1,
                        color = AppColors.TextLight
                    )
                }
            }
        }
    }
}

