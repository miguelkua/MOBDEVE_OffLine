package com.kua.miguel.mobdeve.s11.argamosakuamp.dialogs

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.DialogEditEntryBinding
import com.yalantis.ucrop.UCrop
import java.io.File

class EditEntryDialogFragment : DialogFragment() {

    private var _binding: DialogEditEntryBinding? = null
    private val viewBinding get() = _binding!!

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
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private var progressDialog: ProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogEditEntryBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the content until data is loaded
        viewBinding.root.visibility = View.INVISIBLE

        entryId = arguments?.getString(ARG_ENTRY_ID)

        setupViews()
        preloadEntryData()
    }

    private fun setupViews() {
        // Set up character count TextWatcher
        viewBinding.etEditItemName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val charCount = s?.length ?: 0
                viewBinding.tvEditCharacterCount.text = "$charCount / 25 Characters"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewBinding.btnEditIncrease.setOnClickListener {
            var quantity = viewBinding.tvEditQuantity.text.toString().toInt()
            if (quantity < 99) {
                quantity++
                viewBinding.tvEditQuantity.text = quantity.toString()
            } else {
                Toast.makeText(context, "Maximum quantity is 99", Toast.LENGTH_SHORT).show()
            }
        }

        viewBinding.btnEditDecrease.setOnClickListener {
            var quantity = viewBinding.tvEditQuantity.text.toString().toInt()
            if (quantity > 1) {
                quantity--
                viewBinding.tvEditQuantity.text = quantity.toString()
            } else {
                Toast.makeText(context, "Quantity cannot be less than 1", Toast.LENGTH_SHORT).show()
            }
        }

        viewBinding.btnEditEntry.setOnClickListener {
            val itemName = viewBinding.etEditItemName.text.toString()
            val quantity = viewBinding.tvEditQuantity.text.toString().toInt()

            if (itemName.isEmpty()) {
                Toast.makeText(context, "Item name cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (itemName.length > 25) {
                Toast.makeText(context, "Item name cannot exceed 25 characters", Toast.LENGTH_SHORT).show()
            } else {
                if (isImageChanged || itemName != oldItemName || quantity != oldQuantity) {
                    if (isImageChanged) {
                        showProgressDialog()
                    }
                    updateEntryInFirebase(itemName, quantity, croppedImageUri)
                } else {
                    Toast.makeText(context, "No changes detected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewBinding.btnEditImage.setOnClickListener {
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

        viewBinding.btnEditCancel.setOnClickListener {
            dismiss()
        }
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

                        oldItemName = itemName ?: ""
                        oldQuantity = quantity ?: 0

                        viewBinding.etEditItemName.setText(itemName)
                        viewBinding.tvEditQuantity.text = quantity?.toString()

                        if (imageUriString != null) {
                            try {
                                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUriString)
                                val oneMegaByte: Long = 1024 * 1024
                                storageReference.getBytes(oneMegaByte)
                                    .addOnSuccessListener { bytes ->
                                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        viewBinding.ivEditBarcodeSample.setImageBitmap(bitmap)
                                        viewBinding.root.visibility = View.VISIBLE
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("EditEntryDialog", "Failed to load image: ${exception.message}")
                                        viewBinding.root.visibility = View.VISIBLE // Show view even if image loading fails
                                    }
                            } catch (e: IllegalArgumentException) {
                                Log.e("EditEntryDialog", "Invalid image URI: $imageUriString")
                                viewBinding.root.visibility = View.VISIBLE // Show view even if image URI is invalid
                            }
                        } else {
                            viewBinding.root.visibility = View.VISIBLE // Show view if no image URI
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to load entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                    viewBinding.root.visibility = View.VISIBLE // Show view even if Firestore document loading fails
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
                val tempFile = File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                val outputStream = tempFile.outputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                imageUri = Uri.fromFile(tempFile)
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
                viewBinding.ivEditBarcodeSample.setImageURI(it)
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

    private fun updateEntryInFirebase(itemName: String, quantity: Int, newImageUri: Uri?) {
        val userId = auth.currentUser?.uid
        if (userId != null && entryId != null) {
            val documentRef = firestore.collection("users").document(userId)
                .collection("currentList").document(entryId!!)

            if (newImageUri != null) {
                val storageRef = storage.reference
                val oldImageRef = documentRef.get().continueWith { task ->
                    task.result?.getString("imageUri")?.let { FirebaseStorage.getInstance().getReferenceFromUrl(it) }
                }

                val imageRef = storageRef.child("images/$userId/${System.currentTimeMillis()}_${newImageUri.lastPathSegment}")
                imageRef.putFile(newImageUri)
                    .addOnSuccessListener { taskSnapshot ->
                        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            documentRef.update("itemName", itemName, "quantity", quantity, "imageUri", downloadUri.toString())
                                .addOnSuccessListener {
                                    oldImageRef.result?.delete()?.addOnCompleteListener {
                                        Log.d("EditEntryDialog", "Old image deleted")
                                    }
                                    Toast.makeText(context, "Entry updated successfully", Toast.LENGTH_SHORT).show()
                                    dismiss()
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(context, "Failed to update entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("EditEntryDialog", "Failed to update entry", exception)
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
                        Log.e("EditEntryDialog", "Failed to upload image", exception)
                    }
                    .addOnCompleteListener {
                        dismissProgressDialog()
                    }
            } else {
                documentRef.update("itemName", itemName, "quantity", quantity)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Entry updated successfully", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Failed to update entry: ${exception.message}", Toast.LENGTH_SHORT).show()
                        Log.e("EditEntryDialog", "Failed to update entry", exception)
                    }
                    .addOnCompleteListener {
                        dismissProgressDialog()
                    }
            }
        }
    }

    private fun showProgressDialog() {
        progressDialog = ProgressDialog(context).apply {
            setMessage("Updating entry...")
            setCancelable(false)
            show()
        }
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ENTRY_ID = "entry_id"
        private var oldItemName: String = ""
        private var oldQuantity: Int = 0

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
