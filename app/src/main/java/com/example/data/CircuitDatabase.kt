package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.CircuitComponent
import com.example.model.CircuitLayer
import com.example.model.CircuitSheet
import com.example.model.LadderRung
import com.example.model.WireConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

@Entity(tableName = "circuit_projects")
data class CircuitProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val componentsJson: String,
    val wiresJson: String,
    val ladderRungsJson: String = "[]",
    val sheetsJson: String = "[]",
    val layersJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

class CircuitTypeConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromComponents(components: List<CircuitComponent>): String {
        return json.encodeToString(components)
    }

    @TypeConverter
    fun toComponents(jsonStr: String): List<CircuitComponent> = decodeListSafely(jsonStr)

    @TypeConverter
    fun fromWires(wires: List<WireConnection>): String {
        return json.encodeToString(wires)
    }

    @TypeConverter
    fun toWires(jsonStr: String): List<WireConnection> = decodeListSafely(jsonStr)

    @TypeConverter
    fun fromLadderRungs(rungs: List<LadderRung>): String {
        return json.encodeToString(rungs)
    }

    @TypeConverter
    fun toLadderRungs(jsonStr: String): List<LadderRung> = decodeListSafely(jsonStr)

    @TypeConverter
    fun fromSheets(sheets: List<CircuitSheet>): String {
        return json.encodeToString(sheets)
    }

    @TypeConverter
    fun toSheets(jsonStr: String): List<CircuitSheet> {
        val recovered = decodeListSafely<CircuitSheet>(jsonStr)
        return recovered.ifEmpty { listOf(CircuitSheet("sheet_1", "برگه ۱", 0)) }
    }

    @TypeConverter
    fun fromLayers(layers: List<CircuitLayer>): String {
        return json.encodeToString(layers)
    }

    @TypeConverter
    fun toLayers(jsonStr: String): List<CircuitLayer> {
        val recovered = decodeListSafely<CircuitLayer>(jsonStr)
        return recovered.ifEmpty { defaultLayers() }
    }

    /**
     * Reads arrays item-by-item so a single malformed legacy item does not hide
     * every valid component/wire/rung. Invalid whole JSON still uses the caller's
     * safe fallback; this converter never mutates or deletes the stored source.
     */
    private inline fun <reified T> decodeListSafely(raw: String): List<T> {
        val root = try {
            json.parseToJsonElement(raw)
        } catch (_: Exception) {
            return emptyList()
        }
        val array = root as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            try {
                json.decodeFromJsonElement<T>(item)
            } catch (_: Exception) {
                null
            }
        }
    }

    companion object {
        fun defaultLayers(): List<CircuitLayer> = listOf(
            CircuitLayer("layer_power", "قدرت", isVisible = true, isLocked = false, colorHex = "#FF5722"),
            CircuitLayer("layer_control", "فرمان", isVisible = true, isLocked = false, colorHex = "#00E5FF"),
            CircuitLayer("layer_notes", "یادداشت", isVisible = true, isLocked = false, colorHex = "#FFD600")
        )
    }
}

@Dao
interface CircuitProjectDao {
    @Query("SELECT * FROM circuit_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<CircuitProjectEntity>>

    @Query("SELECT * FROM circuit_projects WHERE id = :id")
    suspend fun getProjectById(id: String): CircuitProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProject(project: CircuitProjectEntity)

    @Query("DELETE FROM circuit_projects WHERE id = :id")
    suspend fun deleteProject(id: String)
}

@Entity(tableName = "project_drafts")
data class ProjectDraftEntity(
    @PrimaryKey val draftKey: String,
    val originalProjectId: String? = null,
    val title: String,
    val description: String,
    val componentsJson: String,
    val wiresJson: String,
    val ladderRungsJson: String = "[]",
    val sheetsJson: String = "[]",
    val layersJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDraftDao {
    @Query("SELECT * FROM project_drafts WHERE draftKey = :key")
    suspend fun getDraft(key: String): ProjectDraftEntity?

    @Query("SELECT * FROM project_drafts ORDER BY updatedAt DESC")
    suspend fun getAllDrafts(): List<ProjectDraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDraft(draft: ProjectDraftEntity)

    @Query("DELETE FROM project_drafts WHERE draftKey = :key")
    suspend fun deleteDraft(key: String)

    @Query("DELETE FROM project_drafts")
    suspend fun clearAllDrafts()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE circuit_projects ADD COLUMN ladderRungsJson TEXT NOT NULL DEFAULT '[]'")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS project_drafts (
                draftKey TEXT NOT NULL PRIMARY KEY,
                originalProjectId TEXT,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                componentsJson TEXT NOT NULL,
                wiresJson TEXT NOT NULL,
                ladderRungsJson TEXT NOT NULL DEFAULT '[]',
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE circuit_projects ADD COLUMN sheetsJson TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE circuit_projects ADD COLUMN layersJson TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE project_drafts ADD COLUMN sheetsJson TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE project_drafts ADD COLUMN layersJson TEXT NOT NULL DEFAULT '[]'")
    }
}

@Database(entities = [CircuitProjectEntity::class, ProjectDraftEntity::class], version = 4, exportSchema = false)
@TypeConverters(CircuitTypeConverters::class)
abstract class AppCircuitDatabase : RoomDatabase() {
    abstract fun circuitProjectDao(): CircuitProjectDao
    abstract fun projectDraftDao(): ProjectDraftDao
}