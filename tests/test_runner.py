import subprocess
import time
import os
import sys

print("Stopping any currently running instances of the skill...")
os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
if os.path.exists("test_mode.txt"):
    os.remove("test_mode.txt")
time.sleep(3)

print("Starting the skill and tracking REAL-TIME dynamic tags for Voice Synthesis...")

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

task1_responses = 0

try:
    for line in iter(process.stdout.readline, ''):
        print(line, end="")
        
        # 1. CONSENT
        if ">>> ROBOT_LISTENING: CONSENT" in line or ">>> ROBOT_LISTENING: CONSENT_RETRY" in line:
            say("Yes, I fully consent to the recording.")
            
        # 2. GLASSES
        elif ">>> ROBOT_LISTENING: GLASSES" in line or ">>> ROBOT_LISTENING: GLASSES_RETRY" in line:
            say("Yes, I have put them on.")
            
        # 3. CALIBRATION
        elif ">>> ROBOT_LISTENING: CALIBRATION" in line or ">>> ROBOT_LISTENING: CALIBRATION_RETRY" in line:
            say("Yes, the calibration is successful. I am ready.")
            
        # 4. TASK 1
        elif ">>> ROBOT_LISTENING: PRE_TASK1" in line or ">>> ROBOT_LISTENING: PRE_TASK1_RETRY" in line:
            say("I have rested. I am ready to begin the first task.")
            
        elif ">>> ROBOT_LISTENING: TASK1_CONVERSATION" in line or ">>> ROBOT_LISTENING: TASK1_CONVERSATION_RETRY" in line:
            task1_responses += 1
            if task1_responses == 1:
                say("The language barrier was definitely the most challenging part, but it was a great learning experience.")
            elif task1_responses == 2:
                say("I also found the food to be completely incredible, even if I did not always know what I was ordering.")
            elif task1_responses == 3:
                say("Overall, I would highly recommend it to anyone who likes exploring new cultures.")
            elif task1_responses == 4:
                say("Well, another thing is that the public transportation was surprisingly easy to navigate.")
            
        elif ">>> ROBOT_LISTENING: TASK1" in line or ">>> ROBOT_LISTENING: TASK1_RETRY" in line:
            say("My favorite travel destination is Kyoto, Japan, because of its beautiful temples and peaceful gardens.")
            
        # 5. TASK 2
        elif ">>> ROBOT_LISTENING: PRE_TASK2" in line or ">>> ROBOT_LISTENING: PRE_TASK2_RETRY" in line:
            say("Yes, I am ready to begin the story.")
            
        elif ">>> ROBOT_LISTENING: TASK2" in line:
            say("Yes, I found that very interesting. Thank you.")
            time.sleep(10)
            os.system("pkill -f 'furhatos.app.eyetracking.MainKt' || true")
            sys.exit(0)

except KeyboardInterrupt:
    print("\nStopping...")
    process.kill()
    sys.exit(0)
