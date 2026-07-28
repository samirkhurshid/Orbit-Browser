package com.orbit.browser.di

import android.content.Context
import androidx.room.Room
import com.orbit.browser.data.db.OBDatabase
import com.orbit.browser.security.dns.SecureDnsResolver
import com.orbit.browser.security.vault.PasswordCipher
import com.orbit.browser.security.vault.PasswordVaultDatabase
import com.orbit.browser.security.vault.PasswordVaultRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OBDatabase =
        Room.databaseBuilder(context, OBDatabase::class.java, "orbit_browser.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideHistoryDao(db: OBDatabase) = db.historyDao()

    @Provides @Singleton
    fun provideBookmarkDao(db: OBDatabase) = db.bookmarkDao()

    @Provides @Singleton
    fun provideQuickAccessDao(db: OBDatabase) = db.quickAccessDao()

    @Provides @Singleton
    fun provideFrequentSiteDao(db: OBDatabase) = db.frequentSiteDao()

    @Provides @Singleton
    fun provideDownloadDao(db: OBDatabase) = db.downloadDao()

    @Provides @Singleton
    fun provideSearchSuggestionDao(db: OBDatabase) = db.searchSuggestionDao()

    @Provides @Singleton
    fun provideSecureDnsResolver(): SecureDnsResolver = SecureDnsResolver()

    @Provides @Singleton
    fun provideOkHttpClient(dnsResolver: SecureDnsResolver): OkHttpClient =
        OkHttpClient.Builder()
            .dns(dnsResolver)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    // ── Password vault — deliberately a separate Room DB from OBDatabase ──

    @Provides @Singleton
    fun providePasswordVaultDatabase(@ApplicationContext context: Context): PasswordVaultDatabase =
        Room.databaseBuilder(context, PasswordVaultDatabase::class.java, "orbit_password_vault.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideSavedLoginDao(db: PasswordVaultDatabase) = db.savedLoginDao()

    @Provides @Singleton
    fun providePasswordCipher(): PasswordCipher = PasswordCipher()

    @Provides @Singleton
    fun providePasswordVaultRepository(
        dao: com.orbit.browser.security.vault.SavedLoginDao,
        cipher: PasswordCipher,
    ): PasswordVaultRepository = PasswordVaultRepository(dao, cipher)
}
