package com.example.eas

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.eas.data.AppDatabase
import com.example.eas.data.Member
import com.example.eas.data.TransactionHistory
import com.example.eas.ui.theme.EASTheme
import com.example.eas.viewmodel.CoffeeBlissViewModel
import kotlinx.coroutines.delay

val GoldText = Color(0xFFE9C168)
val DarkGreen = Color(0xFF103529)

class MainActivity : ComponentActivity() {
    private val viewModel: CoffeeBlissViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(applicationContext)
                val sharedPrefs = applicationContext.getSharedPreferences("CoffeeBlissPrefs", Context.MODE_PRIVATE)
                return CoffeeBlissViewModel(db.coffeeBlissDao(), sharedPrefs) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EASTheme { CoffeeBlissApp(viewModel) } }
    }
}

@Composable
fun CoffeeBlissApp(viewModel: CoffeeBlissViewModel) {
    val navController = rememberNavController()
    val memberId by viewModel.currentMemberId.collectAsState()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen {
                if (memberId == null) {
                    navController.navigate("auth") { popUpTo("splash") { inclusive = true } }
                } else {
                    navController.navigate("main") { popUpTo("splash") { inclusive = true } }
                }
            }
        }
        composable("auth") {
            AuthScreen(viewModel, onAuthSuccess = {
                navController.navigate("main") { popUpTo("auth") { inclusive = true } }
            })
        }
        composable("main") {
            MainDashboardScreen(viewModel, onLogout = {
                navController.navigate("auth") { popUpTo("main") { inclusive = true } }
            })
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(Unit) { delay(3000); currentOnTimeout() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Welcome to Bliss Coffee\nMembership", style = MaterialTheme.typography.headlineLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun AuthScreen(viewModel: CoffeeBlissViewModel, onAuthSuccess: () -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    val authError by viewModel.authError.collectAsState()
    val memberId by viewModel.currentMemberId.collectAsState()

    LaunchedEffect(memberId) { if (memberId != null) onAuthSuccess() }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isLoginMode) "Masuk ke Akun" else "Daftar Akun Baru", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoginMode) {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

                authError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.login(email, password) }, modifier = Modifier.fillMaxWidth()) { Text("Login") }
                TextButton(onClick = { isLoginMode = false }) { Text("Belum punya akun? Daftar di sini") }
            } else {
                var name by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var phone by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No HP") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password Baru") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { if(name.isNotBlank() && email.isNotBlank() && password.isNotBlank()) viewModel.registerMember(name, email, phone, password) }, modifier = Modifier.fillMaxWidth()) { Text("Daftar") }
                TextButton(onClick = { isLoginMode = true }) { Text("Sudah punya akun? Login di sini") }
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: Int) {
    CARD("Kartu", R.drawable.ic_home),
    TRANSACTION("Transaksi", R.drawable.ic_favorite),
    REDEEM("Redeem", R.drawable.ic_favorite),
    PROFILE("Profil", R.drawable.ic_account_box)
}

