package com.sahyadrisiri.watermapper

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContent {
            SahyadriTheme {
                AppRoot()
            }
        }
    }
}

private val Navy = Color(0xFF0D3B66)
private val Blue = Color(0xFF00639A)
private val Teal = Color(0xFF0EB898)
private val Mist = Color(0xFFF4FAFF)
private val Pale = Color(0xFFE6F6FF)
private val ErrorRed = Color(0xFFBA1A1A)
private val Ink = Color(0xFF001F2A)
private val Muted = Color(0xFF63758A)

@Composable
private fun SahyadriTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Navy,
            secondary = Blue,
            tertiary = Teal,
            background = Mist,
            surface = Color.White,
            onPrimary = Color.White,
            onSurface = Ink
        ),
        content = content
    )
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val repo = remember { LocalRepository(context) }
    var session by remember { mutableStateOf(repo.currentSession()) }

    if (session == null) {
        AuthScreen(
            onAuthenticated = {
                session = it
            },
            repo = repo
        )
    } else {
        MainShell(
            repo = repo,
            user = session!!,
            onLogout = {
                repo.logout()
                session = null
            }
        )
    }
}

@Composable
private fun AuthScreen(repo: LocalRepository, onAuthenticated: (User) -> Unit) {
    val scope = rememberCoroutineScope()
    var registerMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Mist, Color(0xFFC7F2E6), Color(0xFFD9F2FF))))
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bolt, null, tint = Navy, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Text("Sahyadri-Siri", color = Navy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium)
            }
            Text("Community water health map for streams, wells, and field reports.", color = Muted)

            Surface(
                color = Color.White.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (registerMode) "Create account" else "Welcome back", fontWeight = FontWeight.Bold, color = Ink)
                    AnimatedVisibility(registerMode) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    OutlinedTextField(value = email, onValueChange = { email = it.trim() }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall) }
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Navy),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        onClick = {
                            loading = true
                            error = null
                            info = null
                            scope.launch {
                                val result = if (registerMode) repo.register(name, email, password) else repo.login(email, password)
                                loading = false
                                result.onSuccess(onAuthenticated).onFailure { error = it.message }
                            }
                        }
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp) else Text(if (registerMode) "Register" else "Sign in")
                    }
                    TextButton(onClick = {
                        registerMode = !registerMode
                        error = null
                        info = null
                    }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(if (registerMode) "Already have an account? Sign in" else "New here? Create account")
                    }
                    if (!registerMode) {
                        TextButton(
                            enabled = email.isNotBlank() && !loading,
                            onClick = {
                                loading = true
                                error = null
                                info = null
                                scope.launch {
                                    repo.resetPassword(email)
                                        .onSuccess { info = "Password reset email sent to ${email.trim()}." }
                                        .onFailure { error = it.message }
                                    loading = false
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Forgot password?")
                        }
                    }
                    info?.let { Text(it, color = Teal, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun MainShell(repo: LocalRepository, user: User, onLogout: () -> Unit) {
    var activeUser by remember { mutableStateOf(user) }
    var selectedTab by remember { mutableStateOf(AppTab.Map) }
    val reports = remember { mutableStateListOf<WaterReport>().apply { addAll(repo.loadReports()) } }
    var showReportSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Mist,
        bottomBar = {
            NavigationBar(containerColor = Color.White.copy(alpha = 0.96f), tonalElevation = 10.dp) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label, maxLines = 1) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == AppTab.Map || selectedTab == AppTab.Reports || selectedTab == AppTab.Dashboard) {
                FloatingActionButton(containerColor = Navy, contentColor = Color.White, onClick = { showReportSheet = true }) {
                    Icon(Icons.Default.Add, "New report")
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                AppTab.Map -> MapScreen(reports)
                AppTab.Dashboard -> DashboardScreen(reports, onNewReport = { showReportSheet = true })
                AppTab.Reports -> ReportsScreen(reports, onNewReport = { showReportSheet = true })
                AppTab.Chat -> ChatScreen(user = activeUser, reports = reports)
                AppTab.Profile -> ProfileScreen(
                    user = activeUser,
                    reports = reports,
                    repo = repo,
                    onUserChanged = { activeUser = it },
                    onLogout = onLogout
                )
            }
        }
    }

    if (showReportSheet) {
        NewReportSheet(
            repo = repo,
            onDismiss = { showReportSheet = false },
            onSaved = {
                reports.add(0, it)
                repo.saveReports(reports)
                showReportSheet = false
            }
        )
    }
}

@Composable
private fun TopBar(title: String, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Bolt, null, tint = Navy)
        Spacer(Modifier.width(10.dp))
        Text(title, color = Navy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun MapScreen(reports: List<WaterReport>) {
    var query by remember { mutableStateOf("") }
    val visibleReports = if (query.isBlank()) reports else reports.filter {
        it.place.contains(query, ignoreCase = true) ||
            it.summary.contains(query, ignoreCase = true) ||
            it.status.label.contains(query, ignoreCase = true)
    }

    Box(Modifier.fillMaxSize()) {
        OsmMap(visibleReports)
        Column(Modifier.fillMaxWidth()) {
            TopBar("Sahyadri-Siri") {
                IconButton(onClick = {}) { Icon(Icons.Default.Search, "Search", tint = Navy) }
            }
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 22.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = Muted)
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        decorationBox = { inner ->
                            if (query.isBlank()) Text("Search streams or locations...", color = Muted)
                            inner()
                        }
                    )
                    Icon(Icons.Default.Tune, null, tint = Muted)
                }
            }
        }

        Legend(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp)
        )
    }
}

