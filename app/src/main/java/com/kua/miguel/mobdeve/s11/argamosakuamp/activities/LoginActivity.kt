package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityLoginBinding

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            // User is signed in
            // navigateToMainActivity() // Navigate to main activity or home screen
        } else {
            // User is signed out
            // No action needed here as we're showing the login screen
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewBinding: ActivityLoginBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        auth = FirebaseAuth.getInstance()

//        Log.d("MainActivity", "Binding initialized: ${viewBinding != null}")

        viewBinding.tvSignUpLink.setOnClickListener {
//            Log.d("MainActivity", "Sign Up link clicked")
            navigateToSignUpActivity()
        }

        viewBinding.btnLogin.setOnClickListener {
            loginUser(viewBinding.etEmail.text.toString(), viewBinding.etPassword.text.toString())
        }

//        viewBinding.btnLogin.setOnClickListener {
//            navigateToListActivity()
//        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun navigateToSignUpActivity() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    val user = auth.currentUser
                    // Navigate to next activity
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

//    private fun navigateToListActivity() {
//        val intent = Intent(this, ListActivity::class.java)
//        startActivity(intent)
//    }
}