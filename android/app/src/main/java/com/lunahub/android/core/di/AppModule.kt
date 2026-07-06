package com.lunahub.android.core.di

import com.lunahub.android.data.remote.CameraHttpService
import com.lunahub.android.data.remote.LunaIndexParser
import com.lunahub.android.data.repository.DefaultLunaRepository
import com.lunahub.android.domain.repository.LunaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindLunaRepository(repository: DefaultLunaRepository): LunaRepository

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(12, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
            return Retrofit.Builder()
                .baseUrl("http://192.168.42.1/")
                .client(okHttpClient)
                .build()
        }

        @Provides
        @Singleton
        fun provideCameraHttpService(retrofit: Retrofit): CameraHttpService {
            return retrofit.create(CameraHttpService::class.java)
        }

        @Provides
        @Singleton
        fun provideLunaIndexParser(): LunaIndexParser = LunaIndexParser()
    }
}
