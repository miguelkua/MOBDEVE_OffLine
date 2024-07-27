package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kua.miguel.mobdeve.s11.argamosakuamp.R
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fetch user profile from Firestore and set it to the EditTexts
        fetchUserProfile()

        binding.btnLogout.setOnClickListener {
            logoutUser()
        }

        binding.btnCheckHistory.setOnClickListener {
            navigateToHistory()
        }

        binding.btnList.setOnClickListener {
            navigateToList()
        }

        binding.btnCart.setOnClickListener {
            navigateToCart()
        }

        // Set up click listeners for EditImageButtons
        setupEditButtonListeners()

        // Set up text watchers for validation
        setupTextWatchers()
    }

    private fun fetchUserProfile() {
        val user = auth.currentUser
        user?.let {
            val docRef = firestore.collection("users").document(user.uid)
            docRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val email = document.getString("email")
                    val name = document.getString("name")
                    val birthday = document.getString("birthday")
                    val contact = document.getString("contact")

                    binding.etProfileEmail.setText(email ?: "")
                    binding.etProfileName.setText(name ?: "")
                    binding.etProfileBirthday.setText(birthday ?: "")
                    binding.etProfileContact.setText(contact ?: "")
                } else {
                    Log.d("ProfileActivity", "No such document")
                }
            }.addOnFailureListener { exception ->
                Log.d("ProfileActivity", "get failed with ", exception)
            }
        }
    }

    private fun setupEditButtonListeners() {
        binding.btnEditName.setOnClickListener { onEditButtonClicked("name") }
        binding.btnEditBirthday.setOnClickListener { onEditButtonClicked("birthday") }
        binding.btnEditEmail.setOnClickListener { onEditButtonClicked("email") }
        binding.btnEditContact.setOnClickListener { onEditButtonClicked("contact") }
    }

    private fun onEditButtonClicked(field: String) {
        val currentUser = auth.currentUser ?: return
        val docRef = firestore.collection("users").document(currentUser.uid)

        docRef.get().addOnSuccessListener { document ->
            val currentValue = document?.getString(field)
            val editText = when (field) {
                "name" -> binding.etProfileName
                "birthday" -> binding.etProfileBirthday
                "email" -> binding.etProfileEmail
                "contact" -> binding.etProfileContact
                else -> return@addOnSuccessListener
            }

            val newValue = editText.text.toString()

            if (newValue != currentValue) {
                showConfirmationDialog(field, newValue, currentValue)
            } else {
                Toast.makeText(this, "No changes to $field", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Log.d("ProfileActivity", "Failed to fetch document: ", exception)
        }
    }

    private fun showConfirmationDialog(field: String, newValue: String, currentValue: String?) {
        val message = "Are you sure you want to update your $field from \"$currentValue\" to \"$newValue\"?"
        AlertDialog.Builder(this)
            .setTitle("Confirm Update")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                updateProfileField(field, newValue)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateProfileField(field: String, newValue: String) {
        val currentUser = auth.currentUser ?: return
        val docRef = firestore.collection("users").document(currentUser.uid)

        docRef.update(field, newValue)
            .addOnSuccessListener {
                Toast.makeText(this, "$field updated successfully", Toast.LENGTH_SHORT).show()
                fetchUserProfile() // Refresh profile data
            }
            .addOnFailureListener { exception ->
                Log.d("ProfileActivity", "Update failed: ", exception)
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Clear "Remember Me" setting
        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("RememberMe")
        editor.apply()

        // Navigate back to LoginActivity and reset everything
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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

    private fun navigateToHistory() {
        val intent = Intent(this, HistoryListActivity::class.java)
        startActivity(intent)
    }

    private fun setupTextWatchers() {
        binding.etProfileName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && s.length == 35) {
                    Toast.makeText(this@ProfileActivity, "Name cannot exceed 35 characters", Toast.LENGTH_SHORT).show()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etProfileContact.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null && !s.toString().matches("\\d*".toRegex())) {
                    binding.etProfileContact.setText(s.toString().filter { it.isDigit() })
                    binding.etProfileContact.setSelection(binding.etProfileContact.text.length)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
}
