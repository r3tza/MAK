# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie są trzy kroki: edycja kierunków (I-21), wybór koloru z palety barw (I-22) i drobne poprawki z audytu z 2026-09-25 (I-20). Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

Każdy krok ma być wykonalny także przez słabszego agenta bez odgadywania intencji. Podaj kolejność małych zmian, docelowe pliki lub obszary kodu, zależności, przypadki brzegowe, sposób sprawdzenia i jednoznaczne kryterium zakończenia. Jeśli do wykonania brakuje decyzji, zapisz ją jako bloker zamiast pozostawiać ukryte założenie.

## 3. Drobne poprawki z audytu z 2026-09-25 (I-20)

Każdy punkt to osobny commit.

1. Potwierdzenie usunięcia korekty tygodnia i nieużywanego kalendarza. Snackbar nie obsługuje akcji, więc użyj dialogów w stylu `CourseDeletionDialog`: `requestWeekOverrideDeletion`, `confirm...`, `cancel...` oraz to samo dla kalendarza. Treść korekty: „Korekta tygodnia od {data} zostanie usunięta. Rytm A/B wróci do automatycznego wyliczenia.” Treść kalendarza: „Kalendarz {zakres} nie jest używany przez żaden kierunek i zostanie usunięty razem ze swoimi korektami.” Akcja „Usuń” w kolorze błędu. Testy ViewModelu (brak usunięcia bez potwierdzenia) i Compose.
2. Data korekty: przy zmianie daty w formularzu normalizuj ją do poniedziałku tygodnia (`TemporalAdjusters.previousOrSame(MONDAY)`) i pokaż pod polem „Korekta obejmuje tydzień od poniedziałku {data}.”. Usuń błąd „Wybierz poniedziałek.”, który przestaje być osiągalny. Test ViewModelu dla środy.
3. Powiadomienia na Androidzie 12 i starszym: zgoda `POST_NOTIFICATIONS` istnieje od API 33. W `MainActivity` przy API poniżej 33 zamiast prośby o zgodę otwieraj `Settings.ACTION_APP_NOTIFICATION_SETTINGS` z `EXTRA_APP_PACKAGE`; przy API 33 i nowszym zostaw prośbę. Po powrocie do aplikacji stan `notificationsBlocked` odświeża się jak dotąd. Sprawdź lint (`InlinedApi` znika) i ręcznie na emulatorze API 33 lub nowszym, że zachowanie się nie zmieniło.
4. Aktualizacja `androidx.core:core-ktx` do 1.19.1 i `navigation-compose` do 2.10.2 w `gradle/libs.versions.toml`. Kotlin 2.4.20 tylko razem ze zgodnym KSP i pluginem Koin, zgodnie z `STACK.md`; jeśli zgodnych wersji nie ma, zostaw Kotlin i zapisz powód w `LOG.md`. Po zmianie pełne testy i lint.
5. Eksport: `contentResolver.openOutputStream(uri, "wt")`, aby nadpisanie istniejącego dokumentu obcinało plik.

Weryfikacja: `gradlew.bat test connectedDebugAndroidTest lintDebug`.

Kryterium zakończenia: każdy punkt wykonany albo odrzucony z uzasadnieniem w `LOG.md`.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`; I-14 czeka na decyzję o kopii zapasowej. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
