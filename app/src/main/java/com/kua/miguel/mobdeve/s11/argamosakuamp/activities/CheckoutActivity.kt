package com.kua.miguel.mobdeve.s11.argamosakuamp.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.kua.miguel.mobdeve.s11.argamosakuamp.adapters.CheckoutAdapter
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ActivityCheckoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CheckoutActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCheckoutBinding
    private lateinit var checkoutAdapter: CheckoutAdapter
    private val checkoutItems = ArrayList<EntryModel>()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Retrieve data directly from Firestore
        loadCheckoutItems()

        // Setup RecyclerView
        checkoutAdapter = CheckoutAdapter(checkoutItems)
        viewBinding.recyclerViewCheckout.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerViewCheckout.adapter = checkoutAdapter

        // Finish Shopping button click listener
        viewBinding.btnFinishShopping.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun loadCheckoutItems() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .collection("currentList")
                .get()
                .addOnSuccessListener { snapshot ->
                    checkoutItems.clear()
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
                        checkoutItems.add(entry)
                    }
                    checkoutAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Checkout")
        builder.setMessage("Finishing shopping will clear your current cart. Do you wish to continue?")
        builder.setPositiveButton("Yes") { dialog, _ ->
            saveToPastLists()
            clearCurrentList()
            dialog.dismiss()
            // Navigate back to ListActivity
            navigateToListActivity()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun saveToPastLists() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            val pastListData = hashMapOf(
                "date" to currentDate,
                "items" to checkoutItems.map { item ->
                    hashMapOf(
                        "itemName" to item.productName,
                        "quantity" to item.productQuantity,
                        "imageUri" to item.productPicture
                    )
                }
            )

            firestore.collection("users").document(userId)
                .collection("pastLists")
                .add(pastListData)
                .addOnSuccessListener {
                    Toast.makeText(this, "List Saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearCurrentList() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .collection("currentList")
                .get()
                .addOnSuccessListener { snapshot ->
                    val batch = firestore.batch()
                    for (document in snapshot.documents) {
                        batch.delete(document.reference)
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            // Optionally show a toast or log the success
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun navigateToListActivity() {
        val intent = Intent(this, ListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
