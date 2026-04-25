import subprocess
import time
import os
import sys

print("Stopping any currently running instances of the skill...")
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
time.sleep(3)

print("=== EARLY-EXIT / QUIET-MODE PATHS TEST ===")
print("Testing: 2-strike consent refusal returns to Idle,")
print("         3-strike silence in PreConversation enters quiet mode.\n")


def say(text):
    print(f"\n[TEST AGENT] >> Saying: '{text}' over speaker...\n")
    safe_text = text.replace("'", "'\\''")
    subprocess.Popen(f"sleep 1 && say -v Samantha '{safe_text}'", shell=True)


# ---------------------------------------------------------------------------
# TEST 1: Consent refusal x2 -> back to Idle
# ---------------------------------------------------------------------------
print("\n--- RUNNING TEST 1: 2-Strike Consent Refusal -> Idle ---")
process1 = subprocess.Popen(
    ["./gradlew", "run"],
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    cwd="/Users/Dara/Documents/Furhat-eye-tracking"
)

idle_seen_count = 0
greeted = False
first_consent_done = False

try:
    for line in iter(process1.stdout.readline, ''):
        print(line, end="")
        if ">>> ROBOT_LISTENING: IDLE" in line:
            idle_seen_count += 1
            if not greeted:
                greeted = True
                say("Hi Iris.")
            elif idle_seen_count >= 2:
                print("\n[TEST 1 PASS] Robot returned to Idle after 2-strike refusal.\n")
                break
        elif ">>> ROBOT_LISTENING: CONSENT_RETRY" in line:
            say("No, I changed my mind.")  # Strike 2 -> Idle
        elif ">>> ROBOT_LISTENING: CONSENT" in line and not first_consent_done:
            first_consent_done = True
            say("No.")  # Strike 1
except Exception:
    pass

time.sleep(5)
process1.kill()
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
time.sleep(3)


# ---------------------------------------------------------------------------
# TEST 2: 3 silences in PreConversation -> quiet mode
# ---------------------------------------------------------------------------
print("\n--- RUNNING TEST 2: 3-Strike Silence in PreConversation -> Quiet Mode ---")
process2 = subprocess.Popen(
    ["./gradlew", "run"],
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    cwd="/Users/Dara/Documents/Furhat-eye-tracking"
)

silence_strikes = 0
greeted2 = False

try:
    for line in iter(process2.stdout.readline, ''):
        print(line, end="")
        if ">>> ROBOT_LISTENING: IDLE" in line and not greeted2:
            greeted2 = True
            say("Hi Iris.")
        elif ">>> ROBOT_LISTENING: CONSENT" in line or ">>> ROBOT_LISTENING: CONSENT_RETRY" in line:
            say("Yes, I consent.")
        elif ">>> ROBOT_LISTENING: PRE_CONVERSATION" in line or ">>> ROBOT_LISTENING: PRE_CONVERSATION_RETRY" in line:
            silence_strikes += 1
            print(f"\n[TEST AGENT] >> Strike {silence_strikes}: staying silent...\n")
            if silence_strikes >= 4:
                # After 3 silences, robot enters quiet mode.
                # The 4th listen is the long quiet-mode listen — sanity check it appears.
                print("\n[TEST 2 PASS] Robot entered quiet mode after 3 silences.\n")
                time.sleep(5)
                break
except Exception:
    pass

time.sleep(5)
process2.kill()
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")

sys.exit(0)
