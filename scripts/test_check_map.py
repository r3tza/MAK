#!/usr/bin/env python3
"""Testy skryptu check_map.py na katalogach tymczasowych."""

from __future__ import annotations

import os
import re
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import check_map  # noqa: E402

VALID_PLAN = """# Plan

## 1. Zrobić rzecz (I-01)

## 2. Zrobić drugą (I-02)

## Po tych krokach

Wybierz następne zadanie.
"""

VALID_QUEUE = """# Kolejka

| ID | Status | Zadanie i kryterium zakończenia | Zależność |
|---|---|---|---|
| I-01 | do implementacji | Zrobić rzecz. | brak |
| I-02 | w toku | Zrobić drugą. | I-01 |
"""

VALID_LOG = """# Log

## 2026-09-22: Wpis

- fakt
"""

VALID_README = """# README

[Plan](docs/PLAN.md)
"""


def write_repo(
    root: Path,
    plan: str = VALID_PLAN,
    queue: str = VALID_QUEUE,
    log: str = VALID_LOG,
    readme: str = VALID_README,
) -> None:
    (root / "README.md").write_text(readme, encoding="utf-8")
    (root / "AGENTS.md").write_text("# AGENTS\n", encoding="utf-8")
    (root / "CLAUDE.md").write_text("# CLAUDE\n", encoding="utf-8")
    docs = root / "docs"
    docs.mkdir(exist_ok=True)
    (docs / "PLAN.md").write_text(plan, encoding="utf-8")
    (docs / "QUEUE.md").write_text(queue, encoding="utf-8")
    (docs / "LOG.md").write_text(log, encoding="utf-8")


class CheckMapTest(unittest.TestCase):
    def setUp(self) -> None:
        self._temp = tempfile.TemporaryDirectory()
        self.root = Path(self._temp.name)

    def tearDown(self) -> None:
        self._temp.cleanup()

    def errors(self) -> list[str]:
        return check_map.check_repository(self.root)

    def assertHasError(self, needle: str) -> None:
        errors = self.errors()
        self.assertTrue(
            any(needle in error for error in errors),
            f"Brak błędu zawierającego {needle!r} w {errors!r}",
        )

    def assertErrorMatches(self, pattern: str) -> None:
        errors = self.errors()
        self.assertTrue(
            any(re.search(pattern, error) for error in errors),
            f"Brak błędu pasującego do {pattern!r} w {errors!r}",
        )

    def test_valid_repository_has_no_errors(self) -> None:
        write_repo(self.root)
        self.assertEqual([], self.errors())

    def test_missing_link_is_reported(self) -> None:
        write_repo(self.root, readme="# README\n\n[Plan](docs/MISSING.md)\n")
        self.assertHasError("nieistniejącego pliku")

    def test_missing_link_in_contributing_is_reported(self) -> None:
        write_repo(self.root)
        (self.root / "CONTRIBUTING.md").write_text(
            "# Współpraca\n\n[Produkt](docs/MISSING.md)\n", encoding="utf-8"
        )
        self.assertHasError("nieistniejącego pliku")

    def test_wrong_case_link_is_reported(self) -> None:
        write_repo(self.root, readme="# README\n\n[Plan](docs/plan.md)\n")
        self.assertHasError("nieistniejącego pliku")

    def test_sixth_plan_step_is_reported(self) -> None:
        steps = "".join(
            f"## {index}. Krok {index} (I-0{index})\n\n" for index in range(1, 7)
        )
        queue = (
            "| ID | Status | Zadanie i kryterium zakończenia | Zależność |\n"
            "|---|---|---|---|\n"
            + "".join(
                f"| I-0{index} | do implementacji | Krok {index}. | brak |\n"
                for index in range(1, 7)
            )
        )
        write_repo(self.root, plan=steps, queue=queue)
        self.assertHasError("limit to 5")
        self.assertErrorMatches(r"PLAN\.md:\d+: kroków planu")

    def test_missing_step_id_is_reported(self) -> None:
        write_repo(
            self.root,
            plan="# Plan\n\n## 1. Zrobić rzecz (I-09)\n\n## Po tych krokach\n",
        )
        self.assertHasError("nie istnieje w QUEUE.md")

    def test_duplicate_queue_id_is_reported(self) -> None:
        queue = (
            "| ID | Status | Zadanie i kryterium zakończenia | Zależność |\n"
            "|---|---|---|---|\n"
            "| I-01 | do implementacji | Raz. | brak |\n"
            "| I-01 | do implementacji | Dwa. | brak |\n"
        )
        write_repo(
            self.root,
            plan="# Plan\n\n## 1. Zrobić rzecz (I-01)\n\n## Po tych krokach\n",
            queue=queue,
        )
        self.assertHasError("powtórzony identyfikator")

    def test_invalid_status_is_reported(self) -> None:
        queue = (
            "| ID | Status | Zadanie i kryterium zakończenia | Zależność |\n"
            "|---|---|---|---|\n"
            "| I-01 | robi się | Raz. | brak |\n"
        )
        write_repo(
            self.root,
            plan="# Plan\n\n## 1. Zrobić rzecz (I-01)\n\n## Po tych krokach\n",
            queue=queue,
        )
        self.assertHasError("nieznany status")

    def test_dependency_to_unknown_id_is_reported(self) -> None:
        queue = (
            "| ID | Status | Zadanie i kryterium zakończenia | Zależność |\n"
            "|---|---|---|---|\n"
            "| I-01 | do implementacji | Raz. | I-99 |\n"
        )
        write_repo(
            self.root,
            plan="# Plan\n\n## 1. Zrobić rzecz (I-01)\n\n## Po tych krokach\n",
            queue=queue,
        )
        self.assertHasError("zależność wskazuje nieistniejące zadanie")

    def test_twenty_first_log_entry_is_reported(self) -> None:
        entries = "".join(
            f"## 2026-09-{day:02d}: Wpis {day}\n\n- fakt\n\n" for day in range(1, 22)
        )
        write_repo(self.root, log="# Log\n\n" + entries)
        self.assertHasError("limit to 20")
        self.assertErrorMatches(r"LOG\.md:\d+: wpisów w logu")


if __name__ == "__main__":
    unittest.main()
