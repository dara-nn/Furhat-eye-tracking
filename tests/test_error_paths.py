import subprocess
import time
import os
import sys

print("Stopping any currently running instances of the skill...")
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
time.sleep(3)

print("=== UNHAPPY PATH + SPONTANEOUS RESPONSE TEST ===")
print("Testing: consent confusion, pre-conversation hesitation,")
print("         silence in conversation, and recovery via follow-up turns.\n")

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

passed_consent = False
passed_pre_conversation = False
greeted = False

first_consent = True
first_pre_conversation = True
first_conversation = True
first_consent_retry = True
first_pre_conversation_retry = True

followups = 0
FOLLOWUP_LIMIT = 2

try:
    for line in iter(process.stdout.readline, ''):
        print(line, end="")

        # ---- WAKE-UP GREETING ----
        if ">>> ROBOT_LISTENING: IDLE" in line and not greeted:
            greeted = True
            say("Hi Iris.")

        # ---- CONSENT ----
        elif ">>> ROBOT_LISTENING: CONSENT_RETRY" in line and not passed_consent:
            if first_consent_retry:
                first_consent_retry = False
                say("Wait, what exactly are you recording?")
            else:
                passed_consent = True
                say("Okay, yes I consent to that.")
        elif ">>> ROBOT_LISTENING: CONSENT" in line and not passed_consent and first_consent:
            first_consent = False
            say("I'm not totally sure what you mean.")

        # ---- PRE-CONVERSATION ----
        elif ">>> ROBOT_LISTENING: PRE_CONVERSATION_RETRY" in line and not passed_pre_conversation:
            if first_pre_conversation_retry:
                first_pre_conversation_retry = False
                say("Can I get some water first?")
            else:
                passed_pre_conversation = True
                say("Yes, I am ready now.")
        elif ">>> ROBOT_LISTENING: PRE_CONVERSATION" in line and not passed_pre_conversation and first_pre_conversation:
            first_pre_conversation = False
            say("I need a minute to rest.")

        # ---- CONVERSATION - silence then answer ----
        elif ">>> ROBOT_LISTENING: CONVERSATION_OPEN" in line and first_conversation:
            first_conversation = False
            print("\n[TEST AGENT] >> Intentionally staying silent (testing silence prompt)...\n")

        # ---- CONVERSATION follow-up turns ----
        elif ">>> ROBOT_LISTENING: CONVERSATION_RETRY" in line or ">>> ROBOT_LISTENING: CONVERSATION_TURN" in line:
            followups += 1
            if followups == 1:
                say("My name is Sam and I work as a graphic designer.")
            elif followups == 2:
                say("I really love working with typography and color systems.")
            elif followups >= FOLLOWUP_LIMIT + 1:
                say("That is the gist of it, thank you.")
                print("\n[TEST AGENT] >> Unhappy Path test complete. Exiting in 15s...\n")
                time.sleep(15)
                os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
                sys.exit(0)

except KeyboardInterrupt:
    print("\nStopping...")
    process.kill()
    sys.exit(0)
