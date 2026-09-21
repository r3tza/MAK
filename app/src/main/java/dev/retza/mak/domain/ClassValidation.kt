package dev.retza.mak.domain

import java.time.LocalDate
import java.time.LocalTime

data class ClassForm(
    val name: String,
    val semesterProgramId: String?,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val recurrence: Recurrence = Recurrence.EVERY_WEEK,
    val date: LocalDate? = null
)

enum class ClassValidationError {
    NAME_REQUIRED,
    PROGRAM_REQUIRED,
    START_TIME_REQUIRED,
    END_TIME_REQUIRED,
    END_NOT_AFTER_START,
    CROSSES_MIDNIGHT,
    DATE_REQUIRED
}

data class ClassValidationResult(
    val errors: Set<ClassValidationError>
) {
    val isValid: Boolean
        get() = errors.isEmpty()

    fun has(error: ClassValidationError): Boolean = error in errors
}

object ClassValidator {
    fun validate(form: ClassForm): ClassValidationResult = validateClassForm(form)
}

fun validateClassForm(form: ClassForm): ClassValidationResult {
    val errors = linkedSetOf<ClassValidationError>()

    if (form.name.isBlank()) errors += ClassValidationError.NAME_REQUIRED
    if (form.semesterProgramId.isNullOrBlank()) errors += ClassValidationError.PROGRAM_REQUIRED
    if (form.startTime == null) errors += ClassValidationError.START_TIME_REQUIRED
    if (form.endTime == null) errors += ClassValidationError.END_TIME_REQUIRED

    val start = form.startTime
    val end = form.endTime
    if (start != null && end != null && !end.isAfter(start)) {
        errors += ClassValidationError.END_NOT_AFTER_START
        if (end.isBefore(start)) errors += ClassValidationError.CROSSES_MIDNIGHT
    }

    if (form.recurrence == Recurrence.ONCE && form.date == null) {
        errors += ClassValidationError.DATE_REQUIRED
    }

    return ClassValidationResult(errors)
}
