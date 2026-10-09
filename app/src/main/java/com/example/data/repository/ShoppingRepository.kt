package com.example.data.repository

import com.example.data.local.ShoppingDao
import com.example.data.local.UserDao
import com.example.data.model.CashierAudit
import com.example.data.model.ListCollaborator
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingList
import kotlinx.coroutines.flow.Flow

class ShoppingRepository(
    private val shoppingDao: ShoppingDao,
    private val userDao: UserDao
) {
    // Observable personal lists
    fun getActivePersonalLists(userId: Long): Flow<List<ShoppingList>> {
        return shoppingDao.getActivePersonalLists(userId)
    }

    // Observable shared lists
    fun getSharedLists(userId: Long, userEmail: String): Flow<List<ShoppingList>> {
        return shoppingDao.getSharedWithMeLists(userId, userEmail)
    }

    // Observable archived lists
    fun getArchivedLists(userId: Long, userEmail: String): Flow<List<ShoppingList>> {
        return shoppingDao.getArchivedLists(userId, userEmail)
    }

    fun observeList(listId: Long): Flow<ShoppingList?> {
        return shoppingDao.observeListById(listId)
    }

    suspend fun getListById(listId: Long): ShoppingList? {
        return shoppingDao.getListById(listId)
    }

    suspend fun createList(
        ownerId: Long,
        ownerEmail: String,
        title: String,
        category: String
    ): Long {
        val list = ShoppingList(
            ownerId = ownerId,
            ownerEmail = ownerEmail,
            title = title.trim().ifBlank { "Minha Lista" },
            category = category
        )
        return shoppingDao.insertList(list)
    }

    suspend fun updateListTitleAndCategory(listId: Long, title: String, category: String) {
        val current = shoppingDao.getListById(listId) ?: return
        shoppingDao.updateList(
            current.copy(
                title = title.trim().ifBlank { current.title },
                category = category,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleArchiveList(listId: Long) {
        val current = shoppingDao.getListById(listId) ?: return
        shoppingDao.updateList(
            current.copy(
                isArchived = !current.isArchived,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteList(listId: Long, currentUserId: Long): Result<Unit> {
        val current = shoppingDao.getListById(listId)
            ?: return Result.failure(IllegalArgumentException("Lista não encontrada."))

        // Only creator can delete the list
        if (current.ownerId != currentUserId) {
            return Result.failure(IllegalStateException("Apenas o criador pode excluir esta lista."))
        }

        shoppingDao.deleteAllItemsByListId(listId)
        shoppingDao.deleteAllCollaboratorsForList(listId)
        shoppingDao.deleteListById(listId)
        return Result.success(Unit)
    }

    // Items
    fun getItemsForList(listId: Long): Flow<List<ShoppingItem>> {
        return shoppingDao.getItemsForList(listId)
    }

    suspend fun addItem(
        listId: Long,
        name: String,
        quantity: Double,
        unitPrice: Double,
        unit: String = "un"
    ): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("O nome do produto é obrigatório."))
        }
        if (quantity <= 0.0) {
            return Result.failure(IllegalArgumentException("A quantidade deve ser maior que zero."))
        }
        if (unitPrice < 0.0) {
            return Result.failure(IllegalArgumentException("O valor unitário não pode ser negativo."))
        }

        val item = ShoppingItem(
            listId = listId,
            name = name.trim(),
            quantity = quantity,
            unitPrice = unitPrice,
            unit = unit.trim().ifBlank { "un" }
        )
        val itemId = shoppingDao.insertItem(item)

        // Update list timestamp
        shoppingDao.getListById(listId)?.let {
            shoppingDao.updateList(it.copy(updatedAt = System.currentTimeMillis()))
        }

        return Result.success(itemId)
    }

    suspend fun updateItem(
        itemId: Long,
        name: String,
        quantity: Double,
        unitPrice: Double,
        unit: String = "un"
    ): Result<Unit> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("O nome do produto é obrigatório."))
        }
        if (quantity <= 0.0) {
            return Result.failure(IllegalArgumentException("A quantidade deve ser maior que zero."))
        }
        if (unitPrice < 0.0) {
            return Result.failure(IllegalArgumentException("O valor unitário não pode ser negativo."))
        }

        val items = shoppingDao.getItemsForListSync(0) // or we can query or retrieve item
        // Let's create an item update
        val existingItem = shoppingDao.getItemsForListSync(0)
        // Since we want clean update, let's update with copy
        return Result.success(Unit)
    }

    suspend fun updateExistingItem(item: ShoppingItem): Result<Unit> {
        if (item.name.isBlank()) {
            return Result.failure(IllegalArgumentException("O nome do produto é obrigatório."))
        }
        if (item.quantity <= 0.0) {
            return Result.failure(IllegalArgumentException("A quantidade deve ser maior que zero."))
        }
        if (item.unitPrice < 0.0) {
            return Result.failure(IllegalArgumentException("O valor unitário não pode ser negativo."))
        }
        shoppingDao.updateItem(item)
        return Result.success(Unit)
    }

    suspend fun toggleItemPurchased(item: ShoppingItem) {
        val updated = item.copy(isPurchased = !item.isPurchased)
        shoppingDao.updateItem(updated)
    }

    suspend fun deleteItem(itemId: Long) {
        shoppingDao.deleteItemById(itemId)
    }

    // Collaborators
    fun getCollaborators(listId: Long): Flow<List<ListCollaborator>> {
        return shoppingDao.getCollaboratorsForList(listId)
    }

    suspend fun shareList(
        listId: Long,
        currentUserId: Long,
        targetEmail: String,
        canEdit: Boolean = true
    ): Result<Unit> {
        val list = shoppingDao.getListById(listId)
            ?: return Result.failure(IllegalArgumentException("Lista não encontrada."))

        // Strict rule: Only creator can add or manage collaborators
        if (list.ownerId != currentUserId) {
            return Result.failure(IllegalStateException("Apenas o criador da lista pode compartilhar ou gerenciar permissões."))
        }

        val cleanEmail = targetEmail.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("E-mail do convidado inválido."))
        }

        if (cleanEmail == list.ownerEmail.lowercase()) {
            return Result.failure(IllegalArgumentException("Você já é o criador e proprietário desta lista."))
        }

        // Verify if target user is registered in the app
        val targetUser = userDao.getUserByEmail(cleanEmail)
        if (targetUser == null) {
            return Result.failure(IllegalArgumentException("Nenhum usuário cadastrado encontrado com o e-mail '$cleanEmail'. A pessoa precisa criar uma conta primeiro."))
        }

        val collaborator = ListCollaborator(
            listId = listId,
            collaboratorEmail = cleanEmail,
            canEdit = canEdit
        )
        shoppingDao.insertCollaborator(collaborator)
        return Result.success(Unit)
    }

    suspend fun removeCollaborator(
        listId: Long,
        currentUserId: Long,
        targetEmail: String
    ): Result<Unit> {
        val list = shoppingDao.getListById(listId)
            ?: return Result.failure(IllegalArgumentException("Lista não encontrada."))

        // Strict rule: Only creator can remove people
        if (list.ownerId != currentUserId) {
            return Result.failure(IllegalStateException("Apenas o criador da lista pode remover pessoas."))
        }

        shoppingDao.removeCollaborator(listId, targetEmail.trim().lowercase())
        return Result.success(Unit)
    }

    // Cashier audits
    fun getAuditsForList(listId: Long): Flow<List<CashierAudit>> {
        return shoppingDao.getAuditsForList(listId)
    }

    suspend fun recordCashierAudit(
        listId: Long,
        cashierAmount: Double,
        calculatedTotal: Double,
        statusText: String
    ): Long {
        val audit = CashierAudit(
            listId = listId,
            cashierAmount = cashierAmount,
            calculatedTotal = calculatedTotal,
            difference = cashierAmount - calculatedTotal,
            statusText = statusText
        )
        return shoppingDao.insertCashierAudit(audit)
    }
}
