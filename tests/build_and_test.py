import subprocess
import time
import sys
import os

print("=== FURHAT EYE-TRACKING TEST SUITE ===")
print("Compiling the Kotlin Skill...")

build_process = subprocess.run(["./gradlew", "classes"], cwd="/Users/Dara/Documents/Furhat-eye-tracking")

if build_process.returncode != 0:
    print("Build failed! Aborting tests.")
    sys.exit(1)

print("\n--- Running HAPPY PATH tests ---")
subprocess.run(["python3", "tests/test_runner.py"], cwd="/Users/Dara/Documents/Furhat-eye-tracking")

print("\n--- Running UNHAPPY PATH tests ---")
subprocess.run(["python3", "tests/test_error_paths.py"], cwd="/Users/Dara/Documents/Furhat-eye-tracking")

print("\n--- Running EDGE-EXIT / QUIET-MODE tests ---")
subprocess.run(["python3", "tests/test_quit_path.py"], cwd="/Users/Dara/Documents/Furhat-eye-tracking")

print("\n=== ALL TESTS COMPLETED ===")
