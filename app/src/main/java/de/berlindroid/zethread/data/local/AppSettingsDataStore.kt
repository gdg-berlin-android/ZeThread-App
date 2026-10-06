package de.berlindroid.zethread.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zethread_settings")

data class AppConfig(
    val repoOwner: String = "gdg-berlin-android",
    val repoName: String = "ZeThread-Contributions",
    val branch: String = "main",
    val pathPrefix: String = "patches",
    val githubToken: String = "",
    val useMockRepository: Boolean = false
)

class AppSettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_REPO_OWNER = stringPreferencesKey("repo_owner")
        private val KEY_REPO_NAME = stringPreferencesKey("repo_name")
        private val KEY_BRANCH = stringPreferencesKey("branch")
        private val KEY_PATH_PREFIX = stringPreferencesKey("path_prefix")
        private val KEY_GITHUB_TOKEN = stringPreferencesKey("github_token")
        private val KEY_USE_MOCK_REPO = booleanPreferencesKey("use_mock_repo")

        val DEFAULT_CONFIG = AppConfig()
    }

    val configFlow: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        AppConfig(
            repoOwner = prefs[KEY_REPO_OWNER] ?: DEFAULT_CONFIG.repoOwner,
            repoName = prefs[KEY_REPO_NAME] ?: DEFAULT_CONFIG.repoName,
            branch = prefs[KEY_BRANCH] ?: DEFAULT_CONFIG.branch,
            pathPrefix = prefs[KEY_PATH_PREFIX] ?: DEFAULT_CONFIG.pathPrefix,
            githubToken = prefs[KEY_GITHUB_TOKEN] ?: "",
            useMockRepository = prefs[KEY_USE_MOCK_REPO] ?: DEFAULT_CONFIG.useMockRepository
        )
    }

    suspend fun saveConfig(config: AppConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REPO_OWNER] = config.repoOwner.trim()
            prefs[KEY_REPO_NAME] = config.repoName.trim()
            prefs[KEY_BRANCH] = config.branch.trim()
            prefs[KEY_PATH_PREFIX] = config.pathPrefix.trim().removePrefix("/").removeSuffix("/")
            prefs[KEY_GITHUB_TOKEN] = config.githubToken.trim()
            prefs[KEY_USE_MOCK_REPO] = config.useMockRepository
        }
    }
}
