package com.example.worktime

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName="projects", indices=[Index("isArchived"), Index("name")])
data class Project(
  @PrimaryKey(autoGenerate=true) val id: Long = 0,
  val name: String,
  val number: String? = null,
  val isArchived: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName="work_sessions", indices=[Index("projectId"), Index("startTs"), Index("endTs")])
data class WorkSession(
  @PrimaryKey(autoGenerate=true) val id: Long = 0,
  val projectId: Long,
  val startTs: Long,
  val endTs: Long? = null,
  val startedManuallyAt: Boolean = false,
  val note: String? = null
)

@Entity(
  tableName="pause_segments",
  indices=[Index("sessionId"), Index("startTs"), Index("endTs")],
  foreignKeys=[ForeignKey(entity=WorkSession::class, parentColumns=["id"], childColumns=["sessionId"], onDelete=ForeignKey.CASCADE)]
)
data class PauseSegment(
  @PrimaryKey(autoGenerate=true) val id: Long = 0,
  val sessionId: Long,
  val startTs: Long,
  val endTs: Long? = null
)

@Entity(
  tableName="session_edits",
  indices=[Index("sessionId"), Index("editedAt")],
  foreignKeys=[ForeignKey(entity=WorkSession::class, parentColumns=["id"], childColumns=["sessionId"], onDelete=ForeignKey.CASCADE)]
)
data class SessionEdit(
  @PrimaryKey(autoGenerate=true) val id: Long = 0,
  val sessionId: Long,
  val field: String,
  val oldValue: String?,
  val newValue: String?,
  val reason: String?,
  val editedAt: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDao {
  @Insert fun insert(p: Project): Long
  @Update fun update(p: Project)
  @Query("UPDATE projects SET isArchived=1 WHERE id=:id") fun archive(id: Long)
  @Query("UPDATE projects SET isArchived=0 WHERE id=:id") fun unarchive(id: Long)

  @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY name")
  fun getActive(): Flow<List<Project>>

  @Query("SELECT * FROM projects ORDER BY isArchived ASC, name ASC")
  fun getAll(): Flow<List<Project>>

  @Query("SELECT * FROM projects WHERE id = :id")
  fun getByIdSync(id: Long): Project?
}

@Dao
interface SessionDao {
  @Insert fun insert(s: WorkSession): Long
  @Update fun update(s: WorkSession)
  @Query("SELECT * FROM work_sessions WHERE id=:id") fun getById(id: Long): WorkSession?
  @Query("SELECT * FROM work_sessions WHERE endTs IS NULL LIMIT 1") fun getRunning(): WorkSession?

  // NEU: Stable Flow für Compose
  @Query("SELECT * FROM work_sessions WHERE endTs IS NULL LIMIT 1")
  fun observeRunning(): kotlinx.coroutines.flow.Flow<WorkSession?>

  @Query("""
    SELECT (
      (COALESCE(ws.endTs, :now) - ws.startTs)
      - COALESCE((SELECT SUM(COALESCE(ps.endTs, :now) - ps.startTs)
                  FROM pause_segments ps WHERE ps.sessionId = ws.id), 0)
    )
    FROM work_sessions ws WHERE ws.id = :sid
  """)
  fun effectiveForSession(sid: Long, now: Long): Long?

  @Query("""
    SELECT COALESCE(SUM(
      (COALESCE(ws.endTs, :now) - ws.startTs)
      - COALESCE((SELECT SUM(COALESCE(ps.endTs, :now) - ps.startTs)
                  FROM pause_segments ps WHERE ps.sessionId = ws.id), 0)
    ), 0)
    FROM work_sessions ws
    WHERE ws.startTs >= :start AND COALESCE(ws.endTs, :now) < :end
  """)
  fun effectiveSumBetween(start: Long, end: Long, now: Long): Long

  @Query("""
    SELECT COALESCE(SUM(
      (COALESCE(ws.endTs, :now) - ws.startTs)
      - COALESCE((SELECT SUM(COALESCE(ps.endTs, :now) - ps.startTs)
                  FROM pause_segments ps WHERE ps.sessionId = ws.id), 0)
    ), 0)
    FROM work_sessions ws
    WHERE ws.projectId = :projectId
  """)
  fun effectiveSumForProject(projectId: Long, now: Long): Long

  @Query("SELECT * FROM work_sessions WHERE projectId = :projectId ORDER BY startTs ASC")
  fun sessionsForProject(projectId: Long): List<WorkSession>

  @Transaction
  @Query("""
    SELECT * FROM work_sessions
    WHERE date(startTs/1000,'unixepoch','localtime') = date(:day/1000,'unixepoch','localtime')
    ORDER BY startTs
  """)
  fun sessionsForDay(day: Long): List<WorkSession>
}

@Dao
interface PauseDao {
  @Insert fun insert(p: PauseSegment): Long
  @Update fun update(p: PauseSegment)
  @Delete fun delete(p: PauseSegment)
  @Query("SELECT * FROM pause_segments WHERE sessionId=:sid AND endTs IS NULL LIMIT 1") fun getOpenPause(sid: Long): PauseSegment?
  @Query("SELECT * FROM pause_segments WHERE sessionId=:sid ORDER BY startTs") fun allForSession(sid: Long): List<PauseSegment>
}

@Dao
interface EditDao {
  @Insert fun insert(e: SessionEdit): Long
  @Query("SELECT * FROM session_edits WHERE sessionId = :sid ORDER BY editedAt DESC") fun listForSession(sid: Long): List<SessionEdit>
}

@Database(
  entities=[Project::class, WorkSession::class, PauseSegment::class, SessionEdit::class],
  version=3,
  exportSchema = false   // <- Schema-Export in Entwicklung deaktiviert
)
abstract class AppDb : RoomDatabase() {
  abstract fun projectDao(): ProjectDao
  abstract fun sessionDao(): SessionDao
  abstract fun pauseDao(): PauseDao
  abstract fun editDao(): EditDao

  companion object {
    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS session_edits (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            sessionId INTEGER NOT NULL,
            field TEXT NOT NULL,
            oldValue TEXT,
            newValue TEXT,
            reason TEXT,
            editedAt INTEGER NOT NULL,
            FOREIGN KEY(sessionId) REFERENCES work_sessions(id) ON DELETE CASCADE
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_edits_session ON session_edits(sessionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ws_start ON work_sessions(startTs)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ws_project ON work_sessions(projectId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ps_session ON pause_segments(sessionId)")
      }
    }

    fun build(ctx: Context) =
      Room.databaseBuilder(ctx, AppDb::class.java, "app.db")
        .fallbackToDestructiveMigration()    // Entwicklung
        .addMigrations(MIGRATION_2_3)
        .build()
  }
}
