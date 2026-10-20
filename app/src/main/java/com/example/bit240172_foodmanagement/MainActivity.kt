package com.example.bit240172_foodmanagement

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    private lateinit var db: DatabaseHelper
    private lateinit var connectivityManager: ConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = DatabaseHelper(this)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setContent {
            MaterialTheme {
                // Trạng thái ngôn ngữ (Mặc định: false = English)
                var isVi by remember { mutableStateOf(false) }
                var isOnline by remember { mutableStateOf(checkInternet()) }

                // Dữ liệu từ SQLite
                var dishes by remember { mutableStateOf(db.getAllDishes()) }
                var categories by remember { mutableStateOf(db.getAllCategories()) }

                // Giữ trạng thái bộ lọc khi chuyển màn hình
                var searchQuery by remember { mutableStateOf("") }
                var selectedCatId by remember { mutableStateOf(0) }
                var onlyAvailable by remember { mutableStateOf(false) }
                var minPriceStr by remember { mutableStateOf("") }
                var maxPriceStr by remember { mutableStateOf("") }
                var sortOption by remember { mutableStateOf(0) }

                // Điều hướng màn hình: "list", "detail", "form"
                var currentScreen by remember { mutableStateOf("list") }
                var selectedDishId by remember { mutableStateOf(0) }
                var editingDish by remember { mutableStateOf<Dish?>(null) }
                var showCatDialog by remember { mutableStateOf(false) }

                // Lắng nghe kết nối Internet để tự động đồng bộ Offline -> MockAPI
                DisposableEffect(Unit) {
                    val callback = object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            isOnline = true
                            db.autoSyncWithMockApi { syncedCount ->
                                runOnUiThread {
                                    dishes = db.getAllDishes()
                                    if (syncedCount > 0) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            AppStrings.get("sync_done", isVi),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }

                        override fun onLost(network: Network) {
                            isOnline = false
                        }
                    }
                    connectivityManager.registerDefaultNetworkCallback(callback)
                    onDispose { connectivityManager.unregisterNetworkCallback(callback) }
                }

                when (currentScreen) {
                    "list" -> DishListScreen(
                        dishes = dishes,
                        categories = categories,
                        isVi = isVi,
                        isOnline = isOnline,
                        searchQuery = searchQuery, onSearchChange = { searchQuery = it },
                        selectedCatId = selectedCatId, onCatChange = { selectedCatId = it },
                        onlyAvailable = onlyAvailable, onAvailChange = { onlyAvailable = it },
                        minPriceStr = minPriceStr, onMinPriceChange = { minPriceStr = it },
                        maxPriceStr = maxPriceStr, onMaxPriceChange = { maxPriceStr = it },
                        sortOption = sortOption, onSortChange = { sortOption = it },
                        onToggleLang = { isVi = !isVi },
                        onAddClick = { editingDish = null; currentScreen = "form" },
                        onDishClick = { id -> selectedDishId = id; currentScreen = "detail" },
                        onManageCatClick = { showCatDialog = true }
                    )

                    "detail" -> {
                        val dish = dishes.find { it.id == selectedDishId }
                        val catName = categories.find { it.id == dish?.dishCategoryId }?.name ?: ""
                        DishDetailScreen(
                            dish = dish,
                            categoryName = catName,
                            isVi = isVi,
                            onBack = { currentScreen = "list" },
                            onEdit = { d -> editingDish = d; currentScreen = "form" },
                            onDelete = { id ->
                                db.deleteDish(id)
                                dishes = db.getAllDishes()
                                currentScreen = "list"
                            },
                            onSelectThumbnail = { d, index ->
                                val updatedImgs = d.dishImages.mapIndexed { i, img ->
                                    img.copy(isThumbnail = i == index)
                                }
                                db.saveDish(d.copy(dishImages = updatedImgs), isOnline)
                                dishes = db.getAllDishes()
                            }
                        )
                    }

                    "form" -> DishFormScreen(
                        initialDish = editingDish,
                        categories = categories,
                        isVi = isVi,
                        db = db,
                        onBack = { currentScreen = if (editingDish == null) "list" else "detail" },
                        onSave = { newDish ->
                            db.saveDish(newDish, isOnline)
                            dishes = db.getAllDishes()
                            if (!isOnline) {
                                Toast.makeText(this, AppStrings.get("sync_offline", isVi), Toast.LENGTH_SHORT).show()
                            }
                            currentScreen = "list"
                        }
                    )
                }

                if (showCatDialog) {
                    CategoryDialog(
                        categories = categories,
                        isVi = isVi,
                        onDismiss = { showCatDialog = false },
                        onAddCategory = { catName, desc ->
                            db.addCategory(catName, desc)
                            categories = db.getAllCategories()
                        },
                        onDeleteCategory = { catId ->
                            val deleted = db.deleteCategory(catId)
                            if (deleted) categories = db.getAllCategories()
                            deleted
                        }
                    )
                }
            }
        }
    }

    private fun checkInternet(): Boolean {
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}