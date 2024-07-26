package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kua.miguel.mobdeve.s11.argamosakuamp.adapters.HistoryEntryAdapter
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityHistoryEntryBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class HistoryEntryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyEntryAdapter: HistoryEntryAdapter
    private lateinit var viewBinding: ActivityHistoryEntryBinding
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var pastListId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityHistoryEntryBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        recyclerView = viewBinding.recyclerViewHistoryEntry
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Retrieve pastListId from Intent
        pastListId = intent.getStringExtra("pastListId") ?: ""
        if (pastListId.isNotEmpty()) {
            loadHistoryEntries()
        } else {
            Toast.makeText(this, "No past list ID provided", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadHistoryEntries() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .collection("pastLists").document(pastListId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val items = document.get("items") as? List<Map<String, Any>>
                        val entries = items?.map { item ->
                            EntryModel(
                                documentId = "",
                                productName = item["itemName"] as? String ?: "Unknown",
                                productQuantity = (item["quantity"] as? Number)?.toInt() ?: 0,
                                productPicture = item["imageUri"] as? String
                            )
                        } ?: emptyList()

                        historyEntryAdapter = HistoryEntryAdapter(entries)
                        recyclerView.adapter = historyEntryAdapter
                    } else {
                        Toast.makeText(this, "No document found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
