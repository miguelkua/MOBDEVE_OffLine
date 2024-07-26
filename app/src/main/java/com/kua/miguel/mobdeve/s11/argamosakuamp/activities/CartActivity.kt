package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kua.miguel.mobdeve.s11.argamosakuamp.adapters.CartAdapter
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityCartBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class CartActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCartBinding
    private val cartItems = ArrayList<EntryModel>()
    private lateinit var cartAdapter: CartAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Setup RecyclerView
        cartAdapter = CartAdapter(cartItems)
        viewBinding.recyclerViewCart.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerViewCart.adapter = cartAdapter

        // Load data from Firestore
        loadCartItems()

        // Checkout button click listener
        viewBinding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "You can't checkout with no items", Toast.LENGTH_SHORT).show()
            } else {
                showCheckoutConfirmationDialog()
            }
        }

        viewBinding.btnList.setOnClickListener {
            navigateToList()
        }

        viewBinding.btnCheckHistory.setOnClickListener {
            navigateToHistory()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove() // Clean up listener when activity is destroyed
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            listenerRegistration = firestore.collection("users").document(userId)
                .collection("currentList")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        cartItems.clear()
                        for (document in snapshot.documents) {
                            val itemName = document.getString("itemName") ?: ""
                            val quantity = document.getLong("quantity")?.toInt() ?: 0
                            val imageUri = document.getString("imageUri")
                            val entry = EntryModel(
                                documentId = document.id,
                                productPicture = imageUri,
                                productQuantity = quantity,
                                productName = itemName
                            )
                            cartItems.add(entry)
                        }
                        cartAdapter.notifyDataSetChanged()
                        updateTotalItems()
                    }
                }
        }
    }

    private fun updateTotalItems() {
        val totalQuantity = cartItems.sumOf { it.productQuantity ?: 0 }
        viewBinding.llTotalItems.text = "_________________________________\n\nTotal Items: $totalQuantity"
    }

    private fun navigateToList() {
        val intent = Intent(this, ListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun navigateToHistory() {
        val intent = Intent(this, HistoryListActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToCheckout() {
        val intent = Intent(this, CheckoutActivity::class.java)
        startActivity(intent)
    }

    private fun showCheckoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Proceed to Checkout")
            .setMessage("Do you want to proceed?")
            .setPositiveButton("Yes") { dialog, _ ->
                navigateToCheckout()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

}
