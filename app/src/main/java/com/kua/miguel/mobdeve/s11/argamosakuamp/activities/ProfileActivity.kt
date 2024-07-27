package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kua.miguel.mobdeve.s11.argamosakuamp.R
import com.kua.miguel.mobdeve.s11.argamosakuamp.adapters.ListAdapter
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityProfileBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.dialogs.AddEntryDialogFragment
import com.kua.miguel.mobdeve.s11.argamosakuamp.dialogs.EditEntryDialogFragment
import com.kua.miguel.mobdeve.s11.argamosakuamp.helpers.SwipeHelper
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class ProfileActivity : AppCompatActivity() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        displayUsername()

        binding.btnCheckHistory.setOnClickListener {
            // Your navigation code here
        }

        binding.btnList.setOnClickListener {
            navigateToList()
        }

        binding.btnCart.setOnClickListener {
            navigateToCart()
        }

        binding.btnProfile.setOnClickListener {
            // Your navigation code here
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

    private fun displayUsername() {
        val user = auth.currentUser
        val docRef = firestore.collection("users").document(user!!.uid)
        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val username = document.id
                val profileNameTextView: TextView = findViewById(R.id.tvProfileName)
                profileNameTextView.text = username
            } else {
                Log.d("ProfileActivity", "No such document")
            }
        }.addOnFailureListener { exception ->
            Log.d("ProfileActivity", "get failed with ", exception)
        }
    }
}
