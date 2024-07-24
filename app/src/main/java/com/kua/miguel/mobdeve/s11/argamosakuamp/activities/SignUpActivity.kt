package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivitySignupBinding

class SignUpActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

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
                    navigateBackToLogin()
                } else {
                    // If sign up fails, display a message to the user
                    Toast.makeText(this, "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
