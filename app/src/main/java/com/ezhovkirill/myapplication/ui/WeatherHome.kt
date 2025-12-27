package com.ezhovkirill.myapplication.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ezhovkirill.myapplication.R

// Image Constants (kept for reference, though we use R.drawable mostly now)
const val imgBigMoonCloudMidRain = "https://www.figma.com/api/mcp/asset/6c12a22a-5e2c-467c-b3a3-baeb7af2cb31"
const val img = "https://www.figma.com/api/mcp/asset/8ec54272-c60f-4a72-9c87-fc6836fb8e50"
const val imgHouse = "https://www.figma.com/api/mcp/asset/fb811e19-742b-4ccc-9beb-7ecfd7c8feb4"
const val img1 = "https://www.figma.com/api/mcp/asset/7604c847-3a01-4e1a-ab3b-6cb5b3f3f7a4"
const val imgEllipse1 = "https://www.figma.com/api/mcp/asset/14839f9d-dbd7-453d-9404-fc8bd850c5c4"
const val img2 = "https://www.figma.com/api/mcp/asset/836352dd-e460-4e27-be94-bd85ea841a81"
const val img3 = "https://www.figma.com/api/mcp/asset/4c755fd6-6992-4250-9639-e3e2c387970d"
const val imgOutline = "https://www.figma.com/api/mcp/asset/b82ef9c9-c680-4e94-8040-dde33b9cd3db"
const val imgBatteryEnd = "https://www.figma.com/api/mcp/asset/c7c92c91-e25c-46f4-be0b-26bf9ba7e97a"
const val imgFill = "https://www.figma.com/api/mcp/asset/89b65ccf-c6e0-4334-8998-f2f054a20b18"
const val imgWifi = "https://www.figma.com/api/mcp/asset/ea1e77c3-1ff5-4fe6-a373-177b83352004"
const val imgIconMobileSignal = "https://www.figma.com/api/mcp/asset/164c81f1-0655-4173-ba06-32963c8db612"
const val imgRectangle = "https://www.figma.com/api/mcp/asset/90c090ca-0b21-4fd0-9f48-23f887c92bc9"
const val imgEllipse2 = "https://www.figma.com/api/mcp/asset/7ddf506f-1402-45c8-bd06-6e49285a9a49"
const val imgEllipse3 = "https://www.figma.com/api/mcp/asset/7649db10-498e-41c4-a846-e86b1bb5e424"
const val imgSeparator = "https://www.figma.com/api/mcp/asset/8473386f-0817-415d-8cca-3265328e1a54"
const val imgUnderline = "https://www.figma.com/api/mcp/asset/454839f6-dcb6-4bf3-995f-3c198012e3ad"
const val imgIndicator = "https://www.figma.com/api/mcp/asset/ab037005-cf59-4a6f-b14c-6ba622d1de4d"
const val imgRectangle364 = "https://www.figma.com/api/mcp/asset/ff1888d9-fe87-4855-9ff2-5597e5917fc0"
const val imgSubtract = "https://www.figma.com/api/mcp/asset/e64b3f46-0605-4392-9b83-b3f9e942e165"
const val imgEllipse4 = "https://www.figma.com/api/mcp/asset/8671b071-ef52-4b98-be7f-850a1c41d300"
const val imgEllipse5 = "https://www.figma.com/api/mcp/asset/06c59c87-ca02-4879-be24-3c75c0f56f05"
const val imgEllipseBlur = "https://www.figma.com/api/mcp/asset/9f7bf44a-b1ab-4827-bc96-12cfa537b47c"
const val imgEllipseBlur1 = "https://www.figma.com/api/mcp/asset/a092756f-41e7-442f-b5e9-74fdbff6f55b"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherHome(viewModel: WeatherViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.fetchLocationAndWeather()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    if (showSearchDialog) {
        Dialog(
            onDismissRequest = { showSearchDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showSearchDialog = false }
            ) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { 
                        viewModel.searchCity(it)
                    },
                    active = true,
                    onActiveChange = { 
                        if (!it) showSearchDialog = false
                    },
                    placeholder = { Text("Поиск города...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = SearchBarDefaults.colors(containerColor = Color.White),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    LazyColumn {
                        val itemsToShow = if (query.isEmpty()) uiState.suggestedCities else uiState.searchResults
                        items(itemsToShow) { result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectCity(result)
                                        showSearchDialog = false
                                        query = ""
                                    }
                                    .padding(16.dp)
                            ) {
                                Text(text = result.name, fontWeight = FontWeight.Bold)
                                Text(text = result.description, style = TextStyle(color = Color.Gray))
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2E335A), Color(0xFF1C1B33)),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // Background Image
        AsyncImage(
            model = R.drawable.img_background,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // House
        AsyncImage(
            model = R.drawable.img_house,
            contentDescription = "House",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = 304.dp)
        )

        // Weather Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 98.dp)
        ) {
            Text(
                text = uiState.city,
                style = TextStyle(
                    fontSize = 34.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal
                )
            )
            Text(
                text = uiState.currentTemp,
                style = TextStyle(
                    fontSize = 96.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Thin
                )
            )
            Text(
                text = uiState.condition,
                style = TextStyle(
                    fontSize = 20.sp,
                    color = Color(0x99EBEBF5),
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = uiState.highLow,
                style = TextStyle(
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Modal / Bottom Sheet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(325.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Backgrounds
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2E335A).copy(alpha = 0.8f),
                                Color(0xFF1C1B33).copy(alpha = 0.9f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp)
                    )
            )
            AsyncImage(
                model = R.drawable.img_rectangle,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // Hourly Forecast
            Column(modifier = Modifier.padding(top = 69.dp)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.hourly) { item ->
                        HourlyItem(item = item)
                    }
                }
            }

            // Segmented Control (Mock)
            Box(modifier = Modifier.fillMaxWidth().height(49.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Почасовой прогноз", color = Color(0x99EBEBF5), modifier = Modifier.padding(start = 32.dp))
                    Text("Прогноз на неделю", color = Color(0x99EBEBF5), modifier = Modifier.padding(end = 32.dp))
                }
                AsyncImage(model = R.drawable.img_underline, contentDescription = null, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth())
                AsyncImage(model = R.drawable.img_indicator, contentDescription = null, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        // Tab Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Main Bar Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2E335A))
                )
                AsyncImage(
                    model = R.drawable.img_rectangle_364,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
            
            // Floating Button Background
            AsyncImage(
                model = R.drawable.img_subtract,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 258.dp, height = 100.dp)
            )

            // Tab Bar Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(horizontal = 32.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Location",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { showSearchDialog = true }
                )
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "List",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Plus Button
             Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 4.dp)
                    .size(64.dp)
            ) {
                 AsyncImage(
                    model = R.drawable.img_ellipse_4,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                 AsyncImage(
                    model = R.drawable.img_ellipse_5,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(3.dp)
                )
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color(0xFF48319D),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun HourlyItem(item: HourlyUiItem) {
    val backgroundColor = if (item.isActive) Color(0xFF48319D) else Color(0x3348319D)
    val borderColor = if (item.isActive) Color(0x80FFFFFF) else Color(0x33FFFFFF)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(60.dp)
            .height(146.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(30.dp))
            .padding(vertical = 16.dp)
    ) {
        Text(text = item.time, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        
        // Map weather code to icon (simplified logic)
        // In a real app, you'd map item.iconRes (which is weather code) to a specific drawable
        val iconRes = when(item.iconRes) {
            0, 1 -> R.drawable.img_small_sun_cloud_mid_rain // Placeholder for clear/sun
            else -> R.drawable.img_big_moon_cloud_mid_rain // Placeholder for rain/cloud
        }
        
        AsyncImage(
            model = iconRes,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Text(text = item.temp, color = Color.White, fontSize = 20.sp)
    }
}
