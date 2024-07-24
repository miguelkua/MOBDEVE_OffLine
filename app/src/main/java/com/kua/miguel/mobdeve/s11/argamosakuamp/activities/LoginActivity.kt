package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityLoginBinding

class LoginActivity : ComponentActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var viewBinding: ActivityLoginBinding

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            // User is signed in
            navigateToListActivity()
        } else {
            // User is signed out
            // No action needed here as we're showing the login screen
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

//        Log.d("MainActivity", "Binding initialized: ${viewBinding != null}")

        viewBinding.tvSignUpLink.setOnClickListener {
//            Log.d("MainActivity", "Sign Up link clicked")
            navigateToSignUpActivity()
        }

        viewBinding.btnLogin.setOnClickListener {
            loginUser(viewBinding.etEmail.text.toString(), viewBinding.etPassword.text.toString())
        }
    }

    override fun onStart() {
        super.onStart()

        val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val rememberMe = sharedPreferences.getBoolean("RememberMe", false)

        // Check if the user is currently signed in
        val currentUser = auth.currentUser
        if (currentUser != null && rememberMe) {
            // User is signed in and "Remember Me" is checked
            navigateToListActivity()
        } else if (currentUser != null && !rememberMe) {
            // User is signed in but "Remember Me" is not checked
            auth.signOut()
        }
    }


    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun navigateToSignUpActivity() {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToListActivity() {
        val intent = Intent(this, ListActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    val rememberMe = viewBinding.cbRememberMe.isChecked

                    // Save the "Remember Me" status
                    val sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("RememberMe", rememberMe)
                    editor.apply()

                    navigateToListActivity()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

}