package com.example.boardexamreviewer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * AppDatabase is the main database class for the app.
 * We use @Database to tell Room what entities to store and what version it is.
 */
@Database(
    entities = [
        DocumentEntity::class,
        ReviewerEntity::class,
        QuizEntity::class,
        UserEntity::class,
        CategoryEntity::class,
        StudySessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    // This tells Room that we want to use the AppDao interface.
    abstract fun appDao(): AppDao

    companion object {
        // @Volatile ensures that the INSTANCE is always up-to-date across all threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * getDatabase returns the single instance of our database (Singleton pattern).
         * We only want ONE instance of the database to exist in the app.
         */
        fun getDatabase(context: Context): AppDatabase {
            // If INSTANCE is not null, return it. If it is, create it.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "board_exam_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
