#!/usr/bin/env python3
"""Sprawdza spójność dokumentacji MAK.

Skrypt czyta wyłącznie pliki repozytorium, używa standardowej biblioteki
Pythona i nie uruchamia Gradle ani sieci. Zwraca 0, gdy dokumenty są spójne,
albo 1, gdy znajdzie niezgodność.
"""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

ALLOWED_STATUSES = {
    "do implementacji",
    "w toku",
    "odbiór otwarty",
    "gotowe",
    "zablokowane",
}
QUEUE_COLUMNS = ["ID", "Status", "Zadanie i kryterium zakończenia", "Zależność"]
PLAN_STEP_RE = re.compile(r"^##\s+\d+\.\s+.*\(([IO]-\d+)\)\s*$")
PLAN_STEP_PREFIX_RE = re.compile(r"^##\s+\d+\.")
LOG_ENTRY_RE = re.compile(r"^##\s+\d{4}-\d{2}-\d{2}:\s+.+$")
QUEUE_ID_RE = re.compile(r"^[IO]-\d+$")
DEPENDENCY_ID_RE = re.compile(r"[IO]-\d+")
LINK_RE = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
EXTERNAL_PREFIXES = ("http://", "https://", "mailto:", "tel:", "data:")

ROOT_DOCUMENTS = ("README.md", "AGENTS.md", "CLAUDE.md", "CONTRIBUTING.md")


def active_documents(root: Path) -> list[Path]:
    documents: list[Path] = []
    for name in ROOT_DOCUMENTS:
        candidate = root / name
        if candidate.is_file():
            documents.append(candidate)
    docs_dir = root / "docs"
    if docs_dir.is_dir():
        documents.extend(
            sorted(path for path in docs_dir.glob("*.md") if path.is_file())
        )
    return documents


def case_sensitive_exists(path: Path) -> bool:
    normalized = Path(os.path.normpath(str(path)))
    parts = normalized.parts
    if not parts:
        return False
    if normalized.is_absolute():
        current = Path(parts[0])
        remaining = parts[1:]
    else:
        current = Path(".")
        remaining = parts
    for part in remaining:
        if part in ("", "."):
            continue
        if part == "..":
            current = current.parent
            continue
        try:
            entries = os.listdir(current)
        except OSError:
            return False
        if part not in entries:
            return False
        current = current / part
    return True


def check_links(document: Path, errors: list[str]) -> None:
    text = document.read_text(encoding="utf-8")
    for number, line in enumerate(text.splitlines(), start=1):
        for target in LINK_RE.findall(line):
            candidate = target.strip()
            if not candidate or candidate.startswith("#"):
                continue
            if candidate.startswith(EXTERNAL_PREFIXES):
                continue
            if re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:", candidate):
                continue
            path_part = candidate.split("#", 1)[0].split("?", 1)[0]
            if not path_part:
                continue
            resolved = Path(os.path.normpath(str(document.parent / path_part)))
            if not case_sensitive_exists(resolved):
                errors.append(
                    f"{document}:{number}: odnośnik do nieistniejącego pliku: {target}"
                )


def read_queue(queue_path: Path, errors: list[str]) -> set[str]:
    ids: set[str] = set()
    seen: set[str] = set()
    rows: list[tuple[int, list[str]]] = []
    text = queue_path.read_text(encoding="utf-8")
    for number, line in enumerate(text.splitlines(), start=1):
        stripped = line.strip()
        if not stripped.startswith("|"):
            continue
        cells = [cell.strip() for cell in stripped.strip("|").split("|")]
        if cells == QUEUE_COLUMNS:
            continue
        if all(set(cell) <= {"-", ":", " "} for cell in cells):
            continue
        if len(cells) != len(QUEUE_COLUMNS):
            errors.append(
                f"{queue_path}:{number}: wiersz tabeli ma {len(cells)} kolumn, "
                f"oczekiwano {len(QUEUE_COLUMNS)}."
            )
            continue
        rows.append((number, cells))

    for number, cells in rows:
        task_id, status, _, _ = cells
        if not QUEUE_ID_RE.match(task_id):
            errors.append(f"{queue_path}:{number}: niepoprawny identyfikator zadania: {task_id}")
            continue
        if task_id in seen:
            errors.append(f"{queue_path}:{number}: powtórzony identyfikator zadania: {task_id}")
        seen.add(task_id)
        ids.add(task_id)
        if status not in ALLOWED_STATUSES:
            errors.append(f"{queue_path}:{number}: nieznany status zadania: {status}")

    for number, cells in rows:
        dependency = cells[3]
        if not dependency or dependency == "brak":
            continue
        match = DEPENDENCY_ID_RE.search(dependency)
        if match is None:
            errors.append(
                f"{queue_path}:{number}: zależność bez identyfikatora zadania: {dependency}"
            )
        elif match.group(0) not in ids:
            errors.append(
                f"{queue_path}:{number}: zależność wskazuje nieistniejące zadanie: "
                f"{match.group(0)}"
            )
    return ids


def check_plan(plan_path: Path, queue_ids: set[str], errors: list[str]) -> None:
    text = plan_path.read_text(encoding="utf-8")
    steps: list[tuple[int, str]] = []
    for number, line in enumerate(text.splitlines(), start=1):
        if not PLAN_STEP_PREFIX_RE.match(line):
            continue
        match = PLAN_STEP_RE.match(line)
        if match is None:
            errors.append(
                f"{plan_path}:{number}: krok planu bez formatu '## N. tytuł (ID)'."
            )
            continue
        steps.append((number, match.group(1)))
    if len(steps) > 5:
        excess_line = steps[5][0]
        errors.append(
            f"{plan_path}:{excess_line}: kroków planu jest {len(steps)}, limit to 5."
        )
    for number, step_id in steps:
        if step_id not in queue_ids:
            errors.append(
                f"{plan_path}:{number}: identyfikator {step_id} nie istnieje w QUEUE.md."
            )


def check_log(log_path: Path, errors: list[str]) -> None:
    text = log_path.read_text(encoding="utf-8")
    entries = [
        number
        for number, line in enumerate(text.splitlines(), start=1)
        if LOG_ENTRY_RE.match(line)
    ]
    if len(entries) > 20:
        excess_line = entries[20]
        errors.append(
            f"{log_path}:{excess_line}: wpisów w logu jest {len(entries)}, limit to 20."
        )


def check_repository(root: Path) -> list[str]:
    root = root.resolve()
    errors: list[str] = []
    for document in active_documents(root):
        check_links(document, errors)

    queue_path = root / "docs" / "QUEUE.md"
    queue_ids: set[str] = set()
    if queue_path.is_file():
        queue_ids = read_queue(queue_path, errors)

    plan_path = root / "docs" / "PLAN.md"
    if plan_path.is_file():
        check_plan(plan_path, queue_ids, errors)

    log_path = root / "docs" / "LOG.md"
    if log_path.is_file():
        check_log(log_path, errors)

    return errors


def main(argv: list[str]) -> int:
    root = Path(argv[1]) if len(argv) > 1 else Path(__file__).resolve().parent.parent
    errors = check_repository(root)
    for error in errors:
        print(error)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
