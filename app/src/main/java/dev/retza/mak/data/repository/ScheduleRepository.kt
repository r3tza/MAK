package dev.retza.mak.data.repository

import dev.retza.mak.data.database.AppDatabase
import dev.retza.mak.domain.ActivePlanData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface ScheduleRepository {
    fun observeActivePlanData(): Flow<ActivePlanData?>
}

@OptIn(ExperimentalCoroutinesApi::class)
@org.koin.core.annotation.Single(binds = [ScheduleRepository::class])
class RoomScheduleRepository(
    private val database: AppDatabase
) : ScheduleRepository {
    private val semesters = database.semesterDao()

    override fun observeActivePlanData(): Flow<ActivePlanData?> =
        semesters.observeActive().flatMapLatest { semester ->
            if (semester == null) {
                flowOf(null)
            } else {
                semesters.observeWithData(semester.id).map { it?.toActivePlanData() }
            }
        }
}
