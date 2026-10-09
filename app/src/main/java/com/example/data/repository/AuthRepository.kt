package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.ShoppingDao
import com.example.data.local.UserDao
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingList
import com.example.data.model.UserAccount
import com.example.util.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val userDao: UserDao,
    private val shoppingDao: ShoppingDao,
    context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    init {
        val savedUserId = prefs.getLong(KEY_LOGGED_USER_ID, -1L)
        if (savedUserId != -1L) {
            // Restore session
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).run {
                kotlinx.coroutines.runBlocking {
                    val user = userDao.getUserById(savedUserId)
                    _currentUser.value = user
                }
            }
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String = "",
        securityQuestion: String,
        securityAnswer: String
    ): Result<UserAccount> {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("E-mail inválido."))
        }
        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("A senha deve ter no mínimo 4 caracteres."))
        }
        if (securityAnswer.isBlank()) {
            return Result.failure(IllegalArgumentException("Preencha a resposta de segurança para recuperação."))
        }

        val existing = userDao.getUserByEmail(normalizedEmail)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Este e-mail já está cadastrado."))
        }

        val salt = PasswordHasher.generateSalt()
        val passwordHash = PasswordHasher.hashPassword(password, salt)
        val answerHash = PasswordHasher.hashSecurityAnswer(securityAnswer)

        val newUser = UserAccount(
            name = name.trim().ifBlank { "Usuário" },
            email = normalizedEmail,
            passwordHash = passwordHash,
            salt = salt,
            phone = phone.trim(),
            securityQuestion = securityQuestion.trim(),
            securityAnswerHash = answerHash
        )

        val id = userDao.insertUser(newUser)
        val createdUser = newUser.copy(id = id)

        // Seed a default personal list for the new user as requested:
        // "O usuário tem uma lista pessoal exclusiva por padrão."
        val defaultListId = shoppingDao.insertList(
            ShoppingList(
                ownerId = id,
                ownerEmail = normalizedEmail,
                title = "Compras do Mês",
                category = "Supermercado"
            )
        )
        // Add sample starter items
        shoppingDao.insertItem(
            ShoppingItem(
                listId = defaultListId,
                name = "Arroz 5kg",
                quantity = 1.0,
                unitPrice = 28.90,
                unit = "pct"
            )
        )
        shoppingDao.insertItem(
            ShoppingItem(
                listId = defaultListId,
                name = "Feijão Carioca 1kg",
                quantity = 2.0,
                unitPrice = 7.50,
                unit = "pct"
            )
        )
        shoppingDao.insertItem(
            ShoppingItem(
                listId = defaultListId,
                name = "Leite Integral 1L",
                quantity = 4.0,
                unitPrice = 4.89,
                unit = "L"
            )
        )

        saveSession(createdUser)
        return Result.success(createdUser)
    }

    suspend fun login(email: String, password: String): Result<UserAccount> {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("Usuário não encontrado com este e-mail."))

        val isPasswordCorrect = PasswordHasher.verifyPassword(password, user.salt, user.passwordHash)
        if (!isPasswordCorrect) {
            return Result.failure(IllegalArgumentException("Senha incorreta."))
        }

        saveSession(user)
        return Result.success(user)
    }

    suspend fun resetPassword(
        email: String,
        securityAnswer: String,
        newPassword: String
    ): Result<Unit> {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("E-mail não cadastrado."))

        if (newPassword.length < 4) {
            return Result.failure(IllegalArgumentException("A nova senha deve ter pelo menos 4 caracteres."))
        }

        val answerValid = PasswordHasher.verifySecurityAnswer(securityAnswer, user.securityAnswerHash)
        if (!answerValid) {
            return Result.failure(IllegalArgumentException("Resposta de segurança incorreta."))
        }

        val newSalt = PasswordHasher.generateSalt()
        val newHash = PasswordHasher.hashPassword(newPassword, newSalt)
        val updatedUser = user.copy(passwordHash = newHash, salt = newSalt)
        userDao.updateUser(updatedUser)

        if (_currentUser.value?.id == user.id) {
            _currentUser.value = updatedUser
        }

        return Result.success(Unit)
    }

    suspend fun findUserSecurityQuestion(email: String): String? {
        val user = userDao.getUserByEmail(email.trim().lowercase())
        return user?.securityQuestion
    }

    fun logout() {
        prefs.edit().remove(KEY_LOGGED_USER_ID).apply()
        _currentUser.value = null
    }

    fun switchUserDirectly(user: UserAccount) {
        saveSession(user)
    }

    fun getAllUsersFlow(): Flow<List<UserAccount>> {
        return userDao.getAllUsers()
    }

    private fun saveSession(user: UserAccount) {
        prefs.edit().putLong(KEY_LOGGED_USER_ID, user.id).apply()
        _currentUser.value = user
    }

    companion object {
        private const val KEY_LOGGED_USER_ID = "logged_user_id"
    }
}
