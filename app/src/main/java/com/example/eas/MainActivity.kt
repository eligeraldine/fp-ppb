package com.example.eas

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.eas.data.AppDatabase
import com.example.eas.data.Member
import com.example.eas.data.TransactionHistory
import com.example.eas.ui.theme.*
import com.example.eas.viewmodel.CoffeeBlissViewModel
import com.example.eas.viewmodel.TeaProduct
import kotlinx.coroutines.delay

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
        setContent { EASTheme { TeaBlissApp(viewModel) } }
    }
}

@Composable
fun TeaBlissApp(viewModel: CoffeeBlissViewModel) {
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
    LaunchedEffect(Unit) { delay(2500); currentOnTimeout() }

    Surface(modifier = Modifier.fillMaxSize(), color = SoftCream) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocalDrink,
                contentDescription = "Tea Logo",
                tint = MatchaGreen,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Tea Bliss",
                style = MaterialTheme.typography.displaySmall,
                color = DarkLeaf,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Membership",
                style = MaterialTheme.typography.titleMedium,
                color = TeaAmber,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun AuthScreen(viewModel: CoffeeBlissViewModel, onAuthSuccess: () -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    val authError by viewModel.authError.collectAsState()
    val memberId by viewModel.currentMemberId.collectAsState()

    LaunchedEffect(memberId) { if (memberId != null) onAuthSuccess() }

    Scaffold(containerColor = SoftCream) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 32.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                (if (isLoginMode) "Selamat Datang" else "Bergabung Bersama Kami"),
                style = MaterialTheme.typography.headlineMedium,
                color = DarkLeaf,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                (if (isLoginMode) "Silakan masuk untuk melanjutkan" else "Daftarkan akun Tea Bliss kamu"),
                style = MaterialTheme.typography.bodyMedium,
                color = DarkLeaf.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isLoginMode) {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                StyledTextField(value = email, onValueChange = { email = it }, label = "Email")
                Spacer(modifier = Modifier.height(16.dp))
                StyledTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)

                authError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

                Spacer(modifier = Modifier.height(32.dp))
                StyledButton(text = "Masuk", onClick = { viewModel.login(email, password) })
                TextButton(onClick = { isLoginMode = false }) { Text("Belum punya akun? Daftar", color = DarkLeaf) }
            } else {
                var name by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var phone by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }

                StyledTextField(value = name, onValueChange = { name = it }, label = "Nama Lengkap")
                Spacer(modifier = Modifier.height(16.dp))
                StyledTextField(value = email, onValueChange = { email = it }, label = "Email")
                Spacer(modifier = Modifier.height(16.dp))
                StyledTextField(value = phone, onValueChange = { phone = it }, label = "No HP", isNumber = true)
                Spacer(modifier = Modifier.height(16.dp))
                StyledTextField(value = password, onValueChange = { password = it }, label = "Password Baru", isPassword = true)

                Spacer(modifier = Modifier.height(32.dp))
                StyledButton(text = "Daftar", onClick = {
                    if (name.isNotBlank() && email.isNotBlank() && password.isNotBlank())
                        viewModel.registerMember(name, email, phone, password)
                })
                TextButton(onClick = { isLoginMode = true }) { Text("Sudah punya akun? Masuk", color = DarkLeaf) }
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    CARD("Member", Icons.Default.CreditCard),
    MENU("Menu", Icons.Default.RestaurantMenu),
    HISTORY("Riwayat", Icons.Default.History),
    REDEEM("Redeem", Icons.Default.CardGiftcard),
    PROFILE("Profil", Icons.Default.Person)
}

@Composable
fun MainDashboardScreen(viewModel: CoffeeBlissViewModel, onLogout: () -> Unit) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CARD) }
    val memberId by viewModel.currentMemberId.collectAsState()

    LaunchedEffect(memberId) { if (memberId == null) onLogout() }

    Scaffold(
        containerColor = SoftCream,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                contentColor = DarkLeaf,
                tonalElevation = 8.dp,
                modifier = Modifier.height(80.dp)
            ) {
                AppDestinations.entries.forEach { dest ->
                    NavigationBarItem(
                        icon = { Icon(dest.icon, contentDescription = dest.label, modifier = Modifier.size(24.dp)) },
                        label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                        selected = currentDestination == dest,
                        onClick = { currentDestination = dest },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SurfaceWhite,
                            selectedTextColor = DarkLeaf,
                            indicatorColor = MatchaGreen,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentDestination) {
                AppDestinations.CARD -> DigitalCardScreen(viewModel) { currentDestination = it }
                AppDestinations.MENU -> OrderMenuScreen(viewModel)
                AppDestinations.HISTORY -> HistoryScreen(viewModel)
                AppDestinations.REDEEM -> RedeemScreen(viewModel)
                AppDestinations.PROFILE -> ProfileScreen(viewModel)
            }
        }
    }
}

@Composable
fun DigitalCardScreen(viewModel: CoffeeBlissViewModel, onNavigate: (AppDestinations) -> Unit) {
    val memberId by viewModel.currentMemberId.collectAsState()
    var member by remember { mutableStateOf<Member?>(null) }

    LaunchedEffect(memberId) { memberId?.let { viewModel.getCurrentMember()?.collect { member = it } } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Hai, ${member?.name?.split(" ")?.firstOrNull() ?: "Member"} \uD83C\uDF75",
                style = MaterialTheme.typography.headlineMedium,
                color = DarkLeaf,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(colors = listOf(MatchaGreen, DarkLeaf)))
                        .padding(24.dp)
                ) {
                    // Poin
                    Column(modifier = Modifier.align(Alignment.TopStart), horizontalAlignment = Alignment.Start) {
                        Text("Poin Kamu", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
                        Text("${member?.totalPoints ?: 0}", style = MaterialTheme.typography.displayMedium, color = TeaAmber, fontWeight = FontWeight.ExtraBold)
                    }

                    // Nama & ID - Di atas QR atau disesuaikan agar tidak tumpang tindih
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Text(
                            text = member?.name ?: "NAMA MEMBER",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ID: MBR00${member?.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 2.sp
                        )
                    }

                    // QR Code di Kanan Bawah (Besar)
                    Column(modifier = Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            modifier = Modifier.size(130.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "QR Code",
                                    modifier = Modifier.fillMaxSize(0.85f),
                                    tint = DarkLeaf
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Scan Kasir", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Icon(
                        imageVector = Icons.Default.LocalDrink,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.size(100.dp).align(Alignment.Center)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShortcutButton(
                    title = "Menu Teh",
                    icon = Icons.Default.RestaurantMenu,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppDestinations.MENU) }
                )
                ShortcutButton(
                    title = "Redeem",
                    icon = Icons.Default.CardGiftcard,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(AppDestinations.REDEEM) }
                )
            }
        }
    }
}

@Composable
fun ShortcutButton(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = TeaAmber)
            Text(title, style = MaterialTheme.typography.labelLarge, color = DarkLeaf, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OrderMenuScreen(viewModel: CoffeeBlissViewModel) {
    val cart by viewModel.cart.collectAsState()
    val totalAmount = cart.entries.sumOf { it.key.price * it.value }
    val pointsEarned = (totalAmount / 10000).toInt()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("Pilih Teh Favoritmu", style = MaterialTheme.typography.headlineSmall, color = DarkLeaf, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 160.dp)) {
                items(viewModel.teaMenu) { product ->
                    TeaMenuItem(
                        product = product,
                        quantity = cart[product] ?: 0,
                        onAdd = { viewModel.addToCart(product) },
                        onRemove = { viewModel.removeFromCart(product) }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = cart.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkLeaf),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = TeaAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "+$pointsEarned Poin (Rp 10rb = 1 Poin)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${cart.values.sum()} Item dipilih", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            Text("Total: Rp ${totalAmount.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Button(
                            onClick = { viewModel.checkout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MatchaGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("Beli Sekarang", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeaMenuItem(product: TeaProduct, quantity: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(SoftCream, CircleShape), contentAlignment = Alignment.Center) {
                Icon(product.icon, contentDescription = null, tint = DarkLeaf)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, color = DarkLeaf)
                Text("Rp ${product.price.toInt()}", color = TeaAmber, fontSize = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (quantity > 0) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color.Gray) }
                    Text("$quantity", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, color = DarkLeaf)
                }
                IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.AddCircle, contentDescription = null, tint = MatchaGreen) }
            }
        }
    }
}

