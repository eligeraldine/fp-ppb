package com.example.eas.viewmodel

import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eas.data.CoffeeBlissDao
import com.example.eas.data.Member
import com.example.eas.data.TransactionHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TeaProduct(
    val id: Int,
    val name: String,
    val price: Double,
    val icon: ImageVector
)

class CoffeeBlissViewModel(
    private val dao: CoffeeBlissDao,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val savedMemberId = sharedPrefs.getInt("SESSION_ID", -1).let { if (it != -1) it else null }
    private val _currentMemberId = MutableStateFlow<Int?>(savedMemberId)
    val currentMemberId: StateFlow<Int?> = _currentMemberId

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    // --- MENU DATA ---
    val teaMenu = listOf(
        TeaProduct(1, "Jasmine Green Tea", 15000.0, Icons.Default.LocalDrink),
        TeaProduct(2, "Matcha Latte", 25000.0, Icons.Default.EmojiFoodBeverage),
        TeaProduct(3, "Earl Grey Milk Tea", 22000.0, Icons.Default.Coffee),
        TeaProduct(4, "Oolong Peach Tea", 20000.0, Icons.Default.WineBar),
        TeaProduct(5, "Chamomile Honey", 18000.0, Icons.Default.LocalDrink),
        TeaProduct(6, "Thai Tea Special", 23000.0, Icons.Default.EmojiFoodBeverage)
    )

    // --- CART STATE ---
    private val _cart = MutableStateFlow<Map<TeaProduct, Int>>(emptyMap())
    val cart: StateFlow<Map<TeaProduct, Int>> = _cart.asStateFlow()

    fun addToCart(product: TeaProduct) {
        val current = _cart.value.toMutableMap()
        current[product] = current.getOrDefault(product, 0) + 1
        _cart.value = current
    }

    fun removeFromCart(product: TeaProduct) {
        val current = _cart.value.toMutableMap()
        val count = current.getOrDefault(product, 0)
        if (count > 1) {
            current[product] = count - 1
        } else {
            current.remove(product)
        }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun checkout() {
        val memberId = _currentMemberId.value ?: return
        val totalAmount = _cart.value.entries.sumOf { it.key.price * it.value }
        if (totalAmount <= 0) return

        val points = (totalAmount / 10000).toInt()
        val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            val transaction = TransactionHistory(
                memberId = memberId,
                date = date,
                amount = totalAmount,
                pointsEarned = points
            )
            dao.insertTransaction(transaction)
            dao.addPoints(memberId, points)
            clearCart()
        }
    }

    // --- MEMBER OPERATIONS ---
    fun registerMember(name: String, email: String, phone: String, passwordEntered: String) {
        viewModelScope.launch {
            val member = Member(name = name, email = email, phone = phone, password = passwordEntered)
            val id = dao.insertMember(member)
            _currentMemberId.value = id.toInt()
            _authError.value = null
            sharedPrefs.edit().putInt("SESSION_ID", id.toInt()).apply()
        }
    }

    fun login(email: String, passwordEntered: String) {
        viewModelScope.launch {
            val member = dao.getMemberByEmail(email)
            if (member != null) {
                if (member.password == passwordEntered) {
                    _currentMemberId.value = member.id
                    _authError.value = null
                    sharedPrefs.edit().putInt("SESSION_ID", member.id).apply()
                } else {
                    _authError.value = "Password salah!"
                }
            } else {
                _authError.value = "Email tidak ditemukan. Silakan daftar."
            }
        }
    }

    fun logout() {
        _currentMemberId.value = null
        _authError.value = null
        sharedPrefs.edit().remove("SESSION_ID").apply()
    }

    fun getCurrentMember() = _currentMemberId.value?.let { dao.getMemberById(it) }
    fun getTransactions() = _currentMemberId.value?.let { dao.getTransactionsByMember(it) }

    fun redeemPoints(pointsToRedeem: Int) {
        val memberId = _currentMemberId.value ?: return
        viewModelScope.launch { dao.deductPoints(memberId, pointsToRedeem) }
    }

    fun updateMember(name: String, email: String, phone: String) {
        val memberId = _currentMemberId.value ?: return
        viewModelScope.launch {
            dao.updateMemberDetails(memberId, name, email, phone)
        }
    }

    fun getAllMembers() = dao.getAllMembers()
}

class CoffeeBlissViewModelFactory(
    private val dao: CoffeeBlissDao,
    private val sharedPrefs: SharedPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CoffeeBlissViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CoffeeBlissViewModel(dao, sharedPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}