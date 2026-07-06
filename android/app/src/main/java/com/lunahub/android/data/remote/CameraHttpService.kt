package com.lunahub.android.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface CameraHttpService {
    @GET
    suspend fun getDirectory(@Url url: String): Response<ResponseBody>
}
