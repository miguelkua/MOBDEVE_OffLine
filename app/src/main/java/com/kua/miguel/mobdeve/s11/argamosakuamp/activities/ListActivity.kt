package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.kua.miguel.mobdeve.s11.argamosakuamp.R
import com.kua.miguel.mobdeve.s11.argamosakuamp.adapters.ListAdapter
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityListBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class ListActivity : AppCompatActivity() {

    private lateinit var data: ArrayList<EntryModel>
    private lateinit var recyclerView: RecyclerView
    private lateinit var listAdapter: ListAdapter
    private lateinit var viewBinding: ActivityListBinding
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityListBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        recyclerView = viewBinding.recyclerViewItems
        recyclerView.layoutManager = LinearLayoutManager(this)

        data = arrayListOf(
            EntryModel(R.drawable.ic_launcher_background, 10, "Item 1"),
            EntryModel(R.drawable.ic_launcher_background, 5, "Item 2"),
            EntryModel(R.drawable.ic_launcher_background, 20, "Item 3")
        )

        listAdapter = ListAdapter(data)
        recyclerView.adapter = listAdapter

        val btnTestLogout: Button = viewBinding.root.findViewById(R.id.btnTestLogout)
        btnTestLogout.setOnClickListener {
            testLogout()
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
}
