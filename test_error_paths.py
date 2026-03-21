import subprocess
import time
import os
import sys

print("Stopping any currently running instances of the skill...")
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
time.sleep(3)

print("=== UNHAPPY PATH + SPONTANEOUS RESPONSE TEST ===")
print("Testing: consent confusion, glasses refusal, calibration delay,")
print("         random off-topic remarks, silence timeouts, and recovery.\n")

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

# Simple flags: once a phase is PASSED, stop responding to it
passed_consent = False
passed_glasses = False
passed_calibration = False
passed_pre_task1 = False
passed_task1 = False
passed_task1_followup = False
passed_pre_task2 = False

# Track first-attempt for deliberate failures
first_consent = True
first_glasses = True
first_calibration = True
first_pre_task1 = True
first_task1 = True
first_pre_task2 = True
first_glasses_retry = True
first_calibration_retry = True
first_pre_task1_retry = True

try:
    for line in iter(process.stdout.readline, ''):
        print(line, end="")
        
        # ---- CONSENT ----
        if (">>> ROBOT_LISTENING: CONSENT_RETRY" in line) and not passed_consent:
            # Always answer yes on retry
            passed_consent = True
            say("Okay, yes I consent to that.")
        elif (">>> ROBOT_LISTENING: CONSENT" in line) and not passed_consent and first_consent:
            first_consent = False
            say("Wait, what exactly are you recording?")
        
        # ---- GLASSES ----
        elif (">>> ROBOT_LISTENING: GLASSES_RETRY" in line) and not passed_glasses:
            if first_glasses_retry:
                first_glasses_retry = False
                say("What do these glasses even do?")
            else:
                passed_glasses = True
                say("Yes, I have put them on now.")
        elif (">>> ROBOT_LISTENING: GLASSES" in line) and not passed_glasses and first_glasses:
            first_glasses = False
            say("No, not yet.")
        
        # ---- CALIBRATION ----
        elif (">>> ROBOT_LISTENING: CALIBRATION_RETRY" in line) and not passed_calibration:
            if first_calibration_retry:
                first_calibration_retry = False
                say("How long does this usually take?")
            else:
                passed_calibration = True
                say("Alright, the calibration is done now.")
        elif (">>> ROBOT_LISTENING: CALIBRATION" in line) and not passed_calibration and first_calibration:
            first_calibration = False
            say("Not yet, I am still calibrating.")
        
        # ---- PRE-TASK 1 ----
        elif (">>> ROBOT_LISTENING: PRE_TASK1_RETRY" in line) and not passed_pre_task1:
            if first_pre_task1_retry:
                first_pre_task1_retry = False
                say("Can I get some water first?")
            else:
                passed_pre_task1 = True
                say("Yes, I am ready now.")
        elif (">>> ROBOT_LISTENING: PRE_TASK1" in line) and not passed_pre_task1 and first_pre_task1:
            first_pre_task1 = False
            say("I need a minute to rest.")
        
        # ---- TASK 1 (silence then answer) ----
        elif (">>> ROBOT_LISTENING: TASK1_RETRY" in line) and not passed_task1:
            passed_task1 = True
            say("My favorite trip was to France, the food was amazing.")
        elif (">>> ROBOT_LISTENING: TASK1_FOLLOWUP_RETRY" in line) and not passed_task1_followup:
            passed_task1_followup = True
            say("The flight there was definitely the hardest part.")
        elif (">>> ROBOT_LISTENING: TASK1_FOLLOWUP" in line) and not passed_task1_followup:
            passed_task1_followup = True
            say("The flight there was definitely the hardest part.")
        elif (">>> ROBOT_LISTENING: TASK1" in line) and first_task1:
            first_task1 = False
            print("\n[TEST AGENT] >> Intentionally staying silent (testing 40s timeout)...\n")
        
        # ---- PRE-TASK 2 ----
        elif (">>> ROBOT_LISTENING: PRE_TASK2_RETRY" in line) and not passed_pre_task2:
            passed_pre_task2 = True
            say("Alright, I am ready for the story.")
        elif (">>> ROBOT_LISTENING: PRE_TASK2" in line) and not passed_pre_task2 and first_pre_task2:
            first_pre_task2 = False
            say("I am a bit tired, can I take a break?")
        
        # ---- TASK 2 ----
        elif ">>> ROBOT_LISTENING: TASK2" in line:
            say("Yes, I found that very interesting. Thank you.")
            print("\n[TEST AGENT] >> Unhappy Path + Spontaneous Test complete! Exiting in 10s...")
            time.sleep(10)
            os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
            sys.exit(0)

except KeyboardInterrupt:
    print("\nStopping...")
    process.kill()
    sys.exit(0)
