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
    public static final String PREFERENCES = "delta_profile_photos";
    private final Context context;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final CloudinaryPhotoRepository cloudinary = new CloudinaryPhotoRepository(com.aula.delta_mobile_app.services.network.RetrofitClient.getCloudinaryService());
    private final okhttp3.OkHttpClient images = com.aula.delta_mobile_app.services.network.RetrofitClient.createPublicImageClient();

    public ProfilePhotoRepository(Context context) { this.context = context.getApplicationContext(); }

    public void upload(Uri uri, int userId, java.util.function.BooleanSupplier active, Consumer<ScreenState<File>> callback) {
        prepare(uri, value -> {
            if (value.status != ScreenState.Status.SUCCESS) { callback.accept(value); return; }
            boolean retained = false;
            try {
                if (!active.getAsBoolean()) {
                    callback.accept(ScreenState.error("O envio da foto foi cancelado. Tente novamente."));
                    return;
                }
                ScreenState<String> uploaded = cloudinary.upload(value.data);
                if (uploaded.status != ScreenState.Status.SUCCESS) { callback.accept(ScreenState.error(uploaded.message, uploaded.code, uploaded.retryAt)); return; }
                if (!active.getAsBoolean()) {
                    callback.accept(ScreenState.error("O envio da foto foi cancelado. Tente novamente."));
                    return;
                }
                if (!context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString("user:" + userId, uploaded.data).commit())
                    throw new java.io.IOException();
                callback.accept(ScreenState.success(value.data));
                retained = true;
            } catch (Exception failure) {
                callback.accept(ScreenState.error("Não foi possível enviar e confirmar sua foto. Tente novamente."));
            } finally {
                if (!retained) value.data.delete();
            }
        });
    }

    public void load(int userId, java.util.function.BooleanSupplier active, Consumer<ScreenState<File>> callback) {
        worker.execute(() -> {
            File file = null;
            try {
                if (!active.getAsBoolean()) return;
                String url = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString("user:" + userId, null);
                if (url == null) { callback.accept(ScreenState.idle()); return; }
                file = download(url);
                if (!active.getAsBoolean()) { file.delete(); return; }
                callback.accept(ScreenState.success(file));
                file = null;
            } catch (Exception failure) {
                callback.accept(ScreenState.error("Não foi possível carregar sua foto. Tente novamente mais tarde."));
            } finally { if (file != null) file.delete(); }
        });
    }

    private File download(String url) throws java.io.IOException {
        if (!CloudinaryPhotoRepository.validUrl(url)) throw new java.io.IOException();
        File directory = new File(context.getCacheDir(), "profile");
        if (!directory.exists() && !directory.mkdirs()) throw new java.io.IOException();
        File file = File.createTempFile("remote-", ".jpg", directory);
        try {
            try (okhttp3.Response response = images.newCall(new okhttp3.Request.Builder().url(url).cacheControl(new okhttp3.CacheControl.Builder().noCache().noStore().build()).build()).execute()) {
                if (!response.isSuccessful() || response.body() == null) throw new java.io.IOException();
                try (InputStream input = response.body().byteStream(); FileOutputStream output = new FileOutputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int read, total = 0;
                    while ((read = input.read(buffer)) != -1) {
                        total += read;
                        if (total > 10 * 1024 * 1024) throw new java.io.IOException();
                        output.write(buffer, 0, read);
                    }
                }
            }
            Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(file), (decoder, info, source) -> {
                int width = info.getSize().getWidth(), height = info.getSize().getHeight();
                float scale = Math.min(1f, 1024f / Math.max(width, height));
                decoder.setTargetSize(Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
            try (FileOutputStream output = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) throw new java.io.IOException();
            } finally { bitmap.recycle(); }
            return file;
        } catch (Exception failure) {
            file.delete();
            throw new java.io.IOException(failure);
        }
    }

    private void prepare(Uri uri, Consumer<ScreenState<File>> callback) {
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
                prepared = File.createTempFile("upload-", ".jpg", directory);
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
