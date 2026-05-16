package com.aiapkbuilder.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.aiapkbuilder.app.BuildConfig
import com.aiapkbuilder.app.data.api.CodemagicApiService
import com.aiapkbuilder.app.data.api.GitHubActionsApiService
import com.aiapkbuilder.app.data.api.GitHubApiService
import com.aiapkbuilder.app.data.api.OpenAIApiService
import com.aiapkbuilder.app.data.local.AppDatabase
import com.aiapkbuilder.app.data.local.ArtifactDao
import com.aiapkbuilder.app.data.local.BackupRecordDao
import com.aiapkbuilder.app.data.local.BuildConfigDao
import com.aiapkbuilder.app.data.local.BuildHistoryDao
import com.aiapkbuilder.app.data.local.BuildJobDao
import com.aiapkbuilder.app.data.local.CodeCacheDao
import com.aiapkbuilder.app.data.local.DownloadSessionDao
import com.aiapkbuilder.app.data.local.ProjectDao
import com.aiapkbuilder.app.data.local.ShareLinkDao
import com.aiapkbuilder.app.data.local.TemplateDao
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.data.repository.SettingsRepository
import com.aiapkbuilder.app.data.service.AICodeGenerationService
import com.aiapkbuilder.app.data.service.AIProviderManager
import com.aiapkbuilder.app.data.service.ProjectPlannerService
import com.aiapkbuilder.app.data.service.build.ArtifactCache
import com.aiapkbuilder.app.data.service.build.ArtifactManager
import com.aiapkbuilder.app.data.service.build.BuildExecutor
import com.aiapkbuilder.app.data.service.build.BuildProviderFactory
import com.aiapkbuilder.app.data.service.build.LogAggregator
import com.aiapkbuilder.app.data.service.build.docker.DockerService
import com.aiapkbuilder.app.data.service.build.provider.CodemagicProvider
import com.aiapkbuilder.app.data.service.build.provider.DockerProvider
import com.aiapkbuilder.app.data.service.build.provider.GitHubActionsProvider
import com.aiapkbuilder.app.data.service.export.BuildHistoryManager
import com.aiapkbuilder.app.data.service.export.DownloadManager
import com.aiapkbuilder.app.data.service.export.ProjectExporter
import com.aiapkbuilder.app.data.service.export.ShareService
import com.aiapkbuilder.app.data.service.export.StorageManager
import com.aiapkbuilder.app.util.CodeGenerator
import com.aiapkbuilder.app.util.PromptAnalyzer
import com.aiapkbuilder.app.util.ResponseParser
import com.aiapkbuilder.app.util.StreamingResponseHandler
import com.aiapkbuilder.app.util.TemplateProcessor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_apk_builder_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("openai")
    fun provideOpenAIRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.OPENAI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("github")
    fun provideGitHubRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.GITHUB_API_BASE)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("codemagic")
    fun provideCodemagicRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.CODEMAGIC_API_BASE)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideOpenAIService(@Named("openai") retrofit: Retrofit): OpenAIApiService =
        retrofit.create(OpenAIApiService::class.java)

    @Provides
    @Singleton
    fun provideGitHubService(@Named("github") retrofit: Retrofit): GitHubApiService =
        retrofit.create(GitHubApiService::class.java)

    @Provides
    @Singleton
    fun provideCodemagicService(@Named("codemagic") retrofit: Retrofit): CodemagicApiService =
        retrofit.create(CodemagicApiService::class.java)

    @Provides
    @Singleton
    fun provideGitHubActionsService(@Named("github") retrofit: Retrofit): GitHubActionsApiService =
        retrofit.create(GitHubActionsApiService::class.java)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideProjectDao(db: AppDatabase): ProjectDao = db.projectDao()

    @Provides
    @Singleton
    fun provideBuildJobDao(db: AppDatabase): BuildJobDao = db.buildJobDao()

    @Provides
    @Singleton
    fun provideTemplateDao(db: AppDatabase): TemplateDao = db.templateDao()

    @Provides
    @Singleton
    fun provideBuildConfigDao(db: AppDatabase): BuildConfigDao = db.buildConfigDao()

    @Provides
    @Singleton
    fun provideCodeCacheDao(db: AppDatabase): CodeCacheDao = db.codeCacheDao()

    @Provides
    @Singleton
    fun provideArtifactDao(db: AppDatabase): ArtifactDao = db.artifactDao()

    @Provides
    @Singleton
    fun provideDownloadSessionDao(db: AppDatabase): DownloadSessionDao = db.downloadSessionDao()

    @Provides
    @Singleton
    fun provideShareLinkDao(db: AppDatabase): ShareLinkDao = db.shareLinkDao()

    @Provides
    @Singleton
    fun provideBackupRecordDao(db: AppDatabase): BackupRecordDao = db.backupRecordDao()

    @Provides
    @Singleton
    fun provideBuildHistoryDao(db: AppDatabase): BuildHistoryDao = db.buildHistoryDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideAIProviderManager(
        openAIService: OpenAIApiService,
        settingsRepository: SettingsRepository
    ): AIProviderManager = AIProviderManager(openAIService, settingsRepository)

    @Provides
    @Singleton
    fun provideCodeGenerator(gson: Gson): CodeGenerator = CodeGenerator(gson)

    @Provides
    @Singleton
    fun providePromptAnalyzer(): PromptAnalyzer = PromptAnalyzer()

    @Provides
    @Singleton
    fun provideProjectPlanner(): ProjectPlannerService = ProjectPlannerService()

    @Provides
    @Singleton
    fun provideAICodeGenerationService(
        aiProviderManager: AIProviderManager,
        projectPlanner: ProjectPlannerService,
        codeGenerator: CodeGenerator,
        promptAnalyzer: PromptAnalyzer
    ): AICodeGenerationService = AICodeGenerationService(
        aiProviderManager, projectPlanner, codeGenerator, promptAnalyzer
    )

    @Provides
    @Singleton
    fun provideStreamingResponseHandler(): StreamingResponseHandler = StreamingResponseHandler()

    @Provides
    @Singleton
    fun provideResponseParser(gson: Gson): ResponseParser = ResponseParser(gson)

    @Provides
    @Singleton
    fun provideTemplateProcessor(): TemplateProcessor = TemplateProcessor()

    // Build System Services
    @Provides
    @Singleton
    fun provideLogAggregator(
        projectRepository: ProjectRepository
    ): LogAggregator = LogAggregator(projectRepository = projectRepository)

    @Provides
    @Singleton
    fun provideArtifactCache(): ArtifactCache = ArtifactCache()

    @Provides
    @Singleton
    fun provideArtifactManager(
        projectRepository: ProjectRepository,
        artifactCache: ArtifactCache
    ): ArtifactManager = ArtifactManager(projectRepository, artifactCache)

    @Provides
    @Singleton
    fun provideDockerService(): DockerService = DockerService()

    @Provides
    @Singleton
    fun provideGitHubActionsProvider(
        apiService: GitHubActionsApiService
    ): GitHubActionsProvider = GitHubActionsProvider(apiService)

    @Provides
    @Singleton
    fun provideCodemagicProvider(
        apiService: CodemagicApiService
    ): CodemagicProvider = CodemagicProvider(apiService)

    @Provides
    @Singleton
    fun provideDockerProvider(
        dockerService: DockerService
    ): DockerProvider = DockerProvider(dockerService)

    @Provides
    @Singleton
    fun provideBuildProviderFactory(
        githubActionsProvider: GitHubActionsProvider,
        codemagicProvider: CodemagicProvider,
        dockerProvider: DockerProvider
    ): BuildProviderFactory = BuildProviderFactory(
        githubActionsProvider, codemagicProvider, dockerProvider
    )

    @Provides
    @Singleton
    fun provideBuildExecutor(
        providerFactory: BuildProviderFactory,
        projectRepository: ProjectRepository,
        logAggregator: LogAggregator,
        artifactManager: ArtifactManager
    ): BuildExecutor = BuildExecutor(
        providerFactory, projectRepository, logAggregator, artifactManager
    )

    // Phase 5: Export & Management Services
    @Provides
    @Singleton
    fun provideDownloadManager(
        downloadSessionDao: DownloadSessionDao
    ): DownloadManager = DownloadManager(downloadSessionDao)

    @Provides
    @Singleton
    fun provideProjectExporter(
        projectRepository: ProjectRepository,
        @ApplicationContext context: Context
    ): ProjectExporter = ProjectExporter(projectRepository, context)

    @Provides
    @Singleton
    fun provideShareService(
        shareLinkDao: ShareLinkDao,
        @ApplicationContext context: Context
    ): ShareService = ShareService(shareLinkDao, context)

    @Provides
    @Singleton
    fun provideBuildHistoryManager(
        buildHistoryDao: BuildHistoryDao
    ): BuildHistoryManager = BuildHistoryManager(buildHistoryDao)

    @Provides
    @Singleton
    fun provideStorageManager(
        backupRecordDao: BackupRecordDao,
        buildHistoryDao: BuildHistoryDao,
        artifactCache: ArtifactCache,
        @ApplicationContext context: Context
    ): StorageManager = StorageManager(backupRecordDao, buildHistoryDao, artifactCache, context)
}