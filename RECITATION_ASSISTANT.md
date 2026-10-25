# Recitation practice in Nuraya 1.1.0

The Android app includes user-started Arabic speech recognition and word comparison for any numbered verse. It is **experimental word detection**, not a tajwid/makhraj judge. It must not be advertised as reliable real-time Qur’an correction.

## What actually runs

- Uses Android `SpeechRecognizer`, free for Nuraya to invoke without an API key. The user needs a compatible speech provider and Arabic model; availability is device-dependent.
- Prefers `createOnDeviceSpeechRecognizer` on Android 12/API 31+ when the platform reports availability. That check does not guarantee an Arabic model is installed. A language-unavailable error explains how to enable Arabic or switch providers.
- System speech processing is available only after the user explicitly checks the consent box. The selected Android provider can transmit audio to its servers, and data charges can apply. This is not claimed to be offline.
- Requests microphone permission only on the user's Start action. Pauses recitation audio before starting the microphone. Cancels and destroys recognition when the screen leaves composition or the activity stops. No automatic listening/restart in the background.
- Each attempt covers one selected verse. Partial recognition updates highlight matching words if the provider supplies partial results; timing and frequency are provider-controlled. Interim omissions or substitutions are not labelled mistakes. Final recognition marks possible missing/different words for review, with an explicit warning that the recognizer itself can be wrong.
- Uses bounded edit-distance alignment to handle insertions, omissions, and repetitions. Matching ignores vowel marks and normalizes common alef/hamza forms. Dagger alef may be represented by ordinary alef or omitted in the recognized spelling (e.g. `ٱلْعَٰلَمِينَ` → `العالمين`, `ٱلرَّحْمَٰنِ` → `الرحمن`, `ذَٰلِكَ` → `ذلك`). Ordinary written alefs are never stripped. A small explicit spelling table accepts `صلوة`/`زكوة`/`حيوة` with source dagger alef as `صلاة`/`زكاة`/`حياة`, with listed ordinary prefixes; it does not generally replace consonant waw. These are text comparison conventions, not a pronunciation evaluation. It cannot assess vowels, length of madd, ghunnah, or articulation. Other Uthmani/modern spelling differences can still produce false mismatches and are called out in the result UI.
- Expected text is never passed as a recognizer bias prompt. The target uses the existing numbered verse data after the verified opening-basmalah policy. For 2:1, the target is `الٓمٓ`; opening basmalah is not falsely scored as part of that verse. Disconnected letters can be transcribed as spoken letter names, another known limitation.
- Nuraya saves counts of completed recognition attempts and the set of practiced verse references in local app preferences. It stores no audio and no recognized transcript. Attempt counts are not correctness or memorization scores.
- A final result is requested after the user taps Finish. A 15-second final-result timeout and 90-second whole-attempt timeout release the microphone if a provider stalls.

## Free ready-made Qur’an AI research

Verified source: [Tarteel `whisper-base-ar-quran`](https://huggingface.co/tarteel-ai/whisper-base-ar-quran), revision `5c3c53fdf9272c4f6ee0bee09a1e5a4a615ee25c`. The model card declares Apache-2.0 and describes a Whisper-base fine-tune, with model-reported WER 5.7544. Its intended-use, limitation, and training-data sections are incomplete; that figure is not a Nuraya test result and cannot be translated into recitation-grading accuracy.

This is a downloadable model, **not evidence of an unlimited free production inference API**. It does not supply validated tajwid/makhraj correction. Model weights are not bundled and no remote endpoint is configured in this release. This research is documented here without exposing developer setup in the user flow. Integrating it would require on-device conversion/benchmarking or an explicitly operated server, resource/privacy decisions, and evaluation on real recitation errors. Public demo spaces are not treated as dependable APIs and no user audio is uploaded to a demo.

## Platform sources and verification

- [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer): main-thread API, network-processing warning, on-device availability, destroy requirement.
- [RecognitionListener](https://developer.android.com/reference/android/speech/RecognitionListener): partial results may arrive zero, one, or multiple times; results/error lifecycle.
- [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent): Arabic locale, partial results, offline preference semantics.
- Official Android documentation and AOSP class sources were downloaded during implementation; model card/API metadata were retrieved without running inference.

`RecitationMatcherTest` covers normalization, partial-tail handling, tentative partial mismatches, omitted/extra/repeated words, original display ranges, non-Arabic hypotheses, and final substitutions. No microphone/provider/device accuracy test has been performed in the cloud environment. Physical-device checks remain required for Arabic-model availability, permission denial, incoming-call/background interruption, and actual audio recognition.
