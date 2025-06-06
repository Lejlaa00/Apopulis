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



@Composable
fun DataGeneratorScreen(onGenerate: (List<NewsItem>) -> Unit, onNavigate: (String) -> Unit) {
    SidebarWrapper(currentScreen = "generator", onNavigate = onNavigate) {
        val faker = remember { Faker() }
        var count by remember { mutableStateOf("5") }
        var category by remember { mutableStateOf("") }
        var startDate by remember { mutableStateOf("2025-01-01T00:00") }
        var endDate by remember { mutableStateOf("2025-12-31T23:59") }

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = count,
                onValueChange = { count = it },
                label = { Text("Number of news") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Button(onClick = {
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
                        category = category, // dodaćemo kasnije ako prazno
                        tags = listOf(faker.animal.name(), faker.color.name())
                    )
                }

                // Dodaj kategoriju i lokaciju + pošalji u bazu
                val enrichedItems = rawItems.map { item ->
                    val fixedCategory = if (item.category.isNullOrBlank()) Categorizer.categorizeByText(item) else item.category
                    val location = Categorizer.extractLocationByText(item)
                    val enriched = item.copy(category = fixedCategory, location = location)

                    scraper.NewsSender.send(enriched) // šaljemo u bazu
                    enriched
                }

                onGenerate(enrichedItems)
            }) {
                Text("Generate")
            }

        }
    }
}