@Composable
private fun OsmMap(reports: List<WaterReport>) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(8.4)
                controller.setCenter(GeoPoint(13.35, 75.45))
            }
        },
        update = { map ->
            map.overlays.removeAll { it is Marker }
            seedReports(reports).forEach { report ->
                val marker = Marker(map)
                marker.position = GeoPoint(report.latitude, report.longitude)
                marker.title = report.place
                marker.subDescription = "${report.aqiLabel} - ${report.status}"
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(marker)
            }
            map.invalidate()
        }
    )
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Surface(color = Color.White.copy(alpha = 0.88f), shape = RoundedCornerShape(8.dp), shadowElevation = 8.dp, modifier = modifier.width(210.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("WATER QUALITY", color = Muted, fontWeight = FontWeight.Medium)
            LegendItem(Teal, "Pristine Source")
            LegendItem(Blue, "Fair Quality")
            LegendItem(ErrorRed, "Action Required")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Ink)
    }
}

@Composable
private fun ReportsScreen(reports: List<WaterReport>, onNewReport: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("Reports")
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedButton(onClick = onNewReport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ImageSearch, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload image for AI analysis")
                }
            }
            items(reports) { ReportCard(it) }
        }
    }
}

@Composable
private fun DashboardScreen(reports: List<WaterReport>, onNewReport: () -> Unit) {
    val pristine = reports.count { it.status == WaterStatus.Pristine }
    val fair = reports.count { it.status == WaterStatus.Fair }
    val action = reports.count { it.status == WaterStatus.ActionRequired }
    val total = reports.size.coerceAtLeast(1)
    val average = reports.mapNotNull { it.aqiLabel.filter(Char::isDigit).toIntOrNull() }.average().takeIf { !it.isNaN() } ?: 0.0

    Column(Modifier.fillMaxSize()) {
        TopBar("Dashboard")
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Reports", reports.size.toString(), Modifier.weight(1f))
                    MetricCard("Avg AQI", average.toInt().toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Pristine", pristine.toString(), Modifier.weight(1f))
                    MetricCard("Action", action.toString(), Modifier.weight(1f))
                }
            }
            item {
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Water Quality Mix", color = Navy, fontWeight = FontWeight.ExtraBold)
                        QualityPieChart(
                            values = listOf(pristine, fair, action),
                            colors = listOf(Teal, Blue, ErrorRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                        )
                        LegendItem(Teal, "Pristine: ${(pristine * 100) / total}%")
                        LegendItem(Blue, "Fair: ${(fair * 100) / total}%")
                        LegendItem(ErrorRed, "Action required: ${(action * 100) / total}%")
                    }
                }
            }
            item {
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("AQI By Location", color = Navy, fontWeight = FontWeight.ExtraBold)
                        AqiBarChart(reports.take(6), Modifier.fillMaxWidth().height(210.dp))
                    }
                }
            }
            item {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Navy), modifier = Modifier.fillMaxWidth(), onClick = onNewReport) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add AI analyzed report")
                }
            }
        }
    }
}

