package com.mybudget.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mybudget.data.local.dao.CategoryDao
import com.mybudget.data.local.dao.TransactionDao
import com.mybudget.data.local.entity.CategoryEntity
import com.mybudget.data.local.entity.TransactionEntity
import com.mybudget.domain.CategoryCatalog
import com.mybudget.security.DatabaseKeyManager
import net.sqlcipher.database.SupportFactory

@Database(entities = [TransactionEntity::class, CategoryEntity::class], version = 2, exportSchema = true)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN basicCategory TEXT NOT NULL DEFAULT 'Other'")
                db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")
                db.execSQL("ALTER TABLE categories ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name_type ON categories(name, type)")
                seedDefaultCategories(db)
            }
        }

        @Volatile
        private var INSTANCE: BudgetDatabase? = null

        private fun seedDefaultCategories(database: SupportSQLiteDatabase) {
            CategoryCatalog.toEntities().forEach { category ->
                database.execSQL(
                    "INSERT OR IGNORE INTO categories(name, emoji, keywords, basicCategory, type, isCustom) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf<Any>(
                        category.name,
                        category.emoji,
                        category.keywords,
                        category.basicCategory,
                        category.type,
                        if (category.isCustom) 1 else 0
                    )
                )
            }
        }

        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = DatabaseKeyManager.getDatabasePassphrase(context)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget_database"
                )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDefaultCategories(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
