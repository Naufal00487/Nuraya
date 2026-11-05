package com.nuraya.app.recitation.ai;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Host harness for the same JNI symbols packaged in Android; not a device test. */
public final class QuranWhisperEngine {
    private native long nativeCreate(String path);
    private native long nativeGeneration(long id);
    private native byte[] nativeTranscribe(long id, float[] samples, long generation, int threads);
    private native void nativeCancel(long id);
    private native void nativeRelease(long id);

    private static void expect(boolean value, String failure) {
        if (!value) throw new AssertionError(failure);
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("library.so model.bin mono-16k-f32le.pcm [--probe more-pcm-files...]");
        System.load(Path.of(args[0]).toAbsolutePath().toString());
        QuranWhisperEngine engine = new QuranWhisperEngine();
        long handle = engine.nativeCreate(args[1]);
        expect(handle > 0, "Model did not load");
        if (args.length >= 4 && args[3].equals("--probe")) {
            try {
                for (int index = 2; index < args.length; index++) {
                    if (index == 3) continue;
                    byte[] bytes = Files.readAllBytes(Path.of(args[index]));
                    float[] probe = new float[bytes.length / Float.BYTES];
                    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(probe);
                    Arrays.fill(bytes, (byte) 0);
                    long start = System.nanoTime();
                    String result = new String(engine.nativeTranscribe(handle, probe, engine.nativeGeneration(handle), 4), StandardCharsets.UTF_8).trim();
                    Arrays.fill(probe, 0f);
                    System.out.printf("PROBE\t%s\t%.3f\t%s%n", Path.of(args[index]).getFileName(),
                        (System.nanoTime() - start) / 1e9, result.replace('\t', ' ').replace('\n', ' '));
                }
            } finally { engine.nativeRelease(handle); }
            return;
        }
        expect(engine.nativeTranscribe(handle, new float[16_000], engine.nativeGeneration(handle), 4).length == 0,
                "Digital silence produced speech");
        System.out.println("PASS: silence produces no speech");
        float[] malformed = new float[16_000];
        malformed[10] = Float.NaN;
        try {
            engine.nativeTranscribe(handle, malformed, engine.nativeGeneration(handle), 4);
            throw new AssertionError("Nonfinite audio accepted");
        } catch (IllegalArgumentException expected) { }
        System.out.println("PASS: nonfinite audio rejected");
        long obsoleteGeneration = engine.nativeGeneration(handle);
        engine.nativeCancel(handle);
        try {
            engine.nativeTranscribe(handle, new float[16_000], obsoleteGeneration, 4);
            throw new AssertionError("Cancelled queued generation accepted");
        } catch (CancellationException expected) { }
        System.out.println("PASS: cancelled queued request rejected");
        byte[] raw = Files.readAllBytes(Path.of(args[2]));
        float[] samples = new float[raw.length / Float.BYTES];
        ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(samples);
        Arrays.fill(raw, (byte) 0);
        long began = System.nanoTime();
        String text = new String(engine.nativeTranscribe(handle, samples, engine.nativeGeneration(handle), 4), StandardCharsets.UTF_8);
        expect(!text.isBlank(), "Known Quran audio did not produce a transcript");
        System.out.println("PASS: next request after cancellation succeeds");
        System.out.println("TRANSCRIPT: " + text.trim());
        System.out.printf("HOST_INFERENCE_SECONDS: %.3f%n", (System.nanoTime() - began) / 1e9);
        float[] longAudio = new float[90 * 16_000];
        for (int i = 0; i < longAudio.length; i++) longAudio[i] = samples[i % samples.length];
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Throwable> unexpected = new AtomicReference<>();
        long runningGeneration = engine.nativeGeneration(handle);
        Thread running = new Thread(() -> {
            started.countDown();
            try {
                engine.nativeTranscribe(handle, longAudio, runningGeneration, 4);
                unexpected.set(new AssertionError("Cancelled running inference returned a result"));
            } catch (CancellationException expected) {
                // Required result: no stale transcript may reach the screen.
            } catch (Throwable error) { unexpected.set(error); }
        });
        running.start();
        expect(started.await(2, TimeUnit.SECONDS), "Worker failed to start");
        Thread.sleep(150);
        engine.nativeCancel(handle);
        running.join(15_000);
        expect(!running.isAlive(), "Native abort did not stop inference within 15 seconds");
        if (unexpected.get() != null) throw new AssertionError("Native cancellation failure", unexpected.get());
        System.out.println("PASS: running inference is aborted without a stale result");
        String afterAbort = new String(engine.nativeTranscribe(handle, samples,
                engine.nativeGeneration(handle), 4), StandardCharsets.UTF_8);
        expect(!afterAbort.isBlank(), "Loaded context was not reusable after running cancellation");
        System.out.println("PASS: loaded context is reusable after running cancellation");
        // Closing twice or cancelling a closed engine must be safe.
        engine.nativeRelease(handle);
        engine.nativeRelease(handle);
        engine.nativeCancel(handle);
        try {
            engine.nativeTranscribe(handle, samples, 0, 4);
            throw new AssertionError("Closed context accepted inference");
        } catch (IllegalStateException expected) { }
        System.out.println("PASS: close is idempotent and closed context rejected");
        Arrays.fill(samples, 0f);
        Arrays.fill(longAudio, 0f);
        System.out.println("NATIVE_JNI_CHECKS_PASSED: 7");
    }
}
