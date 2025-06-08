package ui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.serpro69.kfaker.Faker
import model.NewsItem
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import org.example.nlp.Categorizer
import scraper.NewsSender
import ui.ui.theme.AppColors
import androidx.compose.ui.Alignment

@Composable
fun DataGeneratorScreen(onGenerate: (List<NewsItem>) -> Unit, onNavigate: (String) -> Unit) {
    SidebarWrapper(currentScreen = "generator", onNavigate = onNavigate) {
        val faker = remember { Faker() }
        var count by remember { mutableStateOf("5") }
        var category by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf("2025-01-01T00:00") }
        var endDate by remember { mutableStateOf("2025-12-31T23:59") }
        var isGenerating by remember { mutableStateOf(false) }
        var generatedCount by remember { mutableStateOf(0) }
        val categories = listOf("gospodarstvo", "splošno", "politika", "vreme", "biznis", "kultura", "lifestyle", "šport", "tehnologija")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                text = "News Generator",
                style = MaterialTheme.typography.h5,
                color = AppColors.TextWhite
            )
            Spacer(Modifier.height(12.dp))

            Card(
                backgroundColor = AppColors.BgLight,
                elevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    InputField("Number of news", count) { count = it }
                    var expanded by remember { mutableStateOf(false) }
                    Text("Category (optional)", color = AppColors.TextLight)
                    Box {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { expanded = true },
                            enabled = false,
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = AppColors.TextLight
                                )
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextWhite,
                                backgroundColor = AppColors.BgDarker,
                                disabledTextColor = AppColors.TextWhite,
                                disabledBorderColor = AppColors.Divider,
                                disabledLabelColor = AppColors.TextMuted,
                                cursorColor = AppColors.Accent
                            )
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .background(AppColors.BgDarkest)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    onClick = {
                                        category = cat
                                        expanded = false
                                    },
                                    modifier = Modifier.background(AppColors.BgDarkest)
                                ) {
                                    Text(cat, color = AppColors.TextWhite)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isGenerating = true
                            generatedCount = 0
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                            val from = try { LocalDateTime.parse(startDate, formatter) } catch (e: Exception) { LocalDateTime.now().minusDays(30) }
                            val to = try { LocalDateTime.parse(endDate, formatter) } catch (e: Exception) { LocalDateTime.now() }

                            val rawItems = (1..(count.toIntOrNull() ?: 0)).map {

                                val nowEpoch = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                                val fromEpoch = from.toEpochSecond(ZoneOffset.UTC)
                                val toEpoch = minOf(to.toEpochSecond(ZoneOffset.UTC), nowEpoch)

                                val randomEpoch = if (toEpoch > fromEpoch) {
                                    Random.nextLong(fromEpoch, toEpoch)
                                } else {
                                    fromEpoch
                                }

                                val randomTime = LocalDateTime.ofEpochSecond(randomEpoch, 0, ZoneOffset.UTC)


                                val fakeParagraphs = List(5) {
                                    List(50) { faker.lorem.words() }.joinToString(" ").replaceFirstChar { it.uppercaseChar() } + "."
                                }.joinToString("\n\n")
                                NewsItem(
                                    title = faker.book.title(),
                                    content = "${faker.book.title()}\n\n$fakeParagraphs",
                                    author = faker.book.author(),
                                    source = "generator.si",
                                    url = "http://generator.si/$it",
                                    imageUrl = "https://picsum.photos/seed/${it}/600/400",
                                    publishedAt = randomTime,
                                    category = category,
                                    tags = listOf(faker.color.name(), faker.animal.name())
                                )
                            }

                            val enrichedItems = rawItems.map { item ->
                                val fixedCategory = if (item.category.isNullOrBlank()) Categorizer.categorizeByText(item) else item.category
                                val location = Categorizer.extractLocationByText(item)
                                val enriched = item.copy(category = fixedCategory, location = location)
                                NewsSender.send(enriched)
                                enriched
                            }

                            generatedCount = enrichedItems.size
                            isGenerating = false
                            onGenerate(enrichedItems)
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Accent)
                    ) {
                        Text("Generate")
                    }

                    if (isGenerating) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = AppColors.Accent,
                                strokeWidth = 3.dp,
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(end = 12.dp)
                            )
                            Text(
                                text = "Generating news...",
                                style = MaterialTheme.typography.body2,
                                color = AppColors.TextMuted
                            )
                        }
                    } else if (generatedCount > 0) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Generated $generatedCount news items.",
                            style = MaterialTheme.typography.body2,
                        )
                    }
                }
            }
        }
    }
}

