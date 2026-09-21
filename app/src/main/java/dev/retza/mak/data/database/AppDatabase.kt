package dev.retza.mak.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.retza.mak.data.entity.AcademicCalendarEntity
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.MakConverters
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.SemesterProgramEntity
import dev.retza.mak.data.entity.StudyProgramEntity
import dev.retza.mak.data.entity.WeekOverrideEntity

@Database(
    entities = [
        SemesterEntity::class,
        StudyProgramEntity::class,
        AcademicCalendarEntity::class,
        SemesterProgramEntity::class,
        ClassEntity::class,
        WeekOverrideEntity::class,
        OccurrenceNoteEntity::class,
        OccurrenceChangeEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(MakConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao

    abstract fun studyProgramDao(): StudyProgramDao

    abstract fun academicCalendarDao(): AcademicCalendarDao

    abstract fun semesterProgramDao(): SemesterProgramDao

    abstract fun classDao(): ClassDao

    abstract fun weekOverrideDao(): WeekOverrideDao

    abstract fun occurrenceNoteDao(): OccurrenceNoteDao

    abstract fun occurrenceChangeDao(): OccurrenceChangeDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mak.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
