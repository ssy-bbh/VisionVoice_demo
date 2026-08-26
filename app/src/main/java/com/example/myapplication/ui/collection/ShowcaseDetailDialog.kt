package com.example.myapplication.ui.collection

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.AppDatabase // 🚨 请确保导入了你的数据库类
import com.example.myapplication.utils.GyroscopeHelper
import com.example.myapplication.utils.ShaderManager
import com.example.myapplication.utils.StorageHelper // 🚨 导入我们刚才写的存储引擎
import kotlinx.coroutines.launch
import java.io.File

class ShowcaseDetailDialog : DialogFragment() {

    private var word: String? = null
    private var imagePath: String? = null
    private var score: Int = 0

    private var gyroHelper: GyroscopeHelper? = null
    private var initialPitch: Float? = null
    private var initialRoll: Float? = null

    // 🚨 核心战线 1：注册系统级相册选择器 (极简无权限申请模式)
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && word != null) {
            // 拿到相册图片了！启动协程丢给引擎去覆盖！
            lifecycleScope.launch {
                // 🚨 防闪退：弹窗可能在选图期间被关掉，requireContext() 会抛 IllegalStateException
                if (!isAdded) return@launch
                val appContext = requireContext().applicationContext
                val appDao = AppDatabase.getInstance(appContext).appDao()

                val savedPath = StorageHelper.saveAndOverwriteImage(
                    context = appContext,
                    targetWord = word!!,
                    bitmap = null,
                    uri = uri, // 传相册拿到的 Uri
                    appDao = appDao
                )

                if (savedPath != null) {
                    // 覆盖成功！立刻刷新当前界面的图片
                    val ivPhoto = view?.findViewById<ImageView>(R.id.ivDetailPhoto)
                    ivPhoto?.setImageURI(Uri.parse(savedPath))
                }
            }
        }
    }

    companion object {
        // 🚨 防闪退：imagePath 必须允许为 null。
        // Java 侧 ShowcaseItem.bestImagePath 是平台类型，解锁时可能没图（AR 帧为空的降级路径），
        // 若声明为非空 String，传 null 进来会直接抛 NPE 闪退。
        fun newInstance(word: String, imagePath: String?, score: Int): ShowcaseDetailDialog {
            val dialog = ShowcaseDetailDialog()
            val args = Bundle().apply {
                putString("WORD", word)
                putString("IMAGE", imagePath)
                putInt("SCORE", score)
            }
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
        arguments?.let {
            word = it.getString("WORD")
            imagePath = it.getString("IMAGE")
            score = it.getInt("SCORE")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_showcase_detail, container, false)
        view.findViewById<View>(R.id.rootContainer).setOnClickListener { dismiss() }

        val tvWord = view.findViewById<TextView>(R.id.tvDetailWord)
        val tvScore = view.findViewById<TextView>(R.id.tvDetailScore)
        val ivPhoto = view.findViewById<ImageView>(R.id.ivDetailPhoto)
        val cardContainer = view.findViewById<View>(R.id.cardContainer)

        // 🚨 核心战线 2：给图片加上长按事件，呼出相册！
        ivPhoto.setOnLongClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            true // 返回 true 表示我们消费了这个长按事件
        }

        word?.let { if (it.isNotEmpty()) tvWord.text = it.substring(0, 1).uppercase() + it.substring(1) }
        tvScore.text = "$score%"

        if (!imagePath.isNullOrEmpty()) {
            try {
                if (imagePath!!.startsWith("content://") || imagePath!!.startsWith("file://")) {
                    ivPhoto.setImageURI(Uri.parse(imagePath))
                } else {
                    val imgFile = File(imagePath!!)
                    if (imgFile.exists()) ivPhoto.setImageURI(Uri.fromFile(imgFile))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 硬件层离屏纹理缓存
        ivPhoto.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shader = ShaderManager.createNewLaserShader()
            shader.setFloatUniform("uFresnelPower", 1.0f)
            cardContainer.cameraDistance = 8000f * resources.displayMetrics.density

            gyroHelper = GyroscopeHelper(requireContext()) { pitch, roll ->
                if (initialPitch == null) initialPitch = pitch
                if (initialRoll == null) initialRoll = roll

                val deltaPitch = pitch - initialPitch!!
                val deltaRoll = roll - initialRoll!!

                val clampedPitch = (deltaPitch * 1.5f).coerceIn(-1.0f, 1.0f)
                val clampedRoll = (deltaRoll * 1.5f).coerceIn(-1.0f, 1.0f)

                shader.setFloatUniform("uGyroOffset", clampedPitch, clampedRoll)
                cardContainer.rotationX = clampedPitch * 15f
                cardContainer.rotationY = clampedRoll * -15f
                ivPhoto.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "uBaseMap"))
            }
        }

        // 入场动画
        cardContainer.alpha = 0f
        cardContainer.scaleX = 0.7f
        cardContainer.scaleY = 0.7f

        cardContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(450)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .start()

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attributes.blurBehindRadius = 40
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            } else {
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0.6f)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gyroHelper?.start()
    }

    override fun onPause() {
        super.onPause()
        gyroHelper?.stop()
    }
}