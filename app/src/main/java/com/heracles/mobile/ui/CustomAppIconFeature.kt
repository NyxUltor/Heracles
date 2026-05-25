package com.heracles.mobile.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon as AndroidIcon
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

private const val ICON_PREFS = "custom_app_icon_prefs"
private const val ICON_PREF_PATH = "icon_path"
private const val ICON_PREF_SOURCE = "icon_source"
private const val ICON_DIR = "custom-icons"
private const val ICON_FILE_NAME = "launcher-icon.png"
private const val MAX_ICON_BYTES = 2L * 1024L * 1024L

@Composable
fun CustomAppIconCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { CustomAppIconStore(context) }

    var sourceText by rememberSaveable { mutableStateOf("") }
    var statusText by rememberSaveable { mutableStateOf("Select an image to stage a disposable app icon copy.") }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var working by rememberSaveable { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            working = true
            val outcome = withContext(Dispatchers.IO) {
                store.importFromUri(context, uri, persistPermission = true)
            }
            previewBitmap = outcome.previewBitmap
            sourceText = outcome.sourceLabel
            statusText = outcome.message
            working = false
        }
    }

    LaunchedEffect(Unit) {
        val existing = withContext(Dispatchers.IO) { store.loadPersistedState(context) }
        sourceText = existing.sourceLabel
        statusText = existing.message
        previewBitmap = existing.previewBitmap
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("App icon (Development)", style = MaterialTheme.typography.titleLarge)
            Text(
                "Development feature: stage a disposable icon for preview or pinned shortcuts. Packaged launcher icons are managed at build-time and may not update due to launcher caching.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = sourceText,
                onValueChange = { sourceText = it },
                label = { Text("Image URI") },
                supportingText = { Text("Use a content:// or file:// URI. The image will be copied into app-private storage.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val parsedUri = parseCandidateUri(sourceText)
                        if (parsedUri == null) {
                            statusText = "Enter a valid URI first."
                            return@Button
                        }
                        // If the URI is a content:// URI we cannot reliably take a persistable
                        // permission unless the URI was returned by the system picker. Encourage
                        // the user to use the picker which will prompt for permission.
                        if (parsedUri.scheme.equals("content", ignoreCase = true)) {
                            statusText = "This URI requires permission — use 'Pick image' so the system can grant access."
                            return@Button
                        }

                        scope.launch {
                            working = true
                            val outcome = withContext(Dispatchers.IO) {
                                store.importFromUri(context, parsedUri, persistPermission = false)
                            }
                            previewBitmap = outcome.previewBitmap
                            sourceText = outcome.sourceLabel
                            statusText = outcome.message
                            working = false
                        }
                    },
                    enabled = !working
                ) {
                    Text("Import URI")
                }

                Button(
                    onClick = { pickerLauncher.launch(arrayOf("image/*")) },
                    enabled = !working
                ) {
                    Text("Pick image")
                }

                Button(
                    onClick = {
                        scope.launch {
                            working = true
                            withContext(Dispatchers.IO) { store.clearPersistedIcon(context) }
                            previewBitmap = null
                            sourceText = ""
                            statusText = "Reset to the default launcher icon asset."
                            working = false
                        }
                    },
                    enabled = !working
                ) {
                    Text("Reset")
                }

                Button(
                    onClick = {
                        scope.launch {
                            working = true
                            val ok = withContext(Dispatchers.IO) { store.createPinnedShortcut(context) }
                            statusText = if (ok) "Pin request sent to launcher (accept prompt on device)." else "Pinned shortcuts not supported on this device."
                            working = false
                        }
                    },
                    enabled = !working
                ) {
                    Text("Pin to home")
                }
            }

            Text(statusText, style = MaterialTheme.typography.bodyMedium)

            previewBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Custom app icon preview",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private class CustomAppIconStore(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(ICON_PREFS, Context.MODE_PRIVATE)
    private val iconDirectory = File(context.filesDir, ICON_DIR)
    private val iconFile = File(iconDirectory, ICON_FILE_NAME)

    data class ImportOutcome(
        val previewBitmap: Bitmap?,
        val sourceLabel: String,
        val message: String,
    )

    fun loadPersistedState(context: Context): ImportOutcome {
        val persistedPath = sharedPreferences.getString(ICON_PREF_PATH, null)
        val persistedSource = sharedPreferences.getString(ICON_PREF_SOURCE, null).orEmpty()
        val file = persistedPath?.let(::File)

        if (file == null || !file.exists()) {
            return ImportOutcome(
                previewBitmap = null,
                sourceLabel = persistedSource,
                message = "No custom icon is staged yet."
            )
        }

        return ImportOutcome(
            previewBitmap = decodeSampledBitmap(file),
            sourceLabel = persistedSource.ifBlank { file.absolutePath },
            message = "Loaded staged icon from private storage."
        )
    }

    fun importFromUri(context: Context, uri: Uri, persistPermission: Boolean): ImportOutcome {
        validateIconUri(context, uri)
        if (persistPermission && uri.scheme.equals("content", ignoreCase = true)) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        iconDirectory.mkdirs()
        copyUriToPrivateFile(context, uri, iconFile)
        val preview = decodeSampledBitmap(iconFile)
        val sourceLabel = uri.toString()
        sharedPreferences.edit()
            .putString(ICON_PREF_PATH, iconFile.absolutePath)
            .putString(ICON_PREF_SOURCE, sourceLabel)
            .apply()

        return ImportOutcome(
            previewBitmap = preview,
            sourceLabel = sourceLabel,
            message = "Validated and copied into app-private storage."
        )
    }

    fun clearPersistedIcon(context: Context) {
        sharedPreferences.edit()
            .remove(ICON_PREF_PATH)
            .remove(ICON_PREF_SOURCE)
            .apply()
        if (iconFile.exists()) {
            iconFile.delete()
        }
    }

    private fun validateIconUri(context: Context, uri: Uri) {
        val scheme = uri.scheme?.lowercase(Locale.US)
        require(scheme == "content" || scheme == "file") {
            "Only content:// and file:// URIs are allowed for icon import."
        }

        val mimeType = resolveMimeType(context, uri)
        require(mimeType != null && mimeType.startsWith("image/")) {
            "Selected URI must point to an image."
        }

        val sizeBytes = resolveSizeBytes(context, uri)
        require(sizeBytes == null || sizeBytes <= MAX_ICON_BYTES) {
            "Selected image is too large. Keep it under 2 MB."
        }
    }

    private fun copyUriToPrivateFile(context: Context, uri: Uri, destination: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var totalBytes = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    totalBytes += read
                    require(totalBytes <= MAX_ICON_BYTES) {
                        "Selected image is too large. Keep it under 2 MB."
                    }
                    output.write(buffer, 0, read)
                }
            }
        } ?: throw IOException("Unable to open the selected image URI.")
    }

    private fun decodeSampledBitmap(file: File): Bitmap? {
        if (!file.exists()) {
            return null
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 256, 256)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }

    private fun calculateInSampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (srcHeight > reqHeight || srcWidth > reqWidth) {
            var halfHeight = srcHeight / 2
            var halfWidth = srcWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun resolveMimeType(context: Context, uri: Uri): String? {
        uri.scheme?.lowercase(Locale.US)?.let { scheme ->
            when (scheme) {
                "content" -> return context.contentResolver.getType(uri)
                "file" -> {
                    val extension = File(uri.path.orEmpty()).extension.lowercase(Locale.US)
                    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                }
            }
        }
        return null
    }

    private fun resolveSizeBytes(context: Context, uri: Uri): Long? {
        uri.scheme?.lowercase(Locale.US)?.let { scheme ->
            when (scheme) {
                "content" -> {
                    context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.SIZE),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                            return cursor.getLong(sizeIndex)
                        }
                    }
                }

                "file" -> {
                    val file = File(uri.path.orEmpty())
                    if (file.exists()) {
                        return file.length()
                    }
                }
            }
        }
        return null
    }

    fun createPinnedShortcut(context: Context): Boolean {
        if (!iconFile.exists()) return false
        val bitmap = decodeSampledBitmap(iconFile) ?: return false
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return false

        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(context.packageName, "${context.packageName}.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val info = ShortcutInfo.Builder(context, "heracles_custom_icon_shortcut")
            .setShortLabel("Heracles (custom)")
            .setLongLabel("Heracles (custom icon)")
            .setIcon(AndroidIcon.createWithBitmap(bitmap))
            .setIntent(intent)
            .build()

        return if (shortcutManager.isRequestPinShortcutSupported) {
            shortcutManager.requestPinShortcut(info, null)
            true
        } else {
            false
        }
    }
}

private fun parseCandidateUri(raw: String): Uri? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) {
        return null
    }
    return runCatching { Uri.parse(trimmed) }.getOrNull()?.takeIf { !it.scheme.isNullOrBlank() }
}
