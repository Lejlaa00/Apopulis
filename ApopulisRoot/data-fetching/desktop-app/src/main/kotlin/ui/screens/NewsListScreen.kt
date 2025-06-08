package ui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.font.FontWeight
import model.NewsItem
import ui.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import java.net.URL
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun NewsListScreen(
    news: List<NewsItem>,
    onEdit: (NewsItem) -> Unit,
    onDelete: (NewsItem) -> Unit,
    onNavigate: (String) -> Unit
) {
    SidebarWrapper(currentScreen = "newsList", onNavigate = onNavigate) {
        var selectedCategory by remember { mutableStateOf("All") }
        var toDeleteItem by remember { mutableStateOf<NewsItem?>(null) }
        var expandedId by remember { mutableStateOf<String?>(null) }
        var selectedItem by remember { mutableStateOf<NewsItem?>(null) }
        var keyword by remember { mutableStateOf("") }

        val categories = listOf("All") + news.mapNotNull { it.category }.distinct()

        val keywordsMap = remember(news) { org.example.nlp.TfidfCalculator.computeTopKeywords(news) }

        val filteredNews = news.filter { item ->
            val keywordMatch = keyword.isBlank() ||
                    item.title.contains(keyword, ignoreCase = true) ||
                    item.content?.contains(keyword, ignoreCase = true) == true ||
                    keywordsMap[item]?.any { it.contains(keyword, ignoreCase = true) } == true

            val categoryMatch = selectedCategory == "All" || item.category == selectedCategory

            keywordMatch && categoryMatch
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.BgDarkest)
                .padding(24.dp)
        )
        {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                backgroundColor = AppColors.BgLight,
                elevation = 8.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CategoryDropdownField(
                            categories = categories,
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            label = { Text("Search by keyword") },
                            singleLine = true,
                            trailingIcon = {
                                if (keyword.isNotEmpty()) {
                                    IconButton(onClick = { keyword = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear keyword",
                                            tint = AppColors.Icon
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextWhite,
                                backgroundColor = AppColors.BgDarker,
                                cursorColor = AppColors.Accent,
                                focusedBorderColor = AppColors.Accent,
                                unfocusedBorderColor = AppColors.Divider,
                                focusedLabelColor = AppColors.TextLight,
                                unfocusedLabelColor = AppColors.TextMuted
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filteredNews.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No news items match the selected filters.")
                }
            } else {
                LazyColumn {
                    items(filteredNews) { item ->
                        val isExpanded = expandedId == item.url
                        var hovered by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .pointerMoveFilter(
                                    onEnter = {
                                        hovered = true
                                        false
                                    },
                                    onExit = {
                                        hovered = false
                                        false
                                    }
                                )
                                .clickable { selectedItem = item },
                            backgroundColor = if (hovered) AppColors.HoverBg else AppColors.BgLight,
                            elevation = if (hovered) 8.dp else 4.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {

                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.h6,
                                    color = AppColors.TextWhite
                                )

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Published at: ${item.publishedAt.toLocalDate()}",
                                            style = MaterialTheme.typography.body2,
                                            color = AppColors.TextMuted
                                        )
                                        item.category?.let {
                                            Text("Category: $it", style = MaterialTheme.typography.body2, color = AppColors.TextMuted)
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = { onEdit(item) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppColors.Icon)
                                        }
                                        IconButton(onClick = { toDeleteItem = item }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AppColors.Icon)
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedItem != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            selectedItem?.let {
                NewsDetailDialog(item = it, onClose = { selectedItem = null })
            }
        }


        if (toDeleteItem != null) {
            AlertDialog(
                onDismissRequest = { toDeleteItem = null },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete this news item?") },
                confirmButton = {
                    TextButton(onClick = {
                        toDeleteItem?.let { onDelete(it) }
                        toDeleteItem = null
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { toDeleteItem = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun NewsDetailDialog(item: NewsItem, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            backgroundColor = AppColors.BgLight,
            elevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.End
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onClose,
                        interactionSource = interactionSource
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppColors.Accent
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(top = 60.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.h6,
                        color = AppColors.TextWhite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                        item.author?.let {
                            InfoRowIcon(Icons.Default.Person, "Author:", it)
                        }
                        item.location?.let {
                            InfoRowIcon(Icons.Default.Place, "Location:", it)
                        }
                        InfoRowIcon(Icons.Default.Link, "Source:", item.source)
                    }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp)
                                .background(AppColors.Divider)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Divider(color = AppColors.Divider)
                    Spacer(Modifier.height(12.dp))

                    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                    val imageUrl = item.imageUrl ?: "https://picsum.photos/600/500"

                    LaunchedEffect(imageUrl) {
                        imageBitmap = loadImageBitmapFromUrl(imageUrl)
                    }

                    imageBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "News Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 600.dp, height = 500.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(MaterialTheme.shapes.medium)
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    item.content?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.body1.copy(
                                color = AppColors.TextLight,
                                lineHeight = 20.sp,
                                letterSpacing = 0.15.sp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            softWrap = true
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                }
            }
        }
    }
}

@Composable
fun InfoRowIcon(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AppColors.Icon,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.body2,
            color = AppColors.TextLight
        )
    }
}

@Composable
fun InfoRowGray(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
            color = AppColors.TextMuted
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            color = AppColors.TextMuted
        )
    }
}

suspend fun loadImageBitmapFromUrl(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    return@withContext try {
        val bytes = URL(url).readBytes()
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        println("Failed to load image: ${e.message}")
        null
    }
}

@Composable
fun DateInputField(
    value: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var text by remember { mutableStateOf(value?.format(formatter) ?: "") }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it.toLocalDateOrNull())
        },
        label = { Text("Search by date") },
        singleLine = true,
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = {
                    text = ""
                    onChange(null)
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear date")
                }
            }
        },
        modifier = modifier,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = AppColors.TextWhite,
            backgroundColor = AppColors.BgDarker,
            cursorColor = AppColors.Accent,
            focusedBorderColor = AppColors.Accent,
            unfocusedBorderColor = AppColors.Divider,
            focusedLabelColor = AppColors.TextLight,
            unfocusedLabelColor = AppColors.TextMuted
        )
    )
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun CategoryDropdownField(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Open category menu",
                        tint = AppColors.Icon // vidljivija strelica
                    )
                }
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = AppColors.TextWhite,
                backgroundColor = AppColors.BgDarker,
                cursorColor = AppColors.Accent,
                focusedBorderColor = AppColors.Accent,
                unfocusedBorderColor = AppColors.Divider,
                focusedLabelColor = AppColors.TextLight,
                unfocusedLabelColor = AppColors.TextMuted
            ),
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(AppColors.BgDarker)
        ) {
            categories.forEach { category ->
                var hovered by remember { mutableStateOf(false) }

                DropdownMenuItem(
                    onClick = {
                        onSelect(category)
                        expanded = false
                    },
                    modifier = Modifier
                        .background(if (hovered) AppColors.HoverBg else Color.Transparent)
                        .pointerMoveFilter(
                            onEnter = {
                                hovered = true
                                false
                            },
                            onExit = {
                                hovered = false
                                false
                            }
                        )
                ) {
                    Text(category, color = AppColors.TextWhite)
                }
            }

        }

    }
}

fun String.toLocalDateOrNull(): LocalDate? =
    try {
        LocalDate.parse(this)
    } catch (e: Exception) {
        null
    }

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.body2
        )
    }
}

