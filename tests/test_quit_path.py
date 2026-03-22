import subprocess
import time
import os
import sys

print("Stopping any currently running instances of the skill...")
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
time.sleep(3)

print("=== EARLY TERMINATION PATHS TEST ===")
print("Testing: 2-Strike Consent Refusal and 3-Strike Silence Refusal\n")

# TEST 1: Consent Refusal
print("\n--- RUNNING TEST 1: 2-Strike Consent Refusal ---")
process1 = subprocess.Popen(
    ["./gradlew", "run"], 
    stdout=subprocess.PIPE, 
    stderr=subprocess.STDOUT, 
    text=True, 
    cwd="/Users/Dara/Documents/Furhat-eye-tracking"
)

def say(text):
    print(f"\n[TEST AGENT] >> Saying: '{text}' over speaker...\n")
    safe_text = text.replace("'", "'\\''")
    subprocess.Popen(f"sleep 1 && say -v Samantha '{safe_text}'", shell=True)

passed_consent = False
try:
    for line in iter(process1.stdout.readline, ''):
        print(line, end="")
        if ">>> ROBOT_LISTENING: CONSENT_RETRY" in line:
            say("No, I changed my mind.") # Strike 2 -> Should go to Idle
            time.sleep(5)
            break
        elif ">>> ROBOT_LISTENING: CONSENT" in line:
            say("No.") # Strike 1
            
except Exception:
    pass

time.sleep(15)
process1.kill()
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")


# TEST 2: Task 1 Silence Timeout
print("\n--- RUNNING TEST 2: 3-Strike Silence in Task 1 ---")
process2 = subprocess.Popen(
    ["./gradlew", "run"], 
    stdout=subprocess.PIPE, 
    stderr=subprocess.STDOUT, 
    text=True, 
    cwd="/Users/Dara/Documents/Furhat-eye-tracking"
)

try:
    for line in iter(process2.stdout.readline, ''):
        print(line, end="")
        if ">>> ROBOT_LISTENING: CONSENT" in line or ">>> ROBOT_LISTENING: CONSENT_RETRY" in line:
            say("Yes")
        elif ">>> ROBOT_LISTENING: GLASSES" in line or ">>> ROBOT_LISTENING: GLASSES_RETRY" in line:
            say("Yes")
        elif ">>> ROBOT_LISTENING: CALIBRATION" in line or ">>> ROBOT_LISTENING: CALIBRATION_RETRY" in line:
            say("Yes")
        elif ">>> ROBOT_LISTENING: PRE_TASK1" in line or ">>> ROBOT_LISTENING: PRE_TASK1_RETRY" in line:
            say("Yes")
        elif ">>> ROBOT_LISTENING: TASK1" in line:
            print("\n[TEST AGENT] >> Will now stay completely silent for 40s to trigger the timeouts...\n")
            time.sleep(40)
            break
            
except Exception:
    pass

# The user can naturally observe both terminations
time.sleep(5)
process2.kill()
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")

sys.exit(0)
