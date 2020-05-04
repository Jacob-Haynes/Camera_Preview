package com.example.camera_preview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation

// This is an arbitrary number we are using to keep track of the permission request
private const val REQUEST_CODE_PERMISSIONS = 10
// This is an array of all the permission specified in the manifest.
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

/**
 * This fragment is for managing permissions for camera
 */
class PermissionsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasPermissions(requireContext())){
            // Request camera-related permissions
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        } else {
            //if permissions have already been granted
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                PermissionsFragmentDirections.actionPermissionsToCamera())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()){
                //take user to the successful fragment when granted permission
                Toast.makeText(context, "Permission request granted", Toast.LENGTH_LONG).show()
                Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                    PermissionsFragmentDirections.actionPermissionsToCamera())
            } else {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        /**
         * convenience to check if all permissions are granted
         */
        fun hasPermissions(context: Context) = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
