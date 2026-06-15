package com.example.eas.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.eas.data.CoffeeBlissDao
import com.example.eas.data.Member
import com.example.eas.data.TransactionHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CoffeeBlissViewModel(
    private val dao: CoffeeBlissDao,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val savedMemberId = sharedPrefs.getInt("SESSION_ID", -1).let { if (it != -1) it else null }
    private val _currentMemberId = MutableStateFlow<Int?>(savedMemberId)
    val currentMemberId: StateFlow<Int?> = _currentMemberId

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

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

    fun addTransaction(amount: Double) {
        val memberId = _currentMemberId.value ?: return
        val points = (amount / 10000).toInt()
        val date = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())

        viewModelScope.launch {
            val transaction = TransactionHistory(memberId = memberId, date = date, amount = amount, pointsEarned = points)
            dao.insertTransaction(transaction)
            dao.addPoints(memberId, points)
        }
    }

    fun redeemPoints(pointsToRedeem: Int) {
        val memberId = _currentMemberId.value ?: return
        viewModelScope.launch { dao.deductPoints(memberId, pointsToRedeem) }
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