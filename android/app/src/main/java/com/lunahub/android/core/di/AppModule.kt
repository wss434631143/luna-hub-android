package com.lunahub.android.core.di

import com.lunahub.android.data.repository.MockLunaRepository
import com.lunahub.android.domain.repository.LunaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindLunaRepository(repository: MockLunaRepository): LunaRepository
}
