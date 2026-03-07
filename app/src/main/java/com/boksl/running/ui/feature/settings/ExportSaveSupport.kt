package com.boksl.running.ui.feature.settings

import android.content.Context
import android.net.Uri
import java.io.File

fun saveExportFileToUri(
    context: Context,
    sourceFilePath: String,
    targetUri: Uri,
) {
    val sourceFile = File(sourceFilePath)
    check(sourceFile.exists()) { "저장할 내보내기 파일을 찾을 수 없습니다." }

    context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
        sourceFile.inputStream().use { inputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: error("디바이스 저장 위치를 열 수 없습니다.")
}
