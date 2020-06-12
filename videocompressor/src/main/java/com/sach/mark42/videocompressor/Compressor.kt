package com.sach.mark42.videocompressor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Compressor {

    fun compress(sourcePath: String, destinationPath: String, listener: CompressorListener) {
        GlobalScope.launch(Dispatchers.Main) {
            val result = startCompression(sourcePath, destinationPath)

            if (result) {
                listener.onSuccess()
            } else {
                listener.onFailure()
            }
        }
    }

    private suspend fun startCompression(sourcePath: String, destinationPath: String) : Boolean = withContext(Dispatchers.IO) {
        //compressVideo(sourcePath, destinationPath)
    }
}