"""Recusa análise sem testes executados e cobertura JaCoCo mensurável."""

from pathlib import Path
import xml.etree.ElementTree as ET


def verify(build: Path) -> int:
    reports = sorted((build / "test-results/testDebugUnitTest").glob("TEST-*.xml"))
    if not reports:
        raise ValueError("Nenhum relatório JUnit encontrado")
    executed = 0
    for report in reports:
        suite = ET.parse(report).getroot()
        tests = int(suite.attrib["tests"])
        skipped = int(suite.get("skipped", "0"))
        if tests < 0 or skipped < 0 or skipped > tests:
            raise ValueError(f"Contagem JUnit inválida: {report.name}")
        if int(suite.get("failures", "0")) or int(suite.get("errors", "0")) or skipped:
            raise ValueError(f"Testes falharam ou foram ignorados: {report.name}")
        executed += tests
    if not executed:
        raise ValueError("Zero testes não é aprovação")
    coverage = ET.parse(build / "reports/coverage/test/debug/report.xml").getroot()
    lines = coverage.find("counter[@type='LINE']")
    if lines is None or int(lines.get("covered", "0")) <= 0:
        raise ValueError("JaCoCo não registrou linhas cobertas")
    print(f"JUnit: {executed} testes aprovados; JaCoCo: {lines.attrib['covered']} linhas cobertas")
    return executed


if __name__ == "__main__":
    verify(Path("app/build"))
