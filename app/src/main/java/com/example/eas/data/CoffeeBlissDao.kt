package com.example.eas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeBlissDao {

    @Query("SELECT * FROM members")
    fun getAllMembers(): Flow<List<Member>>

    @Insert
    suspend fun insertMember(member: Member): Long

    @Query("SELECT * FROM members WHERE id = :memberId")
    fun getMemberById(memberId: Int): Flow<Member?>

    @Query("UPDATE members SET totalPoints = totalPoints + :points WHERE id = :memberId")
    suspend fun addPoints(memberId: Int, points: Int)

    @Query("UPDATE members SET totalPoints = totalPoints - :points WHERE id = :memberId")
    suspend fun deductPoints(memberId: Int, points: Int)

    @Insert
    suspend fun insertTransaction(transaction: TransactionHistory)

    @Query("SELECT * FROM transactions WHERE memberId = :memberId ORDER BY id DESC")
    fun getTransactionsByMember(memberId: Int): Flow<List<TransactionHistory>>

    @Query("SELECT * FROM members WHERE email = :email LIMIT 1")
    suspend fun getMemberByEmail(email: String): Member?

    @Query("UPDATE members SET name = :name, email = :email, phone = :phone WHERE id = :memberId")
    suspend fun updateMemberDetails(memberId: Int, name: String, email: String, phone: String)
}