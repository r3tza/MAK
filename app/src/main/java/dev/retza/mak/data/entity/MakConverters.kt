package dev.retza.mak.data.entity

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class MakConverters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromDayOfWeek(value: DayOfWeek?): Int? = value?.value

    @TypeConverter
    fun toDayOfWeek(value: Int?): DayOfWeek? = value?.let(DayOfWeek::of)

    @TypeConverter
    fun fromWeekType(value: WeekType?): String? = value?.name

    @TypeConverter
    fun toWeekType(value: String?): WeekType? = value?.let(WeekType::valueOf)

    @TypeConverter
    fun fromWeekOverrideScope(value: WeekOverrideScope?): String? = value?.name

    @TypeConverter
    fun toWeekOverrideScope(value: String?): WeekOverrideScope? =
        value?.let(WeekOverrideScope::valueOf)

    @TypeConverter
    fun fromRecurrence(value: Recurrence?): String? = value?.name

    @TypeConverter
    fun toRecurrence(value: String?): Recurrence? = value?.let(Recurrence::valueOf)

    @TypeConverter
    fun fromOccurrenceChangeKind(value: OccurrenceChangeKind?): String? = value?.name

    @TypeConverter
    fun toOccurrenceChangeKind(value: String?): OccurrenceChangeKind? =
        value?.let(OccurrenceChangeKind::valueOf)
}
