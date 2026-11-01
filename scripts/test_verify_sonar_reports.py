from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from verify_sonar_reports import verify


class ReportsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.build = Path(self.temporary.name)
        self.junit = self.build / "test-results/testDebugUnitTest/TEST-suite.xml"
        self.coverage = self.build / "reports/coverage/test/debug/report.xml"
        self.junit.parent.mkdir(parents=True)
        self.coverage.parent.mkdir(parents=True)
        self.junit.write_text('<testsuite tests="3" failures="0" errors="0" skipped="0"/>')
        self.coverage.write_text('<report><counter type="LINE" covered="4" missed="9"/></report>')

    def test_accepts_executed_passing_tests(self):
        with redirect_stdout(StringIO()):
            self.assertEqual(3, verify(self.build))

    def test_rejects_missing_junit(self):
        self.junit.unlink()
        with self.assertRaises(ValueError):
            verify(self.build)

    def test_rejects_failed_errored_skipped_and_empty_suites(self):
        for attributes in ('tests="0"', 'tests="3" failures="1"', 'tests="3" errors="1"', 'tests="3" skipped="1"'):
            with self.subTest(attributes=attributes):
                self.junit.write_text(f'<testsuite {attributes}/>')
                with self.assertRaises(ValueError):
                    verify(self.build)

    def test_rejects_missing_coverage(self):
        self.coverage.unlink()
        with self.assertRaises(FileNotFoundError):
            verify(self.build)

    def test_rejects_empty_and_zero_coverage(self):
        for content in ('<report/>', '<report><counter type="LINE" covered="0" missed="8"/></report>'):
            with self.subTest(content=content):
                self.coverage.write_text(content)
                with self.assertRaises(ValueError):
                    verify(self.build)


if __name__ == "__main__":
    unittest.main()
