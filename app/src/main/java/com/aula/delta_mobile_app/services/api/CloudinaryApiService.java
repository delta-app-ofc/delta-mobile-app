package com.aula.delta_mobile_app.services.api;

import com.aula.delta_mobile_app.model.CloudinaryPhoto;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface CloudinaryApiService {
    @Multipart
    @POST("ggra32fp/image/upload")
    Call<CloudinaryPhoto> upload(@Part MultipartBody.Part file, @Part("upload_preset") RequestBody preset);
}
