package com.valdo.notasinteligentesvaldo.data

import android.content.Context
import androidx.room.Room

object DatabaseBuilder {
    private var INSTANCE: NoteDatabase? = null

    fun getInstance(context: Context): NoteDatabase {
        if (INSTANCE == null) {
            synchronized(NoteDatabase::class) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    .addMigrations(NoteDatabase.MIGRATION_1_2)  // Añade la migración
                    .fallbackToDestructiveMigration() // Solo para desarrollo
                    .build()
            }
        }
        return INSTANCE!!
    }
}