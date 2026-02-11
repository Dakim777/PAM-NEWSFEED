package com.example.newsfeeddaffa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RawNews(
    val id: Int,
    val title: String,
    val category: String,
    val timestamp: Long
)

data class NewsUiModel(
    val id: Int,
    val displayTitle: String,
    val categoryColor: Color,
    val timeFormatted: String,
    val category: String,
    var isRead: Boolean = false,
    var detailContent: String? = null,
    var isLoading: Boolean = false,
    var isExpanded: Boolean = false
)

class NewsRepository {
    private var counter = 0
    private val categories = listOf("Teknologi", "Olahraga", "Bisnis", "Politik")

    val newsStream: Flow<RawNews> = flow {
        while (true) {
            delay(2000)
            counter++
            val randomCategory = categories.random()
            emit(RawNews(
                id = counter,
                title = "Berita #$counter: Update Terkini $randomCategory",
                category = randomCategory,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    suspend fun fetchNewsDetail(id: Int): String {
        delay(1000)
        return "Ini adalah detail lengkap untuk berita nomor $id. \n\n" +
                "Analisis mendalam menunjukkan bahwa tren di sektor ini sedang meningkat pesat. " +
                "Para ahli menyarankan untuk memantau perkembangan lebih lanjut dalam beberapa hari ke depan."
    }
}

class NewsViewModel : ViewModel() {
    private val repository = NewsRepository()
    private val _newsList = MutableStateFlow<List<NewsUiModel>>(emptyList())
    val newsList: StateFlow<List<NewsUiModel>> = _newsList.asStateFlow()

    private val _readCount = MutableStateFlow(0)
    val readCount: StateFlow<Int> = _readCount.asStateFlow()

    private val _activeFilter = MutableStateFlow<String?>(null)
    val activeFilter: StateFlow<String?> = _activeFilter.asStateFlow()

    init {
        viewModelScope.launch {
            repository.newsStream
                .map { raw ->
                    NewsUiModel(
                        id = raw.id,
                        displayTitle = raw.title.uppercase(),
                        categoryColor = getCategoryColor(raw.category),
                        timeFormatted = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(raw.timestamp)),
                        category = raw.category
                    )
                }
                .collect { newItem ->
                    _newsList.update { listOf(newItem) + it }
                }
        }
    }

    fun setFilter(category: String?) {
        _activeFilter.value = category
    }

    fun onNewsClicked(newsItem: NewsUiModel) {
        if (newsItem.isLoading) return

        if (newsItem.detailContent != null) {
            updateItem(newsItem.id) { it.copy(isExpanded = !it.isExpanded) }
            return
        }

        viewModelScope.launch {
            updateItem(newsItem.id) { it.copy(isLoading = true) }
            val detail = repository.fetchNewsDetail(newsItem.id)
            updateItem(newsItem.id) {
                it.copy(
                    isLoading = false,
                    isRead = true,
                    detailContent = detail,
                    isExpanded = true
                )
            }
            if (!newsItem.isRead) {
                _readCount.update { it + 1 }
            }
        }
    }

    private fun updateItem(id: Int, update: (NewsUiModel) -> NewsUiModel) {
        _newsList.update { list ->
            list.map { if (it.id == id) update(it) else it }
        }
    }

    private fun getCategoryColor(category: String): Color {
        return when(category) {
            "Teknologi" -> Color(0xFF2196F3)
            "Olahraga" -> Color(0xFF4CAF50)
            "Bisnis" -> Color(0xFFF44336)
            "Politik" -> Color(0xFFFF9800)
            else -> Color.Gray
        }
    }
}

@Composable
fun NewsFeedScreen(viewModel: NewsViewModel = viewModel()) {
    val allNews by viewModel.newsList.collectAsState()
    val readCount by viewModel.readCount.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()

    val displayedNews = remember(allNews, activeFilter) {
        if (activeFilter == null) allNews
        else allNews.filter { it.category == activeFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text("News Feed Simulator", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Berita Dibaca: $readCount", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterButton("Semua", activeFilter == null) { viewModel.setFilter(null) }
            FilterButton("Tekno", activeFilter == "Teknologi") { viewModel.setFilter("Teknologi") }
            FilterButton("Olahraga", activeFilter == "Olahraga") { viewModel.setFilter("Olahraga") }
            FilterButton("Bisnis", activeFilter == "Bisnis") { viewModel.setFilter("Bisnis") }
            FilterButton("Politik", activeFilter == "Politik") { viewModel.setFilter("Politik") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (displayedNews.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Menunggu berita masuk...", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayedNews, key = { it.id }) { news ->
                    NewsCard(news = news, onClick = { viewModel.onNewsClicked(news) })
                }
            }
        }
    }
}

@Composable
fun FilterButton(text: String, isActive: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun NewsCard(news: NewsUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (news.isRead) Color(0xFFF0F0F0) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = news.categoryColor,
                    modifier = Modifier.size(10.dp),
                    shape = MaterialTheme.shapes.small
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(news.category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = news.categoryColor)
                Spacer(modifier = Modifier.weight(1f))
                Text(news.timeFormatted, fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(news.displayTitle, fontSize = 16.sp, fontWeight = FontWeight.Medium)

            if (news.isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Mengambil detail berita...", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }

            AnimatedVisibility(
                visible = news.isExpanded && news.detailContent != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = news.detailContent ?: "",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.DarkGray,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}