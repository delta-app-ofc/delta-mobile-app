package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.CloudinaryPhoto;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.CloudinaryApiService;
import java.io.File;
import java.lang.reflect.Proxy;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;
import static org.junit.Assert.*;

public class CloudinaryPhotoRepositoryTest {
    private CloudinaryPhotoRepository repository(Response<CloudinaryPhoto> response) {
        CloudinaryApiService api = (file, preset) -> {
            assertEquals("form-data; name=\"file\"; filename=\"profile.jpg\"", file.headers().get("Content-Disposition"));
            Buffer buffer = new Buffer();
            try { preset.writeTo(buffer); } catch (java.io.IOException failure) { throw new AssertionError(failure); }
            assertEquals("delta_profile_photos", buffer.readUtf8());
            return (Call<CloudinaryPhoto>) Proxy.newProxyInstance(Call.class.getClassLoader(), new Class<?>[] { Call.class }, (proxy, method, args) -> {
                if (method.getName().equals("execute")) return response;
                throw new AssertionError(method.getName());
            });
        };
        return new CloudinaryPhotoRepository(api);
    }

    @Test
    public void confirmsUploadOnlyWithCloudinaryUrlAndPublicId() throws Exception {
        CloudinaryPhoto photo = new CloudinaryPhoto();
        photo.publicId = "profile/example";
        photo.secureUrl = "https://res.cloudinary.com/ggra32fp/image/upload/v1/profile/example.jpg";
        File file = File.createTempFile("photo-", ".jpg");
        try {
            ScreenState<String> result = repository(Response.success(photo)).upload(file);
            assertEquals(ScreenState.Status.SUCCESS, result.status);
            assertEquals(photo.secureUrl, result.data);
            photo.publicId = null;
            assertEquals(ScreenState.Status.ERROR, repository(Response.success(photo)).upload(file).status);
        } finally { file.delete(); }
    }

    @Test
    public void rejectsPermissionFailureAndUnconfirmedResponse() throws Exception {
        File file = File.createTempFile("photo-", ".jpg");
        try {
            ScreenState<String> result = repository(Response.error(403, ResponseBody.create(MediaType.get("application/json"), "{}"))).upload(file);
            assertEquals(ScreenState.Status.ERROR, result.status);
            assertEquals(403, result.code);
            assertNull(result.data);
            assertEquals(ScreenState.Status.ERROR, repository(Response.success(null)).upload(file).status);
        } finally { file.delete(); }
    }

    @Test
    public void acceptsOnlyHttpsImagesFromConfiguredCloud() {
        assertTrue(CloudinaryPhotoRepository.validUrl("https://res.cloudinary.com/ggra32fp/image/upload/photo.jpg"));
        assertFalse(CloudinaryPhotoRepository.validUrl("http://res.cloudinary.com/ggra32fp/image/upload/photo.jpg"));
        assertFalse(CloudinaryPhotoRepository.validUrl("https://res.cloudinary.com/other/image/upload/photo.jpg"));
        assertFalse(CloudinaryPhotoRepository.validUrl("https://res.cloudinary.com.evil.example/ggra32fp/image/upload/photo.jpg"));
        assertFalse(CloudinaryPhotoRepository.validUrl("https://user@res.cloudinary.com/ggra32fp/image/upload/photo.jpg"));
        assertFalse(CloudinaryPhotoRepository.validUrl(null));
    }
}
