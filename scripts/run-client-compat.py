#!/usr/bin/env python3
"""Run one client check, stopping broken graphics and hung process trees."""

import argparse
import os
from pathlib import Path
import re
import selectors
import signal
import subprocess
import sys
import time

PASSED_MARKER = "[svc_better_groups_client_test] compatibility game test passed"


def run(command, log_path, setup_timeout=900, game_timeout=300, idle_timeout=120):
    log_path.parent.mkdir(parents=True, exist_ok=True)
    started = last_output = time.monotonic()
    game_started = None
    backends_failed = set()
    pending = ""
    failure = None
    test_passed = False
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                               start_new_session=True)
    selector = selectors.DefaultSelector()
    selector.register(process.stdout, selectors.EVENT_READ)
    try:
        with log_path.open("wb") as log:
            while selector.get_map() or process.poll() is None:
                for key, _ in selector.select(timeout=0.2):
                    data = os.read(key.fd, 65536)
                    if not data:
                        selector.unregister(key.fileobj)
                        continue
                    log.write(data)
                    log.flush()
                    sys.stdout.buffer.write(data)
                    sys.stdout.buffer.flush()
                    last_output = time.monotonic()
                    pending += data.decode("utf-8", errors="replace")
                    lines = pending.split("\n")
                    pending = lines.pop()
                    for line in lines:
                        if PASSED_MARKER in line:
                            test_passed = True
                        if re.search(r"^> Task .*:runProductionClientGameTest(?:\s|$)", line):
                            game_started = game_started or time.monotonic()
                        match = re.search(r"Failed to create backend (OpenGL|Vulkan)", line)
                        if match:
                            backends_failed.add(match.group(1))
                now = time.monotonic()
                if len(backends_failed) == 2:
                    failure = "Both graphics backends failed; stopping before the error dialog can hang."
                elif game_started is not None and now - game_started > game_timeout:
                    failure = f"Client game test exceeded {game_timeout}s."
                elif game_started is not None and now - last_output > idle_timeout:
                    failure = f"Client game test produced no output for {idle_timeout}s."
                elif game_started is None and now - started > setup_timeout:
                    failure = f"Client preparation exceeded {setup_timeout}s."
                if failure:
                    message = f"\n::error::{failure}\n"
                    log.write(message.encode())
                    print(message, flush=True)
                    return 124
            result = process.wait()
            if result == 0 and not test_passed:
                message = "\n::error::Client exited without confirming that the compatibility game test passed.\n"
                log.write(message.encode())
                print(message, flush=True)
                return 1
            return result
    finally:
        selector.close()
        process.stdout.close()
        # Gradle and Minecraft are children in this dedicated session. Kill
        # the entire group, including a stuck native window or orphaned JVM.
        try:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                pass
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        process.wait()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", required=True, type=Path)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ["--"] else args.command
    if not command:
        parser.error("a command is required after --")
    sys.exit(run(command, args.log))
