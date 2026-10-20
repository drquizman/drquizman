Validation: 2026-09-18

:voices:assembleDebug PASS
:voices:testDebugUnitTest PASS — 4 tests, zero failures
:voices:lintDebug PASS — zero errors; 7 non-blocking warnings (SDK/dependency versions, TV banner, intentional exported TTS service, backup metadata)
:tv:assembleDebug PASS

Android TV emulator: API 34, x86, 2 GB RAM
TtsIntegrationTest PASS (17.148 s):
- Connect through android.speech.tts.TextToSpeech to com.drquizman.voices
- Enumerate 10 local Russian voices
- Reject unsupported Japanese locale
- Select M1 and F1, synthesize non-silent 44100 Hz WAV at different speech rates
- Stop a long queued utterance and complete a new synthesis request

UI checked: manager screen has no clipped primary controls; scrollable layout and native focus controls.
Game checked: engine displayed in voice settings; selecting Quizman Voices exposes Supertonic Russian voices.
Screenshots: ../build/voices.png and ../build/quizman-voices.png

No physical ARM device was connected, so performance and pronunciation quality there are not certified.
Final change after synthesis tests only adjusts Android 8 navigation-bar styling; final APK rebuilt and lint rechecked.
