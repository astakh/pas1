package si.unisa.sss.pocketvoiceassistant.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Одна реплика диалога пользователь↔ассистент. */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,          // "user" | "assistant"
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface MessageDao {
    /** Последние N реплик (для контекста LTM). Порядок — обратный, вызывающий инвертирует. */
    @Query("SELECT * FROM messages ORDER BY id DESC LIMIT :limit")
    fun latest(limit: Int): List<Message>

    @Insert
    fun insert(message: Message)

    @Query("DELETE FROM messages")
    fun clear()

    @Query("SELECT COUNT(*) FROM messages")
    fun count(): Int
}

@Database(entities = [Message::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDb::class.java, "assistant.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}

/** Тонкая обёртка: все обращения к БД — из рабочих корутин. */
class ChatRepository(private val db: AppDb) {

    suspend fun addMessage(role: String, text: String) = withContext(Dispatchers.IO) {
        db.messageDao().insert(Message(role = role, text = text))
    }

    /** История последних [maxTurns*2] реплик в хронологическом порядке. */
    suspend fun history(maxTurns: Int): List<Message> = withContext(Dispatchers.IO) {
        db.messageDao().latest(maxTurns * 2).reversed()
    }

    suspend fun clear() = withContext(Dispatchers.IO) { db.messageDao().clear() }
}
