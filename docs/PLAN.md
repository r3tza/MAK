# Plan najbliższych prac

Ten plik zawiera najwyżej pięć najbliższych kroków wykonawczych. Obecnie są trzy. Pełna lista zadań i oddzielny status odbioru są w `QUEUE.md`. Cel i zakres produktu opisuje `PRODUCT.md`, reguły planu `DOMAIN.md`, a zachowanie ekranów `FEATURES.md`. Po ukończeniu kroku uaktualnij kolejkę i wybierz następny; nie dopisuj tu historii wykonania.

## 1. Podzielić repozytorium danych (I-01)

1. Przypisać metody obecnego `MakRepository` do `SemesterRepository`, `ScheduleRepository` albo usługi kopii zapasowej. Zachować jedną granicę atomowego importu.
2. Przenieść najpierw implementacje i testy, potem konsumentów: ViewModele, widget oraz powiadomienia. Nie wystawiać encji Room w nowych kontraktach.
3. Usunąć stary interfejs dopiero po przeniesieniu ostatniego konsumenta. Sprawdzić transakcje i rollback importu.

Kryterium zakończenia: konsument korzysta z odpowiedniej granicy danych, a wspólne reguły planu nadal przechodzą przez `ActivePlanProvider`. Granice docelowe opisuje `ARCHITECTURE.md`.

## 2. Dokończyć podsumowanie „Dzisiaj” (I-02)

1. Dodać domenowe liczenie okienek z połączonych bloków zajęć wszystkich kierunków. Przerwa musi być dłuższa niż globalny próg, domyślnie 30 minut.
2. Zapisać próg w ustawieniach i udostępnić liczbę zajęć, unikalnych kolizji oraz okienek w stanie ekranu.
3. Pokazać trzy wartości w istniejącej karcie gradientowej. Sprawdzić stan pusty, semantykę, kontrast, oba motywy i szerokość 320 dp.

Kryterium zakończenia: logika daje ten sam wynik dla kolizji, przeniesień i odwołań, a karta spełnia układ opisany w `FEATURES.md`, sekcja „Ekran Dzisiaj”.

## 3. Uporządkować karty zajęć (I-03)

1. Zachować oddzielne dane notatki wspólnej i notatki wystąpienia aż do modelu prezentacyjnego. Nie scalać ich w jeden tekst.
2. Ułożyć czas, nazwę, kierunek, lokalizację, kolizję i notatki w osobne czytelne sekcje. Pill kierunku ma odpowiadać jego kolorowi, ale nazwa pozostaje widoczna.
3. Sprawdzić jedną i obie notatki, długie nazwy, 320 dp, motyw ciemny oraz kolejność odczytu przez TalkBack.

Kryterium zakończenia: oba rodzaje notatek są rozróżnialne tekstem i kolorem, a karta pozostaje czytelna bez poziomego przewijania. Szczegóły są w `FEATURES.md`, sekcja „Struktura karty zajęć”.

## Po tych krokach

Wybierz następne zadanie z `QUEUE.md`. Odbiór na urządzeniu można wykonywać niezależnie od powyższej kolejności; otwarte scenariusze są w `FEATURES.md`, sekcja „Odbiór na urządzeniu”.
