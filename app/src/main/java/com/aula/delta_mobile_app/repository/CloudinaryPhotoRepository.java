package com.aula.delta_mobile_app.repository;

import com.aula.delta_mobile_app.model.CloudinaryPhoto;
import com.aula.delta_mobile_app.model.ScreenState;
import com.aula.delta_mobile_app.services.api.CloudinaryApiService;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public final class CloudinaryPhotoRepository {
    private final CloudinaryApiService api;

    public CloudinaryPhotoRepository(CloudinaryApiService api) { this.api = api; }

    public ScreenState<String> upload(File file) throws IOException {
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", "profile.jpg", RequestBody.create(MediaType.get("image/jpeg"), file));
        Response<CloudinaryPhoto> response = api.upload(part, RequestBody.create(MediaType.get("text/plain"), "delta_profile_photos")).execute();
        if (!response.isSuccessful()) {
            if (response.errorBody() != null) response.errorBody().close();
            return ScreenState.error("Não foi possível enviar sua foto. Tente novamente mais tarde.", response.code(), 0);
        }
        CloudinaryPhoto photo = response.body();
        if (photo == null || photo.publicId == null || photo.publicId.trim().isEmpty() || !validUrl(photo.secureUrl))
            return ScreenState.error("O serviço não confirmou sua foto. Tente novamente.");
        return ScreenState.success(photo.secureUrl);
    }

    public static boolean validUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && "res.cloudinary.com".equals(uri.getHost()) && uri.getUserInfo() == null && uri.getPort() == -1
                && uri.getPath().startsWith("/ggra32fp/image/upload/") && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException invalid) { return false; }
    }
}
