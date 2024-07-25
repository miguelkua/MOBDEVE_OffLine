package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kua.miguel.mobdeve.s11.argamosakuamp.R
import com.kua.miguel.mobdeve.s11.argamosakuamp.adapters.ListAdapter
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityListBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.dialogs.AddEntryDialogFragment
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

        viewBinding = ActivityListBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        recyclerView = viewBinding.recyclerViewItems
        recyclerView.layoutManager = LinearLayoutManager(this)

        data = arrayListOf()

        listAdapter = ListAdapter(data)
        recyclerView.adapter = listAdapter

        val btnTestLogout: Button = viewBinding.root.findViewById(R.id.btnTestLogout)
        btnTestLogout.setOnClickListener {
            testLogout()
        }

        val btnAddEntry: Button = viewBinding.root.findViewById(R.id.btnAddEntry)
        btnAddEntry.setOnClickListener {
            showAddEntryDialog()
        }

        // Load entries from Firestore
        loadEntries()
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
        // Handle the addition of the new entry here
        val imageUriString = imageUri?.toString() // Convert URI to String if needed
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
                    // Entry added successfully
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add entry: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the Firestore listener when activity is destroyed
        listenerRegistration?.remove()
    }
}

