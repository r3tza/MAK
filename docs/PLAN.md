# Plan najbliższych prac

Cel: druga część I-72, przyciski w dialogach. Część kreatora jest gotowa (2026-10-08): „Wstecz” i „Wróć do ustawień” są przyciskami tekstowymi, a systemowy gest wstecz cofa o krok; `SetupWizardTest` 11/11 i ręczny przebieg na `Medium_Phone_A12`. Polityka prywatności jest opublikowana i wpisana w Google Auth Platform (2026-10-08).

Przed pierwszym wydaniem z synchronizacją: sprawdzić logowanie Google na podpisanym APK release (`KNOWN_ISSUES.md`), przełączyć aplikację w Google Auth Platform na tryb produkcyjny (Audience, „Opublikuj aplikację”) i przygotować wpis w `release_notes.json` do akceptacji użytkownika.

Weryfikacja na AVD `Medium_Phone_A12` (decyzja użytkownika z 2026-10-08), bo na `Medium_Phone` brakuje miejsca na instalację APK debug.

## 1. Przygotuj makietę dialogów do akceptacji (I-72)

Cel: druga część I-72, czyli „Później” i „Anuluj” w dialogach `MakDialog` jako przyciski tekstowe. Bez akceptacji makiety nie zmieniaj kodu. Dotyczy dialogów w `SyncScreen.kt` (wybór wersji, wyłączenie synchronizacji), `ScheduleScreen.kt` (korekta tygodnia, rząd „Anuluj” i „Zapisz”), `SettingsScreen.kt` („Zamknij” po błędzie importu) i `OccurrenceDetailsScreen.kt` (edycja terminu). `MakConfirmDeletionDialog` już używa przycisków tekstowych.

Kryterium zakończenia: artefakt z porównaniem przed i po dla każdego dialogu i decyzja użytkownika zapisana w `LOG.md`; implementację zaplanuj wtedy jako kolejny krok.
