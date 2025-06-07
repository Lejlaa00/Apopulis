package ui.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
                    InputField("Category (optional)", category) { category = it }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isGenerating = true
                            generatedCount = 0
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                            val from = try { LocalDateTime.parse(startDate, formatter) } catch (e: Exception) { LocalDateTime.now().minusDays(30) }
                            val to = try { LocalDateTime.parse(endDate, formatter) } catch (e: Exception) { LocalDateTime.now() }

                            val rawItems = (1..(count.toIntOrNull() ?: 0)).map {
                                val randomTime = from.plusSeconds(Random.nextLong(0, to.toEpochSecond(ZoneOffset.UTC) - from.toEpochSecond(ZoneOffset.UTC)))
                                NewsItem(
                                    title = faker.book.title(),
                                    content = faker.lorem.words(),
                                    author = faker.name.name(),
                                    source = "generator.si",
                                    url = "http://generator.si/$it",
                                    publishedAt = randomTime,
                                    imageUrl = "https://placekitten.com/${300 + it}/200",
                                    category = category,
                                    tags = listOf(faker.animal.name(), faker.color.name())
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

