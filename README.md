# Furhat Eye-Tracking Research Study

An automated FurhatOS skill for an eye-tracking research study. The robot handles participant consent, glasses setup, calibration, and two conversational tasks.

## Features

- **Automated Research Flow**: Guided process from consent to study completion.
- **Eye-Tracking Integration**: Waits for external calibration success before proceeding.
- **Dual-Task Structure**:
    - **Task 1**: Discussion about a favorite travel destination.
    - **Task 2**: Interactive storytelling and listening.
- **Automated Test Agents**: Python-based simulators using macOS TTS to automate interaction testing.

## Project Structure

```text
src/main/kotlin/furhatos/app/eyetracking/
├── flow/
│   ├── init.kt         # Entry point, consent, and setup logic
│   └── interaction.kt  # Main study tasks and conversation states
├── setting/
│   └── persona.kt      # Research assistant persona definition
├── chatbot/
│   └── gemini.kt       # Gemini API integration for dynamic dialogue
└── main.kt             # Skill configuration and entry
```

## Setup

1. **Gradle Build**:
   ```bash
   ./gradlew shadowJar
   ```
2. **Run Locally**:
   ```bash
   ./gradlew run
   ```
3. **API Key**: Ensure your Gemini API key is configured in `src/main/kotlin/furhatos/app/eyetracking/chatbot/gemini.kt`.

## Automated Testing

The project includes an "Automated Test Agent" to verify the interaction flow without manual speech.

- **Run master test suite**:
  ```bash
  python3 build_and_test.py
  ```
- **Run individual tests**:
  - `python3 test_runner.py` (Happy Path)
  - `python3 test_error_paths.py` (Error recovery and timeouts)

## Requirements

- [FurhatOS SDK](https://furhatrobotics.com/docs/)
- Python 3 (for test agents)
- Gemini API Key