@Composable
private fun QualityPieChart(values: List<Int>, colors: List<Color>, modifier: Modifier = Modifier) {
    val total = values.sum().coerceAtLeast(1)
    Canvas(modifier) {
        var startAngle = -90f
        values.forEachIndexed { index, value ->
            val sweep = 360f * value / total
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = Offset((size.width - size.height) / 2f, 0f),
                size = Size(size.height, size.height)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun AqiBarChart(reports: List<WaterReport>, modifier: Modifier = Modifier) {
    val chartReports = if (reports.isEmpty()) sampleReports() else reports
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        chartReports.forEach { report ->
            val aqi = report.aqiLabel.filter(Char::isDigit).toIntOrNull() ?: 45
            val fraction = (aqi / 100f).coerceIn(0.08f, 1f)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Spacer(Modifier.weight(1f - fraction))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(fraction)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(Brush.verticalGradient(listOf(report.statusColor, report.statusColor.copy(alpha = 0.55f))))
                )
                Spacer(Modifier.height(8.dp))
                Text(aqi.toString(), color = report.statusColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text(report.place.take(8), color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReportCard(report: WaterReport) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                QualityDot(report.statusColor)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(report.place, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(report.createdAt, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text(report.aqiLabel, color = report.statusColor, fontWeight = FontWeight.ExtraBold)
            }
            report.imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(Uri.parse(it)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Text(report.summary, color = Ink)
            Text("Suggested action: ${report.action}", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QualityDot(color: Color) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.WaterDrop, null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CommunityScreen(reports: List<WaterReport>) {
    val actionCount = reports.count { it.status == WaterStatus.ActionRequired }
    Column(Modifier.fillMaxSize()) {
        TopBar("Community")
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MetricRow(reports.size, actionCount) }
            item {
                Text("Recent activity", color = Muted, fontWeight = FontWeight.Bold)
            }
            items(reports.take(8)) {
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 1.dp) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, null, tint = Navy)
                        Spacer(Modifier.width(12.dp))
                        Text("${it.place}: ${it.status.label} water report shared.", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(user: User, reports: List<WaterReport>) {
    val scope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(
            ChatMessage("assistant", "Hi ${user.name}. I can help you use Sahyadri-Siri, upload reports, understand AI analysis, find dashboard insights, or troubleshoot login/profile issues.")
        )
    }
    var draft by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Assistant") {
            Icon(Icons.Default.SmartToy, null, tint = Navy)
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (loading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Navy)
                        Spacer(Modifier.width(8.dp))
                        Text("Thinking...", color = Muted)
                    }
                }
            }
        }
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Ask anything") },
                    modifier = Modifier.weight(1f),
                    minLines = 1,
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    enabled = draft.isNotBlank() && !loading,
                    onClick = {
                        val question = draft.trim()
                        draft = ""
                        messages.add(ChatMessage("user", question))
                        loading = true
                        scope.launch {
                            val answer = GeminiClient.chat(question, reports, BuildConfig.GEMINI_API_KEY)
                                .getOrElse { localAssistantAnswer(question, reports) }
                            messages.add(ChatMessage("assistant", answer))
                            loading = false
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, "Send", tint = Navy)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isUser) Navy else Color.White,
            contentColor = if (isUser) Color.White else Ink,
            shape = RoundedCornerShape(8.dp),
            shadowElevation = if (isUser) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Text(message.text, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun MetricRow(total: Int, actionCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard("Reports", total.toString(), Modifier.weight(1f))
        MetricCard("Needs action", actionCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 3.dp, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, color = Navy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
            Text(label, color = Muted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(
    user: User,
    reports: List<WaterReport>,
    repo: LocalRepository,
    onUserChanged: (User) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editOpen by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var draftName by remember(user.name) { mutableStateOf(user.name) }
    var message by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val savedUri = repo.persistProfilePhoto(context, uri)
        if (savedUri != null) {
            val updated = user.copy(photoUri = savedUri)
            repo.updateProfile(updated)
            onUserChanged(updated)
            message = "Profile photo updated."
        } else {
            message = "Could not save profile photo."
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Profile") {
            IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout", tint = Navy) }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Pale)
                            .clickable { photoPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.photoUri.isNullOrBlank()) {
                            Icon(Icons.Default.Person, null, tint = Navy, modifier = Modifier.size(34.dp))
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(Uri.parse(user.photoUri)),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.name, fontWeight = FontWeight.Bold)
                        Text(user.email, color = Muted)
                        Text("Tap photo to change", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ProfileAction(Icons.Default.Edit, "Edit profile", "Change your display name") { editOpen = true }
            ProfileAction(Icons.Default.ImageSearch, "Add profile pic", "Choose a photo from your gallery") { photoPicker.launch("image/*") }
            ProfileAction(Icons.Default.History, "History", "View your saved water reports") { historyOpen = true }
            ProfileAction(Icons.Default.Badge, "Complete profile", "Name, email, and profile photo status") {
                message = if (user.photoUri.isNullOrBlank()) "Add a profile photo to complete your profile." else "Your profile is complete."
            }
            ProfileAction(Icons.Default.Logout, "Logout", "Sign out from this phone") { onLogout() }
            message?.let { Text(it, color = if (it.contains("complete", true) || it.contains("updated", true)) Teal else Muted) }
        }
    }

    if (editOpen) {
        ModalBottomSheet(onDismissRequest = { editOpen = false }, containerColor = Mist) {
            Column(Modifier.padding(18.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit profile", color = Navy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = draftName, onValueChange = { draftName = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(
                    enabled = draftName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val updated = user.copy(name = draftName.trim())
                        scope.launch {
                            repo.updateProfile(updated)
                            onUserChanged(updated)
                            message = "Profile updated."
                            editOpen = false
                        }
                    }
                ) { Text("Save changes") }
            }
        }
    }

    if (historyOpen) {
        ModalBottomSheet(onDismissRequest = { historyOpen = false }, containerColor = Mist) {
            Column(Modifier.padding(18.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("History", color = Navy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                if (reports.isEmpty()) {
                    Text("No reports yet.", color = Muted)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(420.dp)) {
                        items(reports) { report -> ReportCard(report) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAction(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Pale), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Navy)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Ink)
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewReportSheet(repo: LocalRepository, onDismiss: () -> Unit, onSaved: (WaterReport) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var place by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var analysis by remember { mutableStateOf<AnalysisResult?>(null) }
    var analyzing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
        analysis = null
        error = null
        if (uri != null) {
            analyzing = true
            scope.launch {
                val result = GeminiClient.analyze(context, uri, BuildConfig.GEMINI_API_KEY)
                analyzing = false
                result.onSuccess { analysis = it }.onFailure { error = it.message }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Mist) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("New water report", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = Navy)
            OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text("Location or stream name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Field notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clickable { picker.launch("image/*") },
                shadowElevation = 2.dp
            ) {
                if (imageUri == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.ImageSearch, null, tint = Navy, modifier = Modifier.size(38.dp))
                        Text("Choose water photo", color = Muted)
                    }
                } else {
                    Image(rememberAsyncImagePainter(imageUri), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            if (analyzing) {
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Blue, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Analyzing uploaded image...", color = Muted)
                    }
                }
            }
            error?.let { Text(it, color = ErrorRed) }
            analysis?.let {
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("${it.aqiLabel} - ${it.status.label}", color = it.status.color, fontWeight = FontWeight.Bold)
                        Text(it.summary)
                        Text("Action: ${it.action}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = onDismiss) { Text("Cancel") }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !analyzing && imageUri != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    onClick = {
                        val uri = imageUri ?: return@Button
                        analyzing = true
                        error = null
                        scope.launch {
                            val result = GeminiClient.analyze(context, uri, BuildConfig.GEMINI_API_KEY)
                            analyzing = false
                            result.onSuccess { analysis = it }.onFailure { error = it.message }
                        }
                    }
                ) {
                    Text(if (analysis == null) "Analyze" else "Re-analyze")
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = place.isNotBlank() && analysis != null && !analyzing,
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                onClick = {
                    val result = analysis ?: AnalysisResult("Checking...", WaterStatus.Fair, notes.ifBlank { "Report saved for community review." }, "Verify on site and add a lab reading when available.")
                    onSaved(
                        WaterReport(
                            id = System.currentTimeMillis().toString(),
                            place = place,
                            summary = result.summary,
                            action = result.action,
                            aqiLabel = result.aqiLabel,
                            status = result.status,
                            latitude = 13.35 + Math.random() * 1.2 - 0.6,
                            longitude = 75.45 + Math.random() * 1.2 - 0.6,
                            imageUri = imageUri?.let { repo.persistImage(context, it) },
                            createdAt = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date())
                        )
                    )
                }
            ) {
                Text("Save report")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private enum class AppTab(val label: String, val icon: ImageVector) {
    Map("Map", Icons.Default.Map),
    Dashboard("Dashboard", Icons.Default.Dashboard),
    Reports("Reports", Icons.Default.Analytics),
    Chat("Chat", Icons.Default.SmartToy),
    Profile("Profile", Icons.Default.Person)
}

private data class User(val name: String, val email: String, val photoUri: String? = null)
private data class ChatMessage(val role: String, val text: String)

private enum class WaterStatus(val label: String, val color: Color) {
    Pristine("Pristine", Teal),
    Fair("Fair", Blue),
    ActionRequired("Action required", ErrorRed)
}

private data class WaterReport(
    val id: String,
    val place: String,
    val summary: String,
    val action: String,
    val aqiLabel: String,
    val status: WaterStatus,
    val latitude: Double,
    val longitude: Double,
    val imageUri: String?,
    val createdAt: String
) {
    val statusColor: Color get() = status.color
}

private data class AnalysisResult(
    val aqiLabel: String,
    val status: WaterStatus,
    val summary: String,
    val action: String
)

private class LocalRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("sahyadri_siri", Context.MODE_PRIVATE)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun currentSession(): User? {
        val firebaseUser = auth.currentUser
        val email = firebaseUser?.email ?: prefs.getString("session_email", null) ?: return null
        return User(
            name = prefs.getString("user_name_$email", firebaseUser?.displayName ?: "Field Reporter") ?: "Field Reporter",
            email = email,
            photoUri = prefs.getString("user_photo_$email", null)
        )
    }

    suspend fun register(name: String, email: String, password: String): Result<User> {
        if (name.isBlank() || email.isBlank() || password.length < 6) return Result.failure(IllegalArgumentException("Enter a name, email, and a password with at least 6 characters."))
        return runCatching {
            auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = User(name.trim(), email.trim())
            saveSession(user)
            runCatching {
                firestore.collection("users").document(email.trim()).set(
                    mapOf("name" to user.name, "email" to user.email, "photoUri" to (user.photoUri ?: ""))
                ).await()
            }
            user
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        if (email.isBlank() || password.isBlank()) return Result.failure(IllegalArgumentException("Enter email and password."))
        return runCatching {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            val cleanEmail = email.trim()
            val user = User(
                name = prefs.getString("user_name_$cleanEmail", "Field Reporter") ?: "Field Reporter",
                email = cleanEmail,
                photoUri = prefs.getString("user_photo_$cleanEmail", null)
            )
            saveSession(user)
            user
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        if (email.isBlank()) return Result.failure(IllegalArgumentException("Enter your email first."))
        return runCatching {
            auth.sendPasswordResetEmail(email.trim()).await()
        }
    }

    fun logout() {
        auth.signOut()
        prefs.edit().remove("session_email").apply()
    }

    fun updateProfile(user: User) {
        saveSession(user)
        runCatching {
            firestore.collection("users").document(user.email).set(
                mapOf("name" to user.name, "email" to user.email, "photoUri" to (user.photoUri ?: ""))
            )
        }
    }

    private fun saveSession(user: User) {
        prefs.edit()
            .putString("session_email", user.email)
            .putString("user_name_${user.email}", user.name)
            .putString("user_photo_${user.email}", user.photoUri)
            .apply()
    }

    fun persistImage(context: Context, uri: Uri): String? = runCatching {
        val file = File(context.filesDir, "report_${System.currentTimeMillis()}.jpg")
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
        inputStream.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        Uri.fromFile(file).toString()
    }.getOrNull()

    fun persistProfilePhoto(context: Context, uri: Uri): String? = runCatching {
        val file = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
        inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        Uri.fromFile(file).toString()
    }.getOrNull()

    fun loadReports(): List<WaterReport> {
        val raw = prefs.getString("reports", null) ?: return sampleReports()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                WaterReport(
                    id = item.getString("id"),
                    place = item.getString("place"),
                    summary = item.getString("summary"),
                    action = item.getString("action"),
                    aqiLabel = item.getString("aqiLabel"),
                    status = WaterStatus.valueOf(item.getString("status")),
                    latitude = item.getDouble("latitude"),
                    longitude = item.getDouble("longitude"),
                    imageUri = item.optString("imageUri").ifBlank { null },
                    createdAt = item.getString("createdAt")
                )
            }
        }.getOrElse { sampleReports() }
    }

    fun saveReports(reports: List<WaterReport>) {
        val array = JSONArray()
        reports.forEach {
            array.put(JSONObject().apply {
                put("id", it.id)
                put("place", it.place)
                put("summary", it.summary)
                put("action", it.action)
                put("aqiLabel", it.aqiLabel)
                put("status", it.status.name)
                put("latitude", it.latitude)
                put("longitude", it.longitude)
                put("imageUri", it.imageUri ?: "")
                put("createdAt", it.createdAt)
            })
        }
        prefs.edit().putString("reports", array.toString()).apply()
        runCatching {
            reports.take(50).forEach {
                firestore.collection("reports").document(it.id).set(
                    mapOf(
                        "id" to it.id,
                        "place" to it.place,
                        "summary" to it.summary,
                        "action" to it.action,
                        "aqiLabel" to it.aqiLabel,
                        "status" to it.status.name,
                        "latitude" to it.latitude,
                        "longitude" to it.longitude,
                        "imageUri" to (it.imageUri ?: ""),
                        "createdAt" to it.createdAt
                    )
                )
            }
        }
    }
}

private object GeminiClient {
    private val client = OkHttpClient()

    suspend fun analyze(context: Context, uri: Uri, apiKey: String): Result<AnalysisResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) error("Gemini API key is not configured in the build.")
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Could not read selected image.")
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val prompt = """
                Analyze this water body photo for visible water health clues. Return compact JSON only with:
                aqiLabel, status, summary, action.
                status must be one of Pristine, Fair, ActionRequired.
                aqiLabel should be like "98 AQI", "62 AQI", or "Checking..." when visual evidence is weak.
            """.trimIndent()
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                    .put(JSONObject().put("text", prompt))
                    .put(JSONObject().put("inline_data", JSONObject()
                        .put("mime_type", mime)
                        .put("data", base64)
                    ))
                )))
                .toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Gemini analysis failed: HTTP ${response.code}.")
                val text = JSONObject(response.body?.string().orEmpty())
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val json = JSONObject(text)
                AnalysisResult(
                    aqiLabel = json.optString("aqiLabel", "Checking..."),
                    status = runCatching { WaterStatus.valueOf(json.optString("status", "Fair")) }.getOrDefault(WaterStatus.Fair),
                    summary = json.optString("summary", "Photo analyzed for visible water clarity, color, and surface contamination."),
                    action = json.optString("action", "Collect a follow-up sample and monitor the site.")
                )
            }
        }
    }

    suspend fun chat(question: String, reports: List<WaterReport>, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (apiKey.isBlank()) error("Gemini API key is not configured in the build.")
            val reportSummary = reports.take(8).joinToString("\n") {
                "- ${it.place}: ${it.aqiLabel}, ${it.status.label}, ${it.summary}"
            }
            val prompt = """
                You are Sahyadri-Siri's in-app assistant. Answer briefly and practically.
                You can guide users on login, forgot password, profile photo, edit profile, uploading a water report,
                AI image analysis, dashboard charts, map markers, and report history.

                Current reports:
                $reportSummary

                User question: $question
            """.trimIndent()
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                    .put(JSONObject().put("text", prompt))
                )))
                .toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Assistant failed: HTTP ${response.code}.")
                JSONObject(response.body?.string().orEmpty())
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()
            }
        }
    }
}

private fun localAssistantAnswer(question: String, reports: List<WaterReport>): String {
    val q = question.lowercase()
    return when {
        "password" in q || "forgot" in q -> "On the login screen, enter your email and tap Forgot password. Firebase will send a reset link to that email."
        "profile" in q || "photo" in q -> "Open Profile. Tap the profile picture area to add a photo, or use Edit profile to change your name."
        "upload" in q || "report" in q || "analysis" in q -> "Tap the + button or open Reports, choose a water photo, and the app will automatically run AI analysis. Add the location name, review the result, then save."
        "dashboard" in q || "chart" in q -> "Open Dashboard to see total reports, average AQI, water quality mix, and AQI by location charts."
        "map" in q -> "Open Map to see report markers. Use the search bar to filter locations, summaries, or quality status."
        "logout" in q -> "Open Profile and tap Logout. The app will only ask for login again after you log out."
        else -> "I can help with login, forgot password, profile, upload/report analysis, map, dashboard charts, and history. You currently have ${reports.size} reports saved."
    }
}

private fun seedReports(reports: List<WaterReport>): List<WaterReport> = if (reports.isEmpty()) sampleReports() else reports

private fun sampleReports(): List<WaterReport> = listOf(
    WaterReport("1", "Tunga Headstream", "Clear flow with low visual contamination.", "Protect source and monitor monthly.", "98 AQI", WaterStatus.Pristine, 13.73, 75.25, null, "09 May 2026, 9:30 AM"),
    WaterReport("2", "Village Check Dam", "Turbid water and possible runoff near the bank.", "Alert local group and collect a field test.", "32 AQI", WaterStatus.ActionRequired, 13.14, 75.65, null, "09 May 2026, 10:10 AM"),
    WaterReport("3", "Areca Farm Stream", "Moderate clarity. Awaiting stronger evidence.", "Recheck after rainfall and add pH/TDS reading.", "Checking...", WaterStatus.Fair, 13.42, 75.82, null, "09 May 2026, 11:00 AM")
)
