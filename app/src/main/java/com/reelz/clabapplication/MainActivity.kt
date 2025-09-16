package com.reelz.clabapplication

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.reelz.clabapplication.databinding.ActivityMainBinding
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface


class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var classifier: MobilenetClassifier

    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val bmp = loadBitmapFromUri(it)
            binding.imagePreview.setImageBitmap(bmp)
            val top3 = classifier.classify(bmp)
            binding.txtResult.text = top3.joinToString("\n") { (label, score) ->
                "• $label (${String.format("%.2f", score * 100)}%)"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        classifier = MobilenetClassifier(this)
        classifier.load(useGpu = false, useNnapi = false, numThreads = 4) // Use CPU 4 threads

        binding.btnPick.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // API 28+
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            val bmp: Bitmap = contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input) ?: error("Failed to decode stream")
            } ?: error("Failed to open InputStream")

            rotateIfRequired(uri, bmp)
        }
    }

    // EXIF Orientation Calibration
    private fun rotateIfRequired(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            contentResolver.openInputStream(uri).use { input ->
                val exif = ExifInterface(input!!)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90  -> bitmap.rotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> bitmap.rotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> bitmap.rotate(270f)
                    else -> bitmap
                }
            }
        } catch (_: Throwable) { bitmap }
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
    }

    override fun onDestroy() {
        classifier.close()
        super.onDestroy()
    }
}
