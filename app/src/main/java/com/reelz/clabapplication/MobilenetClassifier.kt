package com.reelz.clabapplication

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.classifier.Classifications

class MobilenetClassifier(private val context: Context) {

    @Volatile private var classifier: ImageClassifier? = null

    fun load(
        useGpu: Boolean = false,
        useNnapi: Boolean = false,
        numThreads: Int = 4,
        modelFile: String = "mobilenet_v2_1.0_224.tflite"
    ) {
        if (classifier != null) return

        val base = BaseOptions.builder()
            .setNumThreads(numThreads)
            .apply {
                if (useGpu) useGpu()
                if (useNnapi) useNnapi()
            }
            .build()

        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(base)
            .setMaxResults(3)
            .build()

        classifier = ImageClassifier.createFromFileAndOptions(context, modelFile, options)
    }

    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        val c = classifier ?: error("Classifier not loaded. Call load() first.")
        val input = TensorImage.fromBitmap(bitmap)
        val results: List<Classifications> = c.classify(input)
        if (results.isEmpty()) return emptyList()

        // Top-1 리스트만 꺼내서 상위 3개를 반환
        return results[0].categories
            .sortedByDescending { it.score }
            .take(3)
            .map { (it.displayName.ifBlank { it.label }) to it.score }
    }

    fun close() {
        classifier?.close()
        classifier = null
    }
}
