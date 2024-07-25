package com.kua.miguel.mobdeve.s11.argamosakuamp.dialogs

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kua.miguel.mobdeve.s11.argamosakuamp.R
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.DialogEditEntryBinding
import com.yalantis.ucrop.UCrop
import java.io.File

class EditEntryDialogFragment : DialogFragment() {

    private var _binding: DialogEditEntryBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_CAMERA_PERMISSION = 100
    private var imageUri: Uri? = null
    private var croppedImageUri: Uri? = null
    private var isImageChanged = false
    private var entryId: String? = null

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogEditEntryBinding.inflate(inflater, container, false)

        // Hide the content until data is loaded, for seamless viewing
        binding.root.visibility = View.INVISIBLE

        entryId = arguments?.getString(ARG_ENTRY_ID)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up character count TextWatcher
        binding.etEditItemName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val charCount = s?.length ?: 0
                binding.tvEditCharacterCount.text = "$charCount / 25 Characters"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnEditIncrease.setOnClickListener {
            var quantity = binding.tvEditQuantity.text.toString().toInt()
            if (quantity < 99) {
                quantity++
                binding.tvEditQuantity.text = quantity.toString()
            } else {
                Toast.makeText(context, "Maximum quantity is 99", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEditDecrease.setOnClickListener {
            var quantity = binding.tvEditQuantity.text.toString().toInt()
            if (quantity > 1) {
                quantity--
                binding.tvEditQuantity.text = quantity.toString()
            } else {
                Toast.makeText(context, "Quantity cannot be less than 1", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEditEntry.setOnClickListener {
            val itemName = binding.etEditItemName.text.toString()
            val quantity = binding.tvEditQuantity.text.toString().toInt()

            if (itemName.isEmpty()) {
                Toast.makeText(context, "Item name cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (itemName.length > 25) {
                Toast.makeText(context, "Item name cannot exceed 25 characters", Toast.LENGTH_SHORT).show()
            } else {
                updateEntryInFirebase(itemName, quantity, croppedImageUri)
                dismiss()
            }
        }

        binding.btnEditImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) {
                        dispatchTakePictureIntent()
                    } else {
                        requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CAMERA_PERMISSION)
                    }
                } else {
                    dispatchTakePictureIntent()
                }
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            }
        }

        binding.btnEditCancel.setOnClickListener {
            dismiss()
        }

        preloadEntryData()
    }

    private fun preloadEntryData() {
        val userId = auth.currentUser?.uid
        if (userId != null && entryId != null) {
            firestore.collection("users").document(userId)
                .collection("currentList").document(entryId!!)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val itemName = document.getString("itemName")
                        val quantity = document.getLong("quantity")?.toInt()
                        val imageUriString = document.getString("imageUri")

                        binding.etEditItemName.setText(itemName)
                        binding.tvEditQuantity.text = quantity?.toString()
                        if (imageUriString != null) {
                            imageUri = Uri.parse(imageUriString)
                            binding.ivEditBarcodeSample.setImageURI(imageUri)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to load entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    // Make the view visible after data is loaded
                    binding.root.visibility = View.VISIBLE
                }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(context, "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                imageUri = saveImageToMediaStore(imageBitmap)
                imageUri?.let {
                    isImageChanged = true
                    startCropActivity(it)
                } ?: run {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Image capture failed", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            croppedImageUri = UCrop.getOutput(data!!)
            croppedImageUri?.let {
                isImageChanged = true
                binding.ivEditBarcodeSample.setImageURI(it)
            }
        } else if (requestCode == UCrop.RESULT_ERROR) {
            Toast.makeText(context, "Crop error: ${UCrop.getError(data!!)?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCropActivity(sourceUri: Uri) {
        val uniqueFileName = "cropped_image_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, uniqueFileName))
        val options = UCrop.Options().apply {
            setCompressionQuality(100)
        }

        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(3f, 1f)
            .withOptions(options)
            .start(requireContext(), this)
    }

    private fun saveImageToMediaStore(bitmap: Bitmap): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${requireContext().getString(R.string.app_name)}")
            }
        }

        val uri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
        return uri
    }

    private fun getCurrentEntry(callback: (Map<String, Any>?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null && entryId != null) {
            firestore.collection("users").document(userId)
                .collection("currentList").document(entryId!!)
                .get()
                .addOnSuccessListener { document ->
                    callback(document?.data as? Map<String, Any>)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to retrieve current entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
        } else {
            callback(null)
        }
    }

    private fun updateEntryInFirebase(itemName: String, quantity: Int, imageUri: Uri?) {
        Log.d("EditEntryDialog", "Updating entry: itemName=$itemName, quantity=$quantity, imageUri=$imageUri")
        val userId = auth.currentUser?.uid
        if (userId != null && entryId != null) {
            getCurrentEntry { currentEntry ->
                if (currentEntry != null) {
                    val entry = mutableMapOf<String, Any>(
                        "itemName" to itemName,
                        "quantity" to quantity
                    )

                    if (isImageChanged) {
                        entry["imageUri"] = imageUri?.toString() ?: ""
                    } else {
                        currentEntry["imageUri"]?.let { existingImageUri ->
                            entry["imageUri"] = existingImageUri
                        }
                    }

                    firestore.collection("users").document(userId)
                        .collection("currentList").document(entryId!!)
                        .set(entry)
                        .addOnSuccessListener {
                            Log.d("EditEntryDialog", "Entry updated successfully")
                            context?.let { safeContext ->
                                Toast.makeText(safeContext, "Entry updated successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("EditEntryDialog", "Failed to update entry: ${exception.message}")
                            context?.let { safeContext ->
                                Toast.makeText(safeContext, "Failed to update entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Log.e("EditEntryDialog", "Current entry not found")
                    Toast.makeText(context, "Current entry not found", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.e("EditEntryDialog", "User or Entry ID is null")
            Toast.makeText(context, "User or Entry ID is null", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_ENTRY_ID = "entry_id"

        fun newInstance(entryId: String): EditEntryDialogFragment {
            val fragment = EditEntryDialogFragment()
            val args = Bundle().apply {
                putString(ARG_ENTRY_ID, entryId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