@Composable
fun MainDashboardScreen(viewModel: CoffeeBlissViewModel, onLogout: () -> Unit) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CARD) }
    val memberId by viewModel.currentMemberId.collectAsState()

    LaunchedEffect(memberId) { if (memberId == null) onLogout() }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(icon = { Icon(painterResource(it.icon), contentDescription = it.label) }, label = { Text(it.label) }, selected = it == currentDestination, onClick = { currentDestination = it })
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                when (currentDestination) {
                    AppDestinations.CARD -> DigitalCardScreen(viewModel)
                    AppDestinations.TRANSACTION -> TransactionScreen(viewModel)
                    AppDestinations.REDEEM -> RedeemScreen(viewModel)
                    AppDestinations.PROFILE -> ProfileScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun DigitalCardScreen(viewModel: CoffeeBlissViewModel) {
    val memberId by viewModel.currentMemberId.collectAsState()
    var member by remember { mutableStateOf<Member?>(null) }

    LaunchedEffect(memberId) { memberId?.let { viewModel.getCurrentMember()?.collect { member = it } } }

    Surface(color = Color(0xFFF7F1E6), modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("MY MEMBERSHIP CARD", color = DarkGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(0.6f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = DarkGreen), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COFFEE BLISS", color = GoldText, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Text("MEMBER", color = GoldText, fontWeight = FontWeight.Normal, fontSize = 18.sp, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(member?.name ?: "NAMA MEMBER", color = GoldText, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                        Text("ID : MBR00${member?.id}", color = GoldText, fontWeight = FontWeight.Normal, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.weight(1.5f))


                    Image(
                        painter = painterResource(id = R.drawable.ic_coffee_logo),
                        contentDescription = "Coffee Bliss Logo",
                        modifier = Modifier
                            .size(140.dp)
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TransactionScreen(viewModel: CoffeeBlissViewModel) {
    val memberId by viewModel.currentMemberId.collectAsState()
    var transactions by remember { mutableStateOf<List<TransactionHistory>>(emptyList()) }
    var inputAmount by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var calculatedPoints by remember { mutableStateOf(0) }

    LaunchedEffect(memberId) { memberId?.let { viewModel.getTransactions()?.collect { transactions = it } } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tambah Transaksi", style = MaterialTheme.typography.titleLarge)
        Text("Aturan: Kelipatan Rp 10.000 = 1 Poin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

        OutlinedTextField(value = inputAmount, onValueChange = { inputAmount = it; errorMessage = null }, label = { Text("Nominal (Rp)") }, modifier = Modifier.fillMaxWidth(), isError = errorMessage != null)
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Button(onClick = {
            val amount = inputAmount.toDoubleOrNull() ?: 0.0
            if (amount < 10000) {
                errorMessage = "Minimal pembelian Rp 10.000 untuk mendapatkan poin!"
            } else {
                calculatedPoints = (amount / 10000).toInt()
                viewModel.addTransaction(amount)
                inputAmount = ""
                errorMessage = null
                showSuccessDialog = true
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Simpan Transaksi") }

        HorizontalDivider()
        Text("Riwayat Transaksi", style = MaterialTheme.typography.titleMedium)
        LazyColumn { items(transactions) { tx -> ListItem(headlineContent = { Text("Pembelian Rp ${tx.amount}") }, supportingContent = { Text("+${tx.pointsEarned} Poin") }, trailingContent = { Text(tx.date) }) } }
    }

    if (showSuccessDialog) {
        AlertDialog(onDismissRequest = { showSuccessDialog = false }, confirmButton = { Button(onClick = { showSuccessDialog = false }) { Text("OK") } }, title = { Text("Transaksi Berhasil!") }, text = { Text("Transaksi disimpan. Selamat, Anda mendapatkan +$calculatedPoints Poin baru!") })
    }
}

@Composable
fun RedeemScreen(viewModel: CoffeeBlissViewModel) {
    val memberId by viewModel.currentMemberId.collectAsState()
    var member by remember { mutableStateOf<Member?>(null) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    var rewardName by remember { mutableStateOf("") }

    LaunchedEffect(memberId) { memberId?.let { viewModel.getCurrentMember()?.collect { member = it } } }
    val currentPoints = member?.totalPoints ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tukar Poin Anda", style = MaterialTheme.typography.headlineMedium)
        Text("Poin Saat Ini: $currentPoints Poin", style = MaterialTheme.typography.bodyLarge)

        Button(onClick = { viewModel.redeemPoints(50); rewardName = "Espresso"; showRedeemDialog = true }, modifier = Modifier.fillMaxWidth(), enabled = currentPoints >= 50) { Text("Tukar 50 Poin - Espresso") }
        Button(onClick = { viewModel.redeemPoints(100); rewardName = "Cappuccino"; showRedeemDialog = true }, modifier = Modifier.fillMaxWidth(), enabled = currentPoints >= 100) { Text("Tukar 100 Poin - Cappuccino") }
        Button(onClick = { viewModel.redeemPoints(150); rewardName = "Latte"; showRedeemDialog = true }, modifier = Modifier.fillMaxWidth(), enabled = currentPoints >= 150) { Text("Tukar 150 Poin - Latte Gratis") }
    }

    if (showRedeemDialog) {
        AlertDialog(onDismissRequest = { showRedeemDialog = false }, confirmButton = { Button(onClick = { showRedeemDialog = false }) { Text("OK") } }, title = { Text("Redeem Berhasil!") }, text = { Text("Penukaran poin sukses! Hadiah $rewardName gratis Anda sedang disiapkan.") })
    }
}

@Composable
fun ProfileScreen(viewModel: CoffeeBlissViewModel) {
    val memberId by viewModel.currentMemberId.collectAsState()
    var member by remember { mutableStateOf<Member?>(null) }

    LaunchedEffect(memberId) { memberId?.let { viewModel.getCurrentMember()?.collect { member = it } } }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Profil Akun", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Nama: ${member?.name}", style = MaterialTheme.typography.bodyLarge)
        Text("Email: ${member?.email}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { viewModel.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Logout") }
    }
}