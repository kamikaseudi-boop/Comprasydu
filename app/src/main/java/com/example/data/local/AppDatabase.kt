package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CashierAudit
import com.example.data.model.ListCollaborator
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingList
import com.example.data.model.UserAccount

@Database(
    entities = [
        UserAccount::class,
        ShoppingList::class,
        ListCollaborator::class,
        ShoppingItem::class,
        CashierAudit::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "compras_conferencia.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
