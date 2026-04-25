import subprocess
import time
import os
import sys

print("Stopping any currently running instances of the skill...")
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
if os.path.exists("test_mode.txt"):
    os.remove("test_mode.txt")
time.sleep(3)

print("=== HAPPY PATH TEST ===")
print("Greet -> consent -> ready -> 4 conversational turns -> exit.\n")

process = subprocess.Popen(
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

turn_count = 0
TURN_LIMIT = 4

try:
    for line in iter(process.stdout.readline, ''):
        print(line, end="")

        # 0. WAKE-UP GREETING
        if ">>> ROBOT_LISTENING: IDLE" in line:
            say("Hi Iris.")

        # 1. CONSENT
        elif ">>> ROBOT_LISTENING: CONSENT" in line or ">>> ROBOT_LISTENING: CONSENT_RETRY" in line:
            say("Yes, I fully consent to the recording.")

        # 2. PRE-CONVERSATION
        elif ">>> ROBOT_LISTENING: PRE_CONVERSATION" in line or ">>> ROBOT_LISTENING: PRE_CONVERSATION_RETRY" in line:
            say("Yes, I am ready to begin.")

        # 3. CONVERSATION - opening intro
        elif ">>> ROBOT_LISTENING: CONVERSATION_OPEN" in line:
            say("My name is Alex and I study computer science.")

        # 4. CONVERSATION - follow-up turns
        elif ">>> ROBOT_LISTENING: CONVERSATION_TURN" in line or ">>> ROBOT_LISTENING: CONVERSATION_RETRY" in line:
            turn_count += 1
            if turn_count == 1:
                say("I really enjoy machine learning, especially building small language models.")
            elif turn_count == 2:
                say("My favorite project so far was a chatbot that helped my classmates revise for exams.")
            elif turn_count == 3:
                say("Outside of studying, I love hiking and trying new coffee shops around the city.")
            elif turn_count >= TURN_LIMIT:
                say("That is pretty much my week in a nutshell.")
                print(f"\n[TEST AGENT] >> Reached {TURN_LIMIT} conversation turns. Exiting in 15s...\n")
                time.sleep(15)
                os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
                sys.exit(0)

except KeyboardInterrupt:
    print("\nStopping...")
    process.kill()
    sys.exit(0)
