package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kua.miguel.mobdeve.s11.argamosakuamp.adapters.HistoryListAdapter
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityHistoryListBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.HistoryEntryModel

class HistoryListActivity : AppCompatActivity() {

    private lateinit var historyListAdapter: HistoryListAdapter
    private lateinit var viewBinding: ActivityHistoryListBinding
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityHistoryListBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)

        viewBinding.btnList.setOnClickListener {
            navigateToList()
        }

        viewBinding.btnCart.setOnClickListener {
            navigateToCart()
        }

        viewBinding.btnProfile.setOnClickListener {
            navigateToProfile()
        }

        loadHistoryEntries()
    }

    private fun loadHistoryEntries() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .collection("pastLists")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        val historyEntries = snapshot.documents.map { document ->
                            HistoryEntryModel(
                                pastListId = document.id,
                                dateTime = "${document.getString("date") ?: "Unknown Date"} | ${document.getString("time") ?: "Unknown Time"}"
                            )
                        }
                        historyListAdapter = HistoryListAdapter(historyEntries)
                        viewBinding.recyclerViewHistory.adapter = historyListAdapter

                        if (historyEntries.isEmpty()) {
                            viewBinding.tvEmptyHistory.visibility = android.view.View.VISIBLE
                            viewBinding.recyclerViewHistory.visibility = android.view.View.GONE
                        } else {
                            viewBinding.tvEmptyHistory.visibility = android.view.View.GONE
                            viewBinding.recyclerViewHistory.visibility = android.view.View.VISIBLE
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun navigateToList() {
        val intent = Intent(this, ListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToCart() {
        val intent = Intent(this, CartActivity::class.java)
        startActivity(intent)
        finish()
    }
}
