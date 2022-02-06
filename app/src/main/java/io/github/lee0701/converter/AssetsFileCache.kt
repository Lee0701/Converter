package io.github.lee0701.converter

import android.content.res.AssetManager
import java.io.File
import java.io.FileOutputStream

class AssetsFileCache(
    private val assets: AssetManager,
    private val cacheDir: File,
) {
    fun cached(fileName: String): File {
        val cachedFile = File(cacheDir, fileName)
        if(!cachedFile.exists()) {
            cachedFile.parentFile?.mkdirs()
            assets.open(fileName).copyTo(FileOutputStream(cachedFile))
        }
        return cachedFile
    }
}