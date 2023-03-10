package com.example.saveimage

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.saveimage.Constants.TAG
import com.example.saveimage.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture?= null
    private lateinit var outputDirectory:File
    private lateinit var bitmap: Bitmap
    private lateinit var name:String

    private var UPLOAD_URL = "http://192.168.1.77/prueba/upload.php"
    private var KEY_IMAGE = "foto";
    private var KEY_NOMBRE = "nombre";
    private var dialog:AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        outputDirectory = getOutputDirectory()

        if(allPermissionGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(
                this,Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        binding.btnTake.setOnClickListener{
            takePhoto()
        }
    }

    private fun encodeImage(bm: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return android.util.Base64.encodeToString(b, android.util.Base64.DEFAULT)
    }

     fun uploadImage(url:String){
         val queue = Volley.newRequestQueue(this)
         val stringRequest = object :StringRequest(Method.POST, url,
             { response ->
                 Toast.makeText(this@MainActivity,response,Toast.LENGTH_LONG).show()
             },
             { error-> Toast.makeText(this@MainActivity, error.message,Toast.LENGTH_LONG).show()})

         {
             @Override
             override fun getParams(): MutableMap<String, String> {
                 val hashMap = HashMap<String, String>()
                 hashMap.put(KEY_IMAGE,encodeImage(bitmap))
                 hashMap.put(KEY_NOMBRE,name)
                 return hashMap
             }

         }
         queue.add(stringRequest)
    }

    private fun getOutputDirectory() : File{
        val mediaDir = externalMediaDirs.firstOrNull()?.let{
            mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdir()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun takePhoto(){
        val imageCapture = imageCapture?:return

        val photofile = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault()).format(System.currentTimeMillis())+".jpg")
        var outputOption = ImageCapture.OutputFileOptions.Builder(photofile).build()
        name =System.currentTimeMillis().toString()
            imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photofile)
                    val msg = "Photo saved"
                    if (Build.VERSION.SDK_INT >= 29) {
                        val source = ImageDecoder.createSource(
                            applicationContext.contentResolver, savedUri
                        )
                        try {
                            bitmap = ImageDecoder.decodeBitmap(source)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(
                                applicationContext.contentResolver,
                                savedUri
                            )
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    uploadImage(UPLOAD_URL)

                    Toast.makeText(this@MainActivity, "$msg $savedUri",Toast.LENGTH_SHORT ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG,"Onerror: ${exception.message}",exception)
                }

            }
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                startCamera()
            }else{
                Toast.makeText(this,"Permission not granted by the user", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider:ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also{ mPreview->
                mPreview.setSurfaceProvider(
                    binding.viewFinder.surfaceProvider
                )
            }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try{
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            }catch (e:Exception){
                Log.d(TAG,"start camera fail",e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all{
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

}

