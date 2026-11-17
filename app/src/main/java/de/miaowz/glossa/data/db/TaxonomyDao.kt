package de.miaowz.glossa.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Languages (shared by all libraries) and categories (scoped by [CategoryType]). */
@Dao
interface TaxonomyDao {

    @Query("SELECT * FROM languages ORDER BY name")
    fun observeLanguages(): Flow<List<LanguageEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name")
    fun observeCategories(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM languages WHERE name = :name LIMIT 1")
    suspend fun findLanguage(name: String): LanguageEntity?

    @Query("SELECT * FROM categories WHERE languageId = :languageId AND type = :type AND name = :name LIMIT 1")
    suspend fun findCategory(languageId: Long, type: CategoryType, name: String): CategoryEntity?

    @Insert
    suspend fun insertLanguage(language: LanguageEntity): Long

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long
}
