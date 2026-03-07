package com.boksl.running.ui.feature.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.boksl.running.BuildConfig
import java.io.File

fun buildExportShareIntent(
    context: Context,
    filePath: String,
): Intent {
    val exportFile = File(filePath)
    check(exportFile.exists()) { "공유할 내보내기 파일을 찾을 수 없습니다." }

    val contentUri =
        FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            exportFile,
        )

    return Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, exportFile.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
        "파일 공유/저장",
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
