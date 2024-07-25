package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivitySignupBinding
import java.util.Locale

class SignUpActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        viewBinding.tvLoginLink.setOnClickListener {
            navigateBackToLogin()
        }

        viewBinding.btnSignUp.setOnClickListener {
            val email = viewBinding.etEmailSignUp.text.toString()
            val password = viewBinding.etPasswordSignUp.text.toString()
            val confirmPassword = viewBinding.etConfirmPasswordSignUp.text.toString()

            if (password == confirmPassword) {
                signUpUser(email, password)
            } else {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to navigate back to LoginActivity
    private fun navigateBackToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Function for registration
    private fun signUpUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success
                    Toast.makeText(this, "Sign-up successful", Toast.LENGTH_SHORT).show()
                    val user = auth.currentUser

                    // Get current date and format it
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val currentDate = sdf.format(System.currentTimeMillis())

                    // Create a new user document in Firestore
                    val userMap = hashMapOf(
                        "email" to email,
                        "createdAt" to currentDate
                    )
                    user?.let {
                        firestore.collection("users").document(it.uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                // Successfully created user document in Firestore
                                navigateBackToLogin()
                            }
                            .addOnFailureListener { e ->
                                // Handle the error
                                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    // If sign up fails, display a message to the user
                    Toast.makeText(this, "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