@Composable
fun HistoryScreen(viewModel: CoffeeBlissViewModel) {
    val memberId by viewModel.currentMemberId.collectAsState()
    var transactions by remember { mutableStateOf<List<TransactionHistory>>(emptyList()) }

    LaunchedEffect(memberId) { memberId?.let { viewModel.getTransactions()?.collect { transactions = it } } }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Riwayat Pesanan", style = MaterialTheme.typography.headlineSmall, color = DarkLeaf, fontWeight = FontWeight.Bold)
        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada pesanan", color = Color.Gray) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(transactions.reversed()) { tx ->
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Total: Rp ${tx.amount.toInt()}", fontWeight = FontWeight.Bold, color = DarkLeaf) },
                            supportingContent = { Text(tx.date, fontSize = 12.sp, color = Color.Gray) },
                            trailingContent = { Text("+${tx.pointsEarned} Poin", color = MatchaGreen, fontWeight = FontWeight.Bold) },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = TeaAmber) }
                        )
                    }
                }
            }
        }
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

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tukar Poin", style = MaterialTheme.typography.headlineSmall, color = DarkLeaf, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = TeaAmber.copy(alpha = 0.15f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = TeaAmber, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Poin Kamu", style = MaterialTheme.typography.labelSmall, color = DarkLeaf.copy(alpha = 0.6f))
                    Text("$currentPoints Poin", style = MaterialTheme.typography.headlineSmall, color = DarkLeaf, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        RedeemItem("50 Poin", "Jasmine Tea Cup", currentPoints >= 50) { viewModel.redeemPoints(50); rewardName = "Jasmine Tea"; showRedeemDialog = true }
        RedeemItem("100 Poin", "Matcha Latte Bottle", currentPoints >= 100) { viewModel.redeemPoints(100); rewardName = "Matcha Latte"; showRedeemDialog = true }
        RedeemItem("150 Poin", "Tea Bliss Starter Kit", currentPoints >= 150) { viewModel.redeemPoints(150); rewardName = "Starter Kit"; showRedeemDialog = true }
    }

    if (showRedeemDialog) {
        AlertDialog(
            containerColor = SurfaceWhite,
            onDismissRequest = { showRedeemDialog = false },
            confirmButton = { TextButton(onClick = { showRedeemDialog = false }) { Text("Siap!", color = DarkLeaf) } },
            title = { Text("Redeem Berhasil", color = DarkLeaf, fontWeight = FontWeight.Bold) },
            text = { Text("Silakan ambil $rewardName kamu di kasir ya.", color = DarkLeaf) }
        )
    }
}

@Composable
fun RedeemItem(points: String, reward: String, isEnabled: Boolean, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(reward, fontWeight = FontWeight.Bold, color = DarkLeaf)
                Text(points, color = TeaAmber, fontSize = 14.sp)
            }
            Button(onClick = onClick, enabled = isEnabled, colors = ButtonDefaults.buttonColors(containerColor = MatchaGreen, disabledContainerColor = Color.LightGray), shape = RoundedCornerShape(12.dp)) {
                Text("Tukar", color = if (isEnabled) Color.White else Color.DarkGray)
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: CoffeeBlissViewModel) {
    val memberId by viewModel.currentMemberId.collectAsState()
    var member by remember { mutableStateOf<Member?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedEmail by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }

    LaunchedEffect(memberId) {
        memberId?.let { viewModel.getCurrentMember()?.collect { currentMember ->
            member = currentMember
            if (!isEditing) {
                editedName = currentMember?.name ?: ""
                editedEmail = currentMember?.email ?: ""
                editedPhone = currentMember?.phone ?: ""
            }
        } }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(text = if (isEditing) "Edit Profil" else "Profil Saya", style = MaterialTheme.typography.headlineSmall, color = DarkLeaf, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Box(contentAlignment = Alignment.BottomEnd) {
            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = MatchaGreen.copy(alpha = 0.8f), modifier = Modifier.size(120.dp))
            if (!isEditing) {
                IconButton(onClick = { isEditing = true }, modifier = Modifier.background(TeaAmber, RoundedCornerShape(50.dp)).size(36.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (isEditing) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StyledTextField(value = editedName, onValueChange = { editedName = it }, label = "Nama Lengkap")
                StyledTextField(value = editedEmail, onValueChange = { editedEmail = it }, label = "Email")
                StyledTextField(value = editedPhone, onValueChange = { editedPhone = it }, label = "No HP", isNumber = true)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { isEditing = false; editedName = member?.name ?: ""; editedEmail = member?.email ?: ""; editedPhone = member?.phone ?: "" }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkLeaf)) { Text("Batal") }
                    Button(onClick = { viewModel.updateMember(editedName, editedEmail, editedPhone); isEditing = false }, colors = ButtonDefaults.buttonColors(containerColor = MatchaGreen), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).height(50.dp)) { Text("Simpan", color = Color.White) }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = member?.name ?: "", style = MaterialTheme.typography.titleLarge, color = DarkLeaf, fontWeight = FontWeight.Bold)
                Text(text = "Member ID: MBR00${member?.id}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ProfileInfoItem(Icons.Default.Email, "Email", member?.email ?: "-")
                    HorizontalDivider(color = SoftCream, thickness = 1.dp)
                    ProfileInfoItem(Icons.Default.Phone, "No HP", member?.phone ?: "-")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFD32F2F))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(40.dp).background(SoftCream, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = TeaAmber, modifier = Modifier.size(20.dp)) }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = DarkLeaf, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledTextField(value: String, onValueChange: (String) -> Unit, label: String, isPassword: Boolean = false, isNumber: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MatchaGreen, unfocusedBorderColor = Color.LightGray, focusedLabelColor = DarkLeaf, unfocusedLabelColor = Color.Gray,
            focusedTextColor = DarkLeaf, unfocusedTextColor = DarkLeaf, focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun StyledButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = TeaAmber)) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}