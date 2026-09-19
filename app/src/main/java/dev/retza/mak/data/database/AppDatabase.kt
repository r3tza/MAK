package dev.retza.mak.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.retza.mak.data.entity.ClassEntity
import dev.retza.mak.data.entity.CourseEntity
import dev.retza.mak.data.entity.MakConverters
import dev.retza.mak.data.entity.OccurrenceChangeEntity
import dev.retza.mak.data.entity.OccurrenceNoteEntity
import dev.retza.mak.data.entity.SemesterEntity
import dev.retza.mak.data.entity.TeacherEntity
import dev.retza.mak.data.entity.WeekOverrideEntity

@Database(
    entities = [
        SemesterEntity::class,
        CourseEntity::class,
        TeacherEntity::class,
        ClassEntity::class,
        WeekOverrideEntity::class,
        OccurrenceNoteEntity::class,
        OccurrenceChangeEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(MakConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao

    abstract fun courseDao(): CourseDao

    abstract fun teacherDao(): TeacherDao

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
                ).build().also { instance = it }
            }
    }
}
