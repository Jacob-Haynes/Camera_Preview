package com.example.camera_preview

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: PreviewView

    private var displayId: Int = -1
    private var lenseFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var camera: Camera? = null

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    // exectutor
    private lateinit var cameraExecutor: ExecutorService

    // display listener for orientation changes
    private val displayListener = object : DisplayManager.DisplayListener{
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let {view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
            }
        } ?: Unit
    }

    override fun onResume() {
        super.onResume()
        // check permissions
        if (!PermissionsFragment.hasPermissions(requireContext())){
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraToPermissions()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_camera, container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        viewFinder = container.findViewById(R.id.view_finder)

        // executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // orientation change
        displayManager.registerDisplayListener(displayListener, null)

        // wait for views
        viewFinder.post {
            //what display
            displayId = viewFinder.display.displayId
            // use cases
            bindCameraUseCases()
        }
    }

    //bind preview
    private fun bindCameraUseCases() {
        // get screen metrics
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        //bind to lifecycle
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lenseFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable{
            //cameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // preview
            preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()

            //unbind
            cameraProvider.unbindAll()

            try {
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)
                //attach viewfinder to surface
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera?.cameraInfo))
            } catch (exc: Exception) {
                Log.e(TAG,"Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))

    }

    //aspectRatio check
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble()/min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)){
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    companion object {

        private const val TAG = "Camera_Preview"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

    }
}
