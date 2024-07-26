package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kua.miguel.mobdeve.s11.argamosakuamp.adapters.HistoryListAdapter
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityHistoryListBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.HistoryEntryModel

class HistoryListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyListAdapter: HistoryListAdapter
    private lateinit var viewBinding: ActivityHistoryListBinding
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityHistoryListBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        recyclerView = viewBinding.recyclerViewHistory
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewBinding.btnList.setOnClickListener {
            navigateToList()
        }

        viewBinding.btnCart.setOnClickListener {
            navigateToCart()
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
                                date = document.getString("date") ?: "Unknown Date"
                            )
                        }
                        historyListAdapter = HistoryListAdapter(historyEntries)
                        recyclerView.adapter = historyListAdapter
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

    private fun navigateToCart() {
        val intent = Intent(this, CartActivity::class.java)
        startActivity(intent)
        finish()
    }
}
