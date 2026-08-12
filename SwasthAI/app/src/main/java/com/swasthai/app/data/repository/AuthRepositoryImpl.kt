package com.swasthai.app.data.repository

import com.swasthai.app.data.local.database.dao.UserDao
import com.swasthai.app.data.local.datastore.UserPreferences
import com.swasthai.app.data.mapper.toDomain
import com.swasthai.app.data.mapper.toEntity
import com.swasthai.app.domain.model.User
import com.swasthai.app.domain.model.UserRole
import com.swasthai.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation of AuthRepository.
 *
 * Supports both online (Firebase) and offline authentication.
 * Offline auth uses hashed passwords stored in the local Room database.
 */
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = userPreferences.isLoggedInFlow

    override suspend fun login(phone: String, password: String): Result<User> {
        return try {
            // TODO: Add Firebase Auth integration when google-services.json is configured
            loginOffline(phone, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginOffline(phone: String, password: String): Result<User> {
        return try {
            val passwordHash = hashPassword(password)
            val userEntity = userDao.authenticateUser(phone, passwordHash)
                ?: return Result.failure(Exception("Invalid credentials"))

            val user = userEntity.toDomain()
            userPreferences.saveUserSession(
                userId = user.id,
                userName = user.name,
                userRole = user.role,
                phone = user.phone
            )
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        name: String,
        phone: String,
        password: String,
        role: UserRole
    ): Result<User> {
        return try {
            // Check if user already exists
            val existing = userDao.getUserByPhone(phone)
            if (existing != null) {
                return Result.failure(Exception("Phone number already registered"))
            }

            val user = User(
                id = UUID.randomUUID().toString(),
                name = name,
                phone = phone,
                role = role
            )

            val passwordHash = hashPassword(password)
            userDao.insertUser(user.toEntity(passwordHash))

            userPreferences.saveUserSession(
                userId = user.id,
                userName = user.name,
                userRole = user.role,
                phone = user.phone
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val userId = userPreferences.userIdFlow.firstOrNull() ?: return null
        return userDao.getUserById(userId)?.toDomain()
    }

    override suspend fun logout() {
        userPreferences.clearSession()
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
