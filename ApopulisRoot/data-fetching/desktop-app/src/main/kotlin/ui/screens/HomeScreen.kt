package ui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LightSidebarColor = Color(0xFFF7F7F8)
val LightSidebarSelected = Color(0xFFE0E0E0)
val LightTextColor = Color(0xFF333333)
val LightInactiveTextColor = Color(0xFF333333)
val LightBorderColor = Color(0xFFD0D0D0)

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
                .background(LightSidebarColor)
                .width(if (sidebarVisible) 220.dp else 40.dp)
                .fillMaxHeight()
        ) {
            // Strelica gore desno
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .clickable { sidebarVisible = !sidebarVisible }
                        .padding(12.dp)
                ) {
                    Text(if (sidebarVisible) "←" else "→", color = LightTextColor)
                }
            }

            if (sidebarVisible) {
                Spacer(Modifier.height(8.dp))
                SidebarItem("News List", "newsList", currentScreen, onNavigate)
                SidebarItem("Add News", "addNews", currentScreen, onNavigate)
                SidebarItem("Scraper", "scraper", currentScreen, onNavigate)
                SidebarItem("Generate Data", "generator", currentScreen, onNavigate)

                Divider(
                    color = LightBorderColor,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                SidebarItem("Home", "home", currentScreen, onNavigate)
            }
        }

        // Glavni sadržaj
        Box(
            modifier = Modifier
                .fillMaxSize()
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
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val isSelected = screenKey == currentScreen
    val backgroundColor = if (isSelected) LightSidebarSelected else Color.Transparent
    val textColor = if (isSelected) LightTextColor else LightInactiveTextColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onNavigate(screenKey) }
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Text(label, color = textColor)
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