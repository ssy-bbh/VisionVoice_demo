package com.example.myapplication.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

class YoloDetector(
    private val context: Context,
    private val modelPath: String,
    private val labelsPath: String,
    private val inputSize: Int = 640,
    private val numThreads: Int = 4
) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val TAG = "VISION_DEBUG"

    // 模型参数
    private var outputChannels = 84
    private var outputAnchors = 8400
    private var isOutputTransposed = false
    private var isModelNCHW = false
    private var isInt8 = false

    // =========================================================================
    // 【关键优化】预分配内存区
    // 避免在 detect() 循环中重复创建对象，防止内存抖动(GC)导致的卡顿
    // =========================================================================
    private lateinit var inputBuffer: ByteBuffer
    private lateinit var outputBuffer: ByteBuffer
    private lateinit var intValues: IntArray

    // 日志限流（避免 Logcat 刷屏）
    private var lastLogTime = 0L

    init {
        initInterpreter()
        loadLabels()
    }

    private fun initInterpreter() {
        try {
            val assetFileDescriptor: AssetFileDescriptor = context.assets.openFd(modelPath)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options()
            options.setNumThreads(numThreads)
            interpreter = Interpreter(mappedByteBuffer, options)

            // 1. 检查输入 Tensor 格式
            val inputTensor = interpreter?.getInputTensor(0)
            val inputShape = inputTensor?.shape()
            val inputDataType = inputTensor?.dataType()
            Log.wtf(TAG, "!!! 模型输入详情 !!! Shape: ${inputShape?.contentToString()}, Type: $inputDataType")

            if (inputShape != null && inputShape.size == 4) {
                if (inputShape[1] == 3) {
                    isModelNCHW = true
                    Log.wtf(TAG, "检测到 NCHW 格式模型 (PyTorch 风格)")
                }
            }
            if (inputDataType == DataType.UINT8 || inputDataType == DataType.INT8) {
                isInt8 = true
                Log.wtf(TAG, "检测到量化模型 (Int8/Uint8)")
            }

            // 2. 检查输出 Tensor 格式
            val outputTensor = interpreter?.getOutputTensor(0)
            val shape = outputTensor?.shape()
            Log.wtf(TAG, "!!! 模型输出详情 !!! Shape: ${shape?.contentToString()}, Type: ${outputTensor?.dataType()}")

            if (shape != null && shape.size == 3) {
                // 自动判断输出维度是 [1, 84, 8400] 还是 [1, 8400, 84]
                if (shape[1] < shape[2]) {
                    outputChannels = shape[1]
                    outputAnchors = shape[2]
                    isOutputTransposed = true // 需要转置读取
                } else {
                    outputAnchors = shape[1]
                    outputChannels = shape[2]
                    isOutputTransposed = false // 标准格式
                }
            }

            // =========================================================================
            // 【关键实现】一次性分配内存 (DirectBuffer 效率更高)
            // =========================================================================
            val bytesPerChannel = if (isInt8) 1 else 4
            inputBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * bytesPerChannel)
            inputBuffer.order(ByteOrder.nativeOrder())

            outputBuffer = ByteBuffer.allocateDirect(1 * outputChannels * outputAnchors * 4)
            outputBuffer.order(ByteOrder.nativeOrder())

            intValues = IntArray(inputSize * inputSize)

            fileInputStream.close()
            assetFileDescriptor.close()
            Log.wtf(TAG, "!!! TFLite 模型加载成功: $modelPath (高性能全屏版) !!!")

        } catch (e: Exception) {
            Log.wtf(TAG, "!!! 模型加载失败 !!!", e)
        }
    }

    private fun loadLabels() {
        try {
            labels = context.assets.open(labelsPath).bufferedReader().use { it.readLines() }
            Log.wtf(TAG, "标签加载成功: ${labels.size} 个类别")
        } catch (e: Exception) {
            Log.wtf(TAG, "标签加载失败", e)
            labels = emptyList()
        }
    }

    fun detect(bitmap: Bitmap): List<Result> {
        if (interpreter == null) return emptyList()

        // =========================================================================
        // 【关键实现 1】预处理：直接拉伸 (Stretch) 到 640x640
        // 移除了复杂的 Letterbox (黑边) 逻辑。
        // 虽然图像会变形，但 YOLO 鲁棒性很强，且这样可以利用全屏像素，实现“沉浸式”识别。
        // =========================================================================
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        // 重置指针，准备写入数据
        inputBuffer.rewind()

        // 获取像素数据 (复用 intValues 数组)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        // 将像素填入 inputBuffer
        if (isModelNCHW) {
            // NCHW (R排完排G排B)
            for (c in 0 until 3) {
                for (i in 0 until inputSize * inputSize) {
                    val pix = intValues[i]
                    val channelValue = when(c) {
                        0 -> (pix shr 16 and 0xFF)
                        1 -> (pix shr 8 and 0xFF)
                        else -> (pix and 0xFF)
                    }
                    if (isInt8) inputBuffer.put(channelValue.toByte())
                    else inputBuffer.putFloat(channelValue / 255.0f)
                }
            }
        } else {
            // NHWC (RGB RGB RGB)
            for (pix in intValues) {
                val r = (pix shr 16 and 0xFF)
                val g = (pix shr 8 and 0xFF)
                val b = (pix and 0xFF)
                if (isInt8) {
                    inputBuffer.put(r.toByte()); inputBuffer.put(g.toByte()); inputBuffer.put(b.toByte())
                } else {
                    inputBuffer.putFloat(r / 255.0f); inputBuffer.putFloat(g / 255.0f); inputBuffer.putFloat(b / 255.0f)
                }
            }
        }

        // 执行推理
        outputBuffer.rewind()
        try {
            interpreter?.run(inputBuffer, outputBuffer)
        } catch (e: Exception) {
            Log.e(TAG, "推理错误", e)
            return emptyList()
        }
        outputBuffer.rewind()

        // 4. 后处理 (Post-processing)
        val results = ArrayList<Result>()
        val confThreshold = 0.50f // 只显示置信度 > 50% 的结果
        val iouThreshold = 0.45f

        for (i in 0 until outputAnchors) {
            var maxScore = 0f
            var maxClassIndex = -1

            // 寻找当前 Anchor 中得分最高的类别
            for (j in 4 until outputChannels) {
                val index = if (isOutputTransposed) (j * outputAnchors + i) else (i * outputChannels + j)
                val score = outputBuffer.getFloat(index * 4)
                if (score > maxScore) {
                    maxScore = score
                    maxClassIndex = j - 4
                }
            }

            if (maxScore > confThreshold) {
                // 读取坐标数据
                val cxIndex = if (isOutputTransposed) (0 * outputAnchors + i) else (i * outputChannels + 0)
                val cyIndex = if (isOutputTransposed) (1 * outputAnchors + i) else (i * outputChannels + 1)
                val wIndex = if (isOutputTransposed) (2 * outputAnchors + i) else (i * outputChannels + 2)
                val hIndex = if (isOutputTransposed) (3 * outputAnchors + i) else (i * outputChannels + 3)

                val rawCx = outputBuffer.getFloat(cxIndex * 4)
                val rawCy = outputBuffer.getFloat(cyIndex * 4)
                val rawW = outputBuffer.getFloat(wIndex * 4)
                val rawH = outputBuffer.getFloat(hIndex * 4)

                // =========================================================================
                // 【关键实现 2】坐标统一还原
                // 无论模型输出是归一化(0~1)还是像素级(0~640)，这里统一转回像素级，方便后续处理
                // =========================================================================
                val cx = if (rawW <= 1.0f) rawCx * inputSize else rawCx
                val cy = if (rawH <= 1.0f) rawCy * inputSize else rawCy
                val w = if (rawW <= 1.0f) rawW * inputSize else rawW
                val h = if (rawH <= 1.0f) rawH * inputSize else rawH

                // =========================================================================
                // 【关键实现 3】极简坐标映射 (全屏拉伸版)
                // 因为图片是直接拉伸的，没有黑边。
                // 所以：模型坐标(0~640) / inputSize(640) = 归一化坐标(0~1)
                // 这个 0~1 的坐标，直接对应手机屏幕的 0%~100% 位置。
                // =========================================================================
                val rect = RectF(
                    (cx - w / 2f) / inputSize,
                    (cy - h / 2f) / inputSize,
                    (cx + w / 2f) / inputSize,
                    (cy + h / 2f) / inputSize
                )

                // 边界限制，防止画出屏幕外
                rect.left = max(0f, min(1f, rect.left))
                rect.top = max(0f, min(1f, rect.top))
                rect.right = max(0f, min(1f, rect.right))
                rect.bottom = max(0f, min(1f, rect.bottom))

                if (rect.width() > 0 && rect.height() > 0) {
                    results.add(Result(rect, maxClassIndex, labels.getOrElse(maxClassIndex) { "Unknown" }, maxScore))
                }
            }
        }

        // NMS 非极大值抑制 (去除重叠框)
        val nmsResults = nms(results, iouThreshold)

        // 简单的心跳日志 (每3秒打印一次，证明活着)
        if (nmsResults.isNotEmpty()) {
            // Log.d(TAG, "识别到: ${nmsResults[0].label}")
        } else {
            val now = SystemClock.elapsedRealtime()
            if (now - lastLogTime > 3000) {
                Log.d(TAG, "Scanning... (Stretch Mode)")
                lastLogTime = now
            }
        }

        return nmsResults
    }

    private fun nms(boxes: ArrayList<Result>, threshold: Float): List<Result> {
        if (boxes.isEmpty()) return emptyList()
        boxes.sortWith { o1, o2 -> o2.score.compareTo(o1.score) }
        val selectedBoxes = ArrayList<Result>()
        val isSuppressed = BooleanArray(boxes.size)
        for (i in boxes.indices) {
            if (isSuppressed[i]) continue
            val boxA = boxes[i]
            selectedBoxes.add(boxA)
            for (j in i + 1 until boxes.size) {
                if (isSuppressed[j]) continue
                val boxB = boxes[j]
                if (calculateIoU(boxA.rect, boxB.rect) > threshold) {
                    isSuppressed[j] = true
                }
            }
        }
        return selectedBoxes
    }

    private fun calculateIoU(a: RectF, b: RectF): Float {
        val intersectionLeft = max(a.left, b.left)
        val intersectionTop = max(a.top, b.top)
        val intersectionRight = min(a.right, b.right)
        val intersectionBottom = min(a.bottom, b.bottom)
        val intersectionArea = max(0f, intersectionRight - intersectionLeft) * max(0f, intersectionBottom - intersectionTop)
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        val unionArea = areaA + areaB - intersectionArea
        return if (unionArea <= 0) 0f else intersectionArea / unionArea
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }


    data class Result(val rect: RectF, val classIndex: Int, val label: String, val score: Float)
}