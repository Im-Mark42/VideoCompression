package com.sach.mark42.videocompressor

interface CompressionListener {
    fun onSuccess()
    fun onFailure()
    fun onProgress(percent: Float)
    fun onCancelled()
}

interface CompressionProgressListener {
    fun onProgressChanged(percent: Float)
    fun onProgressCancelled()
}