package com.example.myadjustcontrast

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myadjustcontrast.CustomView.MatrixImageView.MatrixImageView
import java.io.ByteArrayOutputStream
import java.io.File

class MainActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_CODE_ASK_FOR_PERMISSIONS = 123 // 选择一个未被使用的整数作为请求码
        const val ALBUM_RESULT_CODE = 0x999
    }

    private lateinit var imageView: MatrixImageView
//    private lateinit var imageView: ImageView
    private lateinit var contrastSeekBar: SeekBar
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var saturabilitySeekBar: SeekBar
    private lateinit var saveImageButton :Button
    private lateinit var submitImageButton:Button
    private lateinit var editText:EditText
    private lateinit var getText:Button

    private lateinit var bitmap:Bitmap


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.miv_sample)
        contrastSeekBar = findViewById(R.id.contrastSeekBar)
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        saturabilitySeekBar = findViewById(R.id.saturabilitySeekBar)
        saveImageButton = findViewById(R.id.saveImageButton)
        submitImageButton = findViewById(R.id.submitImageButton)
//        getText = findViewById(R.id.getText)
//        editText = findViewById(R.id.editText)

        contrastSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyAdjustments(progress, brightnessSeekBar.progress, saturabilitySeekBar.progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyAdjustments(contrastSeekBar.progress, progress, saturabilitySeekBar.progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saturabilitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyAdjustments(contrastSeekBar.progress, brightnessSeekBar.progress, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveImageButton.setOnClickListener{
            checkAndRequestPermission()
        }

        submitImageButton.setOnClickListener{
            openSysAlbum()
        }
//        getText.setOnClickListener{
//            val text = editText.text.toString()
//
//        }
    }

    private fun applyAdjustments(contrastValue: Int, brightnessValue: Int, saturabilityValue: Int) {
        // 根据对比度SeekBar的进度值调整对比度，生成对应的ColorMatrix
        val contrastMatrix = adjustContrast(contrastValue)
        // 根据亮度SeekBar的进度值调整亮度，生成对应的ColorMatrix
        val brightnessMatrix = adjustBrightness(brightnessValue)
        // 根据饱和度SeekBar的进度值调整饱和度，生成对应的ColorMatrix
        val saturationMatrix = adjustSaturability(saturabilityValue)

        // 创建一个新的ColorMatrix对象，用于存储最终的颜色变换矩阵
        val combinedMatrix = ColorMatrix()
        // 将对比度矩阵应用到combinedMatrix上
        combinedMatrix.postConcat(contrastMatrix)
        // 将亮度矩阵应用到combinedMatrix上，注意这里是顺序执行，对比度先于亮度
        combinedMatrix.postConcat(brightnessMatrix)
        // 将饱和度矩阵应用到combinedMatrix上，顺序在对比度和亮度之后
        combinedMatrix.postConcat(saturationMatrix)

        // 将最终的颜色变换矩阵设置为ImageView的颜色过滤器
        // ColorMatrixColorFilter是Android中用于应用颜色变换矩阵的类
        imageView.colorFilter = ColorMatrixColorFilter(combinedMatrix)
    }

//--------------------------------------------------------------------------------------------------------


    //onRequestPermissionsResult 回调处理权限请求的结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 处理权限请求结果
    }

    // 在你的Activity或Fragment中注册ActivityResultLauncher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveImageToGallery()
        }
    }

    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 如果是Android 11或更高版本，检查是否获得了管理外部存储的权限
            if (!Environment.isExternalStorageManager()) {
                // 启动一个活动，让用户授予管理所有文件的权限
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", packageName))
                startActivityForResult(intent, REQUEST_CODE_ASK_FOR_PERMISSIONS)
            } else {
                saveImageToGallery()
            }
        } else {
            // 对于较早的版本，请求WRITE_EXTERNAL_STORAGE权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                saveImageToGallery()
            }
        }
    }

    private fun saveImageToGallery() {
        val imageView: MatrixImageView = findViewById(R.id.miv_sample)
        val bitmap = getBitmapFromView(imageView)
        saveBitmapToExternalStorage(bitmap)
    }

    private fun getBitmapFromView(view: View): Bitmap {
        // 创建一个和View尺寸相同的Bitmap
        val originalBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(originalBitmap)
        // 绘制背景和View内容
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        // 创建一个新的Matrix对象，并设置为缩放模式
        val matrix = Matrix()
        matrix.postScale(2f, 2f, view.width / 2f, view.height / 2f) // 放大2倍，以中心点为缩放中心
        // 创建一个新的Bitmap用于存放缩放后的图像
        val scaledBitmap = Bitmap.createBitmap(
            originalBitmap, 0, 0,
            originalBitmap.width, originalBitmap.height,
            matrix, true
        )
        // 释放原始Bitmap的内存
        originalBitmap.recycle()
//
//        val mHeight = dp2px(this,300f)
//        val mWidth = dp2px(this,200f)
//        Log.e("TAG","mHeight->$mHeight mWidth->$mWidth")

        return scaledBitmap
    }


    private fun saveBitmapToExternalStorage(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 对于Android 10及更高版本，使用MediaStore API
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "New Image.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                val outputStream = resolver.openOutputStream(it)
                outputStream?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    runOnUiThread{
                        Toast.makeText(this,"图片已保存到相册！",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // 对于Android 9及更低版本，使用传统方式
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val filePath = "${Environment.getExternalStorageDirectory()}/Pictures/New Image.jpg"
            File(filePath).outputStream().buffered().use {
                it.write(bytes.toByteArray())
                runOnUiThread{
                    Toast.makeText(this,"图片已保存到相册！",Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

//    --------------------------------------------------------------------------------------------

    // onActivityResult 回调处理从其他 Activity 返回的结果
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //设置了一个条件语句来检查请求码是否与预期的相册请求码 ALBUM_RESULT_CODE 匹配，结果码是否表示操作成功，以及返回的 Intent 数据是否不为空。
        if (requestCode == ALBUM_RESULT_CODE && resultCode == Activity.RESULT_OK && data != null) {
            //调用 handleImageOnKitKat 函数来处理返回的图片 Intent。
            handleImageOnKitKat(data)
        }
    }

    // openSysAlbum 函数打开系统相册供用户选择图片
    @SuppressLint("IntentReset")
    private fun openSysAlbum() {
        val albumIntent = Intent(Intent.ACTION_PICK).apply {
            // 设置 intent 的 type 为 "image/*"，这表明我们只对图片文件感兴趣
            type = "image/*"
            // 设置 intent 的 data 为 MediaStore.Images.Media.EXTERNAL_CONTENT_URI，这指向设备上的外部图片存储
            data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        // // 使用 startActivityForResult 方法启动相册选择，
        // 并通过传入的 Intent 和请求码 ALBUM_RESULT_CODE 来处理返回结果
        startActivityForResult(albumIntent, ALBUM_RESULT_CODE)
    }

    /**
     * 这个函数主要处理两种类型的 URI：
     *
     * Document 类型的 URI：
     * 这是 Android 4.4（KitKat）引入的一种新的 URI 形式，
     * 用于访问存储在不同存储提供商中的文件。
     * 函数通过 DocumentsContract 来解析这些 URI，
     * 并根据不同的 authority 来获取实际的图片路径。
     *
     * Content 和 File 类型的 URI：
     * 对于 content 类型的 URI，函数直接查询 MediaStore 来获取图片路径；
     * 对于 file 类型的 URI，函数直接使用 URI 的 path 作为文件路径。
     */
    // handleImageOnKitKat 函数处理从系统相册返回的图片 URI
    private fun handleImageOnKitKat(data: Intent?) {
        var imagePath: String? = null
        // 从传递给该函数的 Intent 中获取数据（即选中图片的 URI）
        val uri = data?.data
        // 检查 uri 是否不为空，并且是 Document 类型的 URI
        if (uri != null && DocumentsContract.isDocumentUri(this, uri)) {
            // 获取 Document URI 的文档 ID
            val docId = DocumentsContract.getDocumentId(uri)
            // 获取 URI 的 authority
            val authority = uri.authority
            when (authority) {
                "com.android.providers.media.documents" -> {
                    // 解析文档 ID 来获取图片在 MediaStore 中的 ID
                    val id = docId.split(":")[1]
                    // 构建查询 MediaStore 的条件
                    val selection = "${MediaStore.Images.Media._ID} = $id"
                    // 调用 getImagePath 函数来获取图片的实际路径
                    imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection)
                }
                "com.android.providers.downloads.documents" -> {
                    // 解析文档 ID 并获取下载内容的 URI
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content: //downloads/public_downloads"),
                        //这行代码接收一个 String 类型的 docId，尝试将其解析为一个长整型（long）的数值，并返回该数值对应的 Long 对象。
                        //用于将字符串转换为 Long 类型的对象
                        java.lang.Long.valueOf(docId)
                    )
                    // 调用 getImagePath 函数来获取图片的实际路径
                    imagePath = getImagePath(contentUri, null)
                }
            }
        } else {
            // 如果 uri 不是 Document 类型，则根据 scheme 来获取图片路径
            if ("content" == uri?.scheme) {
                imagePath = getImagePath(uri, null)
                // 对于 file 类型的 URI，直接获取路径
            } else if ("file" == uri?.scheme)
            {
                imagePath = uri.path
            }
        }
        // 如果 imagePath 不为空，则调用 displayImage 函数来显示图片
        imagePath?.let { displayImage(it) }
    }

    @SuppressLint("Range")
    private fun getImagePath(uri: Uri, selection: String?): String? {
        // 使用 contentResolver 执行查询操作，查询与给定 URI 相关的数据
        val cursor = contentResolver.query(uri, null, selection, null, null)
        var path: String? = null
        // 使用 cursor?.use 来确保游标在操作完成后会被关闭，即使发生异常也是如此
        cursor?.use {
            // 将游标移动到第一行数据
            if (it.moveToFirst()) {
                // 获取图片路径的列索引，使用 MediaStore.Images.Media.DATA 作为列名
                // 从游标中取出第一行的图片路径数据，并将其赋值给 path 变量
                path = it.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))
            }
        }
        return path
    }

    private fun displayImage(imagePath: String) {
        // 使用 BitmapFactory.decodeFile 将文件路径 imagePath 对应的图片文件解码成 Bitmap 对象
        // 并将这个 Bitmap 对象赋值给已声明的 lateinit 变量 bitmap
        bitmap = BitmapFactory.decodeFile(imagePath)
        // 并调用 ImageView 的 setBitmap 方法，将 bitmap 设置到 ImageView 上，从而显示图片
        imageView.setImageBitmap(bitmap)
//        adjustImageScale()
//        imageView.setImageBitmap(bitmap)

    }

    private fun adjustImageScale() {

        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        Log.e("TAG","bitmapWidth -> $bitmapWidth  bitmapHeight -> $bitmapHeight")

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        Log.e("TAG","screenWidth -> $screenWidth  screenHeight -> $screenHeight")

        // 计算缩放比例
        val scaleWidth = screenWidth / bitmapWidth.toFloat()
        val scaleHeight = screenHeight / bitmapHeight.toFloat()
        val scale = Math.min(scaleWidth, scaleHeight)

        // 创建一个新的 Bitmap 并应用缩放
        val newBitmap = Bitmap.createScaledBitmap(bitmap, (bitmapWidth * scale).toInt(), (bitmapHeight * scale).toInt(), true)
        imageView.setImageBitmap(newBitmap)
    }


    private fun dp2px(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }
}

