package ui.ui.screens

import androidx.compose.runtime.Composable
import org.example.model.NewsItem

@Composable
fun AddNewsScreen(
    onSave: (NewsItem) -> Unit,
    onNavigate: (String) -> Unit
) {
    NewsEditScreen(
        item = null,
        onSave = onSave,
        onNavigate = onNavigate,
        showBackButton = false
    )
}
