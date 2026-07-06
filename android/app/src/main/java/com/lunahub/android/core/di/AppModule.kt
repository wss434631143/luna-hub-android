package com.lunahub.android.core.di

import android.content.Context
import androidx.room.Room
import com.lunahub.android.core.database.DownloadTaskDao
import com.lunahub.android.core.database.LunaDatabase
import com.lunahub.android.data.remote.CameraHttpService
import com.lunahub.android.data.repository.DefaultDownloadRepository
import com.lunahub.android.data.remote.LunaIndexParser
import com.lunahub.android.data.repository.DefaultLunaRepository
import com.lunahub.android.domain.repository.DownloadRepository
import com.lunahub.android.domain.repository.LunaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(repository: DefaultDownloadRepository): DownloadRepository

    companion object {
        @Provides
        @Singleton
        fun provideLunaDatabase(@ApplicationContext context: Context): LunaDatabase {
            return Room.databaseBuilder(context, LunaDatabase::class.java, "luna-hub.db")
                .fallbackToDestructiveMigration(false)
                .build()
        }

        @Provides
        fun provideDownloadTaskDao(database: LunaDatabase): DownloadTaskDao = database.downloadTaskDao()

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
