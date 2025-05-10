package ui.screens

import androidx.compose.runtime.Composable
import model.NewsItem

@Composable
fun AddNewsScreen(onSave: (NewsItem) -> Unit, onBack: () -> Unit) {
    NewsEditScreen(item = null, onSave = onSave, onBack = onBack)
}
