package com.valdo.notasinteligentesvaldo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.valdo.notasinteligentesvaldo.models.Note
import com.valdo.notasinteligentesvaldo.models.Category
import com.valdo.notasinteligentesvaldo.models.NoteCategoryCrossRef

@Database(
    entities = [Note::class, Category::class, NoteCategoryCrossRef::class],
    version = 6,  // Subimos a versión 6 para agregar columna isNotificationPersistent en notes
    exportSchema = true
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        // Migración de versión 2 a 3: crea tablas de categorías y relación cruzada, elimina columna category
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (categoryId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS NoteCategoryCrossRef (noteId INTEGER NOT NULL, categoryId INTEGER NOT NULL, PRIMARY KEY(noteId, categoryId))")
                // Eliminar columna category de notes:
                db.execSQL("CREATE TABLE IF NOT EXISTS notes_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL, timestampInit INTEGER NOT NULL, location TEXT, notificationTime INTEGER, isFavorite INTEGER NOT NULL, isMarkdownEnabled INTEGER NOT NULL)")
                db.execSQL("INSERT INTO notes_new (id, title, content, timestamp, timestampInit, location, notificationTime, isFavorite, isMarkdownEnabled) SELECT id, title, content, timestamp, timestampInit, location, notificationTime, isFavorite, isMarkdownEnabled FROM notes")
                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE notes_new RENAME TO notes")
            }
        }
        // Mantener migración anterior
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN timestampInit INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN location TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN notificationTime INTEGER")
                db.execSQL("ALTER TABLE notes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN isMarkdownEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }
        // NUEVO: migración 3 -> 4 para crear índices en la tabla de cruce
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_NoteCategoryCrossRef_noteId ON NoteCategoryCrossRef(noteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_NoteCategoryCrossRef_categoryId ON NoteCategoryCrossRef(categoryId)")
            }
        }
        // NUEVO: migración 4 -> 5 para agregar columna emoji en categories con default 📝
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN emoji TEXT NOT NULL DEFAULT '📝'")
            }
        }
        // NUEVO: migración 5 -> 6 para agregar columna isNotificationPersistent a notes
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isNotificationPersistent INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
