package com.qomg.webappcatalogs.webapp

import android.app.ActivityManager
import android.content.Context
import androidx.core.content.getSystemService
import com.qomg.webappcatalogs.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WebAppRepository(context: Context) {
    private val context = context.applicationContext
    private val dao = WebAppDatabase.getInstance(context).webAppDao()

    fun observeAll(): Flow<List<WebApp>> = dao.observeAll()

    suspend fun get(id: Long): WebApp? = dao.get(id)

    suspend fun markOpened(id: Long) = dao.markOpened(id)

    /** Adds the app unless one with the same URL exists. Returns the app's id. */
    suspend fun add(name: String, url: String): Long =
        dao.findByUrl(url)?.id ?: dao.insert(WebApp(name = name, url = url))

    /** Removes the app with its open task, shortcut, favicon and (when possible) its browsing data. Call on the main thread. */
    suspend fun delete(app: WebApp) {
        context.getSystemService<ActivityManager>()?.appTasks
            ?.filter { it.taskInfo.baseIntent.data == WebAppShortcuts.uri(app.id) }
            ?.forEach { it.finishAndRemoveTask() }
        dao.delete(app.id)
        withContext(Dispatchers.IO) { WebAppIcons.delete(context, app.id) }
        WebAppShortcuts.disable(context, app.id, context.getString(R.string.webapp_deleted))
        WebAppProfiles.delete(app.id)
    }
}
