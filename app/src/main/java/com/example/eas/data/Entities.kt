package com.example.eas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val totalPoints: Int = 0
)

@Entity(tableName = "transactions")
data class TransactionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memberId: Int,
    val date: String,
    val amount: Double,
    val pointsEarned: Int
)

