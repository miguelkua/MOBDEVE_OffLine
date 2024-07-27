package com.kua.miguel.mobdeve.s11.argamosakuamp.dialogs

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
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
import com.google.firebase.storage.FirebaseStorage
import com.kua.miguel.mobdeve.s11.argamosakuamp.R
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.DialogAddEntryBinding
import com.yalantis.ucrop.UCrop
import java.io.File

class AddEntryDialogFragment : DialogFragment() {

    private var _binding: DialogAddEntryBinding? = null
    private val viewBinding get() = _binding!!

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
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private var progressDialog: ProgressDialog? = null

    interface AddEntryListener {
        fun onAddEntry(itemName: String, quantity: Int, imageUri: Uri?)
    }

    private var listener: AddEntryListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogAddEntryBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var quantity = 1
        viewBinding.tvQuantity.text = quantity.toString()

        // Set up character count TextWatcher
        viewBinding.etItemName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val charCount = s?.length ?: 0
                viewBinding.tvCharacterCount.text = "$charCount / 25 Characters"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewBinding.btnIncrease.setOnClickListener {
            if (quantity < 99) {
                quantity++
                viewBinding.tvQuantity.text = quantity.toString()
            } else {
                Toast.makeText(context, "Maximum quantity is 99", Toast.LENGTH_SHORT).show()
            }
        }

        viewBinding.btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                viewBinding.tvQuantity.text = quantity.toString()
            } else {
                Toast.makeText(context, "Quantity cannot be less than 1", Toast.LENGTH_SHORT).show()
            }
        }

        viewBinding.btnAddEntry.setOnClickListener {
            val itemName = viewBinding.etItemName.text.toString()

            if (itemName.isEmpty()) {
                Toast.makeText(context, "Item name cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (itemName.length > 25) {
                Toast.makeText(context, "Item name cannot exceed 25 characters", Toast.LENGTH_SHORT).show()
            } else if (croppedImageUri == null) {
                Toast.makeText(context, "Please upload an image", Toast.LENGTH_SHORT).show()
            } else {
                showProgressDialog()
                uploadImageAndSaveEntry(itemName, quantity, croppedImageUri)
            }
        }

        viewBinding.btnUploadImage.setOnClickListener {
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

        viewBinding.btnCancel.setOnClickListener {
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
                // Save bitmap to a file in cache and get URI
                val tempFile = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                val outputStream = tempFile.outputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                // Create URI for cropping
                imageUri = Uri.fromFile(tempFile)
                startCropActivity(imageUri!!)
            } else {
                Toast.makeText(context, "Image capture failed", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            croppedImageUri = UCrop.getOutput(data!!)
            if (croppedImageUri != null) {
                viewBinding.ivBarcodeSample.setImageURI(null) // Clear previous image
                viewBinding.ivBarcodeSample.setImageURI(croppedImageUri)
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

    private fun uploadImageToFirebase(uri: Uri, onComplete: (String?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("images/${userId}/${System.currentTimeMillis()}.jpg")
        val uploadTask = storageRef.putFile(uri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                onComplete(downloadUri.toString())
            }.addOnFailureListener { e ->
                Log.e("FirebaseStorageError", "Failed to get download URL", e)
                onComplete(null)
            }
        }.addOnFailureListener { e ->
            Log.e("FirebaseStorageError", "Failed to upload image", e)
            onComplete(null)
        }
    }

    private fun uploadImageAndSaveEntry(itemName: String, quantity: Int, imageUri: Uri?) {
        imageUri?.let {
            uploadImageToFirebase(it) { downloadUrl ->
                if (downloadUrl != null) {
                    saveEntryToFirebase(itemName, quantity, downloadUrl)
                } else {
                    dismissProgressDialog()
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } ?: run {
            dismissProgressDialog()
            if (isAdded) {
                Toast.makeText(requireContext(), "Image URI is null", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveEntryToFirebase(itemName: String, quantity: Int, imageUrl: String?) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val entryId = firestore.collection("users").document(userId).collection("currentList").document().id
            val entry = mapOf(
                "itemName" to itemName,
                "quantity" to quantity,
                "imageUri" to imageUrl
            )

            Log.d("FirebaseDebug", "Attempting to save entry with ID: $entryId, data: $entry")

            firestore.collection("users").document(userId).collection("currentList").document(entryId).set(entry)
                .addOnSuccessListener {
                    Log.d("FirebaseDebug", "Successfully added entry with ID: $entryId")
                    dismissProgressDialog()
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Entry added successfully", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirebaseError", "Failed to add entry with ID: $entryId", exception)
                    dismissProgressDialog()
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to add entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Log.w("FirebaseWarning", "User is not logged in")
            dismissProgressDialog()
            if (isAdded) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProgressDialog() {
        progressDialog = ProgressDialog(context).apply {
            setMessage("Processing...")
            setCancelable(false)
            show()
        }
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun resetDialog() {
        viewBinding.etItemName.text.clear()
        viewBinding.tvQuantity.text = "1"
        viewBinding.ivBarcodeSample.setImageResource(R.drawable.barcode_sample)
        viewBinding.tvCharacterCount.text = "0 / 25 Characters"
        imageUri = null
        croppedImageUri = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissProgressDialog()
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
