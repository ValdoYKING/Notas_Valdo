package com.valdo.notasinteligentesvaldo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.valdo.notasinteligentesvaldo.models.Note

@Database(
    entities = [Note::class],
    version = 2,  // ¡Actualizado de 1 a 2!
    exportSchema = true  // Habilitado para ver esquemas
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        // Migración de versión 1 a 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Añade todos los nuevos campos
                database.execSQL("ALTER TABLE notes ADD COLUMN timestampInit INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN location TEXT")
                database.execSQL("ALTER TABLE notes ADD COLUMN notificationTime INTEGER")
                database.execSQL("ALTER TABLE notes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE notes ADD COLUMN category TEXT")
                database.execSQL("ALTER TABLE notes ADD COLUMN isMarkdownEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}