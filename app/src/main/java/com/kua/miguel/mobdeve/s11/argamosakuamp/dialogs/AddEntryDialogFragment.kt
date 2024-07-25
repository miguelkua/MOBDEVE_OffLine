package com.kua.miguel.mobdeve.s11.argamosakuamp.dialogs

import android.app.Activity
import android.app.Dialog
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
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.DialogAddEntryBinding
import com.yalantis.ucrop.UCrop
import java.io.File

class AddEntryDialogFragment : DialogFragment() {

    private var _binding: DialogAddEntryBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_CAMERA_PERMISSION = 100
    private var imageUri: Uri? = null
    private var croppedImageUri: Uri? = null

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    interface AddEntryListener {
        fun onAddEntry(itemName: String, quantity: Int, imageUri: Uri?)
    }

    private var listener: AddEntryListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogAddEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var quantity = 1
        binding.tvQuantity.text = quantity.toString()

        // Set up character count TextWatcher
        binding.etItemName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val charCount = s?.length ?: 0
                binding.tvCharacterCount.text = "$charCount / 25 Characters"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnIncrease.setOnClickListener {
            if (quantity < 99) {
                quantity++
                binding.tvQuantity.text = quantity.toString()
            } else {
                Toast.makeText(context, "Maximum quantity is 99", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                binding.tvQuantity.text = quantity.toString()
            } else {
                Toast.makeText(context, "Quantity cannot be less than 1", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAddEntry.setOnClickListener {
            val itemName = binding.etItemName.text.toString()

            if (itemName.isEmpty()) {
                Toast.makeText(context, "Item name cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (itemName.length > 25) {
                Toast.makeText(context, "Item name cannot exceed 25 characters", Toast.LENGTH_SHORT).show()
            } else {
                saveEntryToFirebase(itemName, quantity, croppedImageUri)
                dismiss()
            }
        }

        binding.btnUploadImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    // For Android 10 and below, also request WRITE_EXTERNAL_STORAGE
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

        binding.btnCancel.setOnClickListener {
            resetDialog()
            dismiss()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                imageUri = saveImageToMediaStore(imageBitmap)
                if (imageUri != null) {
                    startCropActivity(imageUri!!)
                } else {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Image capture failed", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            croppedImageUri = UCrop.getOutput(data!!)
            if (croppedImageUri != null) {
                binding.ivBarcodeSample.setImageURI(null) // Clear previous image
                binding.ivBarcodeSample.setImageURI(croppedImageUri)
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
            .withAspectRatio(3f, 1f) // Aspect ratio suitable for barcodes (width:height)
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

    private fun saveEntryToFirebase(itemName: String, quantity: Int, imageUri: Uri?) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val entryId = firestore.collection("users").document(userId).collection("currentList").document().id
            val entry = mapOf(
                "itemName" to itemName,
                "quantity" to quantity,
                "imageUri" to imageUri?.toString()
            )

            Log.d("FirebaseDebug", "Attempting to save entry with ID: $entryId, data: $entry")

            firestore.collection("users").document(userId).collection("currentList").document(entryId).set(entry)
                .addOnSuccessListener {
                    Log.d("FirebaseDebug", "Successfully added entry with ID: $entryId")
                    if (context != null) {
                        Toast.makeText(context, "Entry added successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebaseError", "Failed to add entry with ID: $entryId", exception)
                    if (context != null) {
                        Toast.makeText(context, "Failed to add entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Log.w("FirebaseWarning", "User is not logged in")
            if (context != null) {
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun resetDialog() {
        binding.etItemName.text.clear()
        binding.tvQuantity.text = "1"
        binding.ivBarcodeSample.setImageResource(R.drawable.barcode_sample)
        binding.tvCharacterCount.text = "0 / 25 Characters"
        imageUri = null
        croppedImageUri = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setAddEntryListener(listener: AddEntryListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            // Set up the dialog dimensions, animations, etc.
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(context, "Camera permission required to capture image", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
