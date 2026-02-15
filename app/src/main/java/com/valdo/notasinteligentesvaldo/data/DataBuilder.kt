package com.valdo.notasinteligentesvaldo.data

import android.content.Context
import androidx.room.Room

/**
 * Objeto singleton para construir y obtener la instancia de la base de datos Room.
 *
 * - Garantiza una única instancia de la base de datos en toda la app.
 * - Aplica migraciones y configuración de Room.
 *
 * Uso:
 *   val db = DatabaseBuilder.getInstance(context)
 */
object DatabaseBuilder {
    private var INSTANCE: NoteDatabase? = null

    /**
     * Obtiene la instancia única de la base de datos.
     * Si no existe, la crea con migraciones y configuración adecuada.
     * @param context Contexto de la aplicación.
     * @return Instancia de NoteDatabase.
     */
    fun getInstance(context: Context): NoteDatabase {
        if (INSTANCE == null) {
            synchronized(NoteDatabase::class) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    .addMigrations(
                        NoteDatabase.MIGRATION_1_2,
                        NoteDatabase.MIGRATION_2_3,
                        NoteDatabase.MIGRATION_3_4,
                        NoteDatabase.MIGRATION_4_5,
                        NoteDatabase.MIGRATION_5_6,
                        NoteDatabase.MIGRATION_6_7,
                        NoteDatabase.MIGRATION_7_8
                    )  // Añade todas las migraciones
                    .fallbackToDestructiveMigration(false) // No destruye datos en migraciones
                    .build()
            }
        }
        return INSTANCE!!
    }
}
