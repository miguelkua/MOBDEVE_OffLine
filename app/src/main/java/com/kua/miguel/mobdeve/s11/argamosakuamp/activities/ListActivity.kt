package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.kua.miguel.mobdeve.s11.argamosakuamp.R
import com.kua.miguel.mobdeve.s11.argamosakuamp.adapters.ListAdapter
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityListBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.dialogs.AddEntryDialogFragment
import com.kua.miguel.mobdeve.s11.argamosakuamp.dialogs.EditEntryDialogFragment
import com.kua.miguel.mobdeve.s11.argamosakuamp.helpers.SwipeHelper
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class ListActivity : AppCompatActivity(), AddEntryDialogFragment.AddEntryListener {

    private lateinit var data: ArrayList<EntryModel>
    private lateinit var recyclerView: RecyclerView
    private lateinit var listAdapter: ListAdapter
    private lateinit var viewBinding: ActivityListBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        viewBinding = ActivityListBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Setup RecyclerView
        recyclerView = viewBinding.recyclerViewItems
        recyclerView.layoutManager = LinearLayoutManager(this)
        data = arrayListOf()
        listAdapter = ListAdapter(data)
        recyclerView.adapter = listAdapter

        // Setup ItemTouchHelper for swipe actions
        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(recyclerView) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                return listOf(
                    deleteButton(position),
                    editButton(position)
                )
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        viewBinding.btnTestLogout.setOnClickListener {
            testLogout()
        }

        viewBinding.btnAddEntry.setOnClickListener {
            showAddEntryDialog()
        }

        viewBinding.btnCart.setOnClickListener {
            navigateToCart()
        }

        viewBinding.btnCheckHistory.setOnClickListener {
            navigateToHistory()
        }

        viewBinding.btnProfile.setOnClickListener {
            navigateToProfile()
        }

        // Load entries from Firestore
        loadEntries()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the Firestore listener when activity is destroyed
        listenerRegistration?.remove()
    }

    private fun loadEntries() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            listenerRegistration = firestore.collection("users").document(userId)
                .collection("currentList")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        data.clear()
                        for (document in snapshot.documents) {
                            val itemName = document.getString("itemName") ?: ""
                            val quantity = document.getLong("quantity")?.toInt() ?: 0
                            val imageUri = document.getString("imageUri")
                            val entry = EntryModel(
                                documentId = document.id,
                                productPicture = imageUri,
                                productQuantity = quantity,
                                productName = itemName
                            )
                            data.add(entry)
                        }
                        listAdapter.notifyDataSetChanged()
                    }
                }
        }
    }

    private fun showAddEntryDialog() {
        val dialog = AddEntryDialogFragment()
        dialog.setAddEntryListener(this)
        dialog.show(supportFragmentManager, "AddEntryDialog")
    }

    override fun onAddEntry(itemName: String, quantity: Int, imageUri: Uri?) {
        val imageUriString = imageUri?.toString()
        Toast.makeText(this, "Added: $itemName, Quantity: $quantity, Image URI: $imageUriString", Toast.LENGTH_SHORT).show()

        // Save the new entry to Firestore
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val entryId = firestore.collection("users").document(userId).collection("currentList").document().id
            val entry = hashMapOf(
                "itemName" to itemName,
                "quantity" to quantity,
                "imageUri" to imageUriString
            )
            firestore.collection("users").document(userId).collection("currentList").document(entryId).set(entry)
                .addOnSuccessListener {
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add entry: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun editButton(position: Int): SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            this,
            R.drawable.edit,
            14.0f,
            android.R.color.holo_orange_light,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    val entryId = data[position].documentId
                    Log.d("ListActivity", "Edit button clicked for entry ID: $entryId")

                    // Create a new instance of the dialog fragment
                    val editEntryDialogFragment = EditEntryDialogFragment.newInstance(entryId)

                    // Show the dialog fragment
                    editEntryDialogFragment.show(supportFragmentManager, "EditEntryDialogFragment")
                }
            }
        )
    }

    private fun deleteButton(position: Int): SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            this,
            R.drawable.delete,
            14.0f,
            android.R.color.holo_red_light,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    showDeleteConfirmationDialog(position)
                }
            }
        )
    }

    private fun showDeleteConfirmationDialog(position: Int) {
        val itemName = data[position].productName
        val entryId = data[position].documentId

        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete $itemName?")
            .setPositiveButton("Yes") { _, _ ->
                // Remove the item from the local list and notify the adapter
                data.removeAt(position)
                listAdapter.notifyItemRemoved(position)

                Toast.makeText(this@ListActivity, "$itemName deleted", Toast.LENGTH_SHORT).show()

                deleteItemFromFirestore(entryId)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteItemFromFirestore(entryId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val documentRef = firestore.collection("users")
                .document(userId)
                .collection("currentList")
                .document(entryId)

            // Deletes both the file and the entru in the DB
            documentRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val imageUri = document.getString("imageUri")
                    if (!imageUri.isNullOrEmpty()) {
                        val imageRef = storage.getReferenceFromUrl(imageUri)
                        imageRef.delete().addOnSuccessListener {
                            Log.d("ListActivity", "Image file deleted successfully from Firebase Storage")
                        }.addOnFailureListener { e ->
                            Log.w("ListActivity", "Error deleting image file from Firebase Storage", e)
                        }
                    }

                    documentRef.delete()
                        .addOnSuccessListener {
                            Log.d("ListActivity", "DocumentSnapshot successfully deleted!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("ListActivity", "Error deleting document", e)
                            Toast.makeText(this, "Failed to delete item from Firebase", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Log.d("ListActivity", "Document does not exist")
                }
            }.addOnFailureListener { e ->
                Log.w("ListActivity", "Error getting document", e)
            }
        }
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToCart() {
        val intent = Intent(this, CartActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToHistory() {
        val intent = Intent(this, HistoryListActivity::class.java)
        startActivity(intent)
    }

    private fun testLogout() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Clear "Remember Me" setting
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("RememberMe")
        editor.apply()

        // Navigate back to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
