import importlib.util
import contextlib
import io
from pathlib import Path
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[2] / "scripts/run-client-compat.py"
spec = importlib.util.spec_from_file_location("client_runner", SCRIPT)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class ClientRunnerTest(unittest.TestCase):
    def run_client(self, source, **timeouts):
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "client.log"
            # Expected watchdog failures must not emit GitHub error annotations
            # while the unit test suite itself is succeeding.
            with contextlib.redirect_stdout(io.TextIOWrapper(io.BytesIO(), write_through=True)):
                code = runner.run([sys.executable, "-u", "-c", source], log, **timeouts)
            return code, log.read_text()

    def test_success_and_assertion_failure_are_preserved_without_retry(self):
        for expected in (0, 7):
            code, log = self.run_client(f"print('one attempt'); raise SystemExit({expected})")
            self.assertEqual(expected, code)
            self.assertEqual("one attempt\n", log)

    def test_one_failed_backend_allows_successful_fallback(self):
        code, _ = self.run_client("print('Failed to create backend Vulkan')")
        self.assertEqual(0, code)

    def test_both_failed_backends_stop_hung_dialog(self):
        code, log = self.run_client("import time; print('Failed to create backend OpenGL'); "
                                   "print('Failed to create backend Vulkan'); time.sleep(60)")
        self.assertEqual(124, code)
        self.assertIn("Both graphics backends failed", log)

    def test_preparation_is_bounded(self):
        code, log = self.run_client("import time; time.sleep(60)", setup_timeout=0.1)
        self.assertEqual(124, code)
        self.assertIn("Client preparation exceeded", log)

    def test_silent_game_is_bounded(self):
        code, log = self.run_client("import time; print('> Task :runner:runProductionClientGameTest'); "
                                   "time.sleep(60)", idle_timeout=0.1)
        self.assertEqual(124, code)
        self.assertIn("produced no output", log)

    def test_chatty_game_is_bounded(self):
        code, log = self.run_client("import time; print('> Task :runner:runProductionClientGameTest'); "
                                   "exec('while True:\\n print(\\\"still alive\\\"); time.sleep(0.02)')",
                                   game_timeout=0.1)
        self.assertEqual(124, code)
        self.assertIn("game test exceeded", log)


if __name__ == "__main__":
    unittest.main()
