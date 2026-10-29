package com.aula.delta_mobile_app.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import com.aula.delta_mobile_app.model.ScreenState;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class ProfilePhotoRepository {
    private final Context context;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    public ProfilePhotoRepository(Context context) { this.context = context.getApplicationContext(); }

    public void prepare(Uri uri, Consumer<ScreenState<File>> callback) {
        worker.execute(() -> {
            File source = null, prepared = null;
            Bitmap bitmap = null;
            try {
                File directory = new File(context.getCacheDir(), "profile");
                if (!directory.exists() && !directory.mkdirs()) throw new java.io.IOException();
                source = File.createTempFile("selected-", ".image", directory);
                try (InputStream input = context.getContentResolver().openInputStream(uri); FileOutputStream output = new FileOutputStream(source)) {
                    if (input == null) throw new java.io.IOException();
                    byte[] buffer = new byte[8192];
                    int read, total = 0;
                    while ((read = input.read(buffer)) != -1) {
                        total += read;
                        if (total > 10 * 1024 * 1024) throw new IllegalArgumentException("Escolha uma imagem de até 10 MB.");
                        output.write(buffer, 0, read);
                    }
                }
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(source), (decoder, info, imageSource) -> {
                    String mime = info.getMimeType();
                    if (!java.util.Arrays.asList("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif").contains(mime))
                        throw new IllegalArgumentException("Escolha uma foto JPEG, PNG, WebP ou HEIC compatível.");
                    int width = info.getSize().getWidth(), height = info.getSize().getHeight();
                    float scale = Math.min(1f, 1024f / Math.max(width, height));
                    decoder.setTargetSize(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
                prepared = File.createTempFile("preview-", ".jpg", directory);
                try (FileOutputStream output = new FileOutputStream(prepared)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) throw new java.io.IOException();
                }
                callback.accept(ScreenState.success(prepared));
            } catch (Exception failure) {
                if (prepared != null) prepared.delete();
                callback.accept(ScreenState.error(failure instanceof IllegalArgumentException ? failure.getMessage() : "Não foi possível preparar esta imagem. Escolha outra foto."));
            } finally {
                if (source != null) source.delete();
                if (bitmap != null) bitmap.recycle();
            }
        });
    }

    public void close() { worker.shutdown(); }
}
