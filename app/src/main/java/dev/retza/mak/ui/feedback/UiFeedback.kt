package dev.retza.mak.ui.feedback

enum class UiFeedbackKind {
    Success,
    Error,
    Warning,
    Info
}

data class UiFeedback(
    val message: String,
    val kind: UiFeedbackKind
)
