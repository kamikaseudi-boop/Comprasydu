package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CashierAudit
import com.example.data.model.ListCollaborator
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingList
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    // Lists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ShoppingList): Long

    @Update
    suspend fun updateList(list: ShoppingList)

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteListById(listId: Long)

    @Query("SELECT * FROM shopping_lists WHERE id = :listId LIMIT 1")
    suspend fun getListById(listId: Long): ShoppingList?

    @Query("SELECT * FROM shopping_lists WHERE id = :listId LIMIT 1")
    fun observeListById(listId: Long): Flow<ShoppingList?>

    // Personal lists
    @Query("SELECT * FROM shopping_lists WHERE ownerId = :userId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getActivePersonalLists(userId: Long): Flow<List<ShoppingList>>

    // Archived lists
    @Query("""
        SELECT DISTINCT l.* FROM shopping_lists l
        LEFT JOIN list_collaborators c ON l.id = c.listId
        WHERE (l.ownerId = :userId OR c.collaboratorEmail = :userEmail)
        AND l.isArchived = 1
        ORDER BY l.updatedAt DESC
    """)
    fun getArchivedLists(userId: Long, userEmail: String): Flow<List<ShoppingList>>

    // Shared lists with collaborator
    @Query("""
        SELECT l.* FROM shopping_lists l
        INNER JOIN list_collaborators c ON l.id = c.listId
        WHERE c.collaboratorEmail = :userEmail AND l.ownerId != :userId AND l.isArchived = 0
        ORDER BY l.updatedAt DESC
    """)
    fun getSharedWithMeLists(userId: Long, userEmail: String): Flow<List<ShoppingList>>

    // Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem): Long

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: Long)

    @Query("DELETE FROM shopping_items WHERE listId = :listId")
    suspend fun deleteAllItemsByListId(listId: Long)

    @Query("SELECT * FROM shopping_items WHERE listId = :listId ORDER BY isPurchased ASC, id DESC")
    fun getItemsForList(listId: Long): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    suspend fun getItemsForListSync(listId: Long): List<ShoppingItem>

    // Collaborators
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollaborator(collaborator: ListCollaborator): Long

    @Query("DELETE FROM list_collaborators WHERE listId = :listId AND collaboratorEmail = :email")
    suspend fun removeCollaborator(listId: Long, email: String)

    @Query("DELETE FROM list_collaborators WHERE listId = :listId")
    suspend fun deleteAllCollaboratorsForList(listId: Long)

    @Query("SELECT * FROM list_collaborators WHERE listId = :listId ORDER BY addedAt ASC")
    fun getCollaboratorsForList(listId: Long): Flow<List<ListCollaborator>>

    @Query("SELECT * FROM list_collaborators WHERE listId = :listId AND collaboratorEmail = :email LIMIT 1")
    suspend fun getCollaborator(listId: Long, email: String): ListCollaborator?

    // Cashier audits
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashierAudit(audit: CashierAudit): Long

    @Query("SELECT * FROM cashier_audits WHERE listId = :listId ORDER BY auditedAt DESC")
    fun getAuditsForList(listId: Long): Flow<List<CashierAudit>>
}
