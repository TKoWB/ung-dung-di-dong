package com.example.bit240172_foodmanagement

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// ================= 1. MÀN HÌNH DANH SÁCH, TÌM KIẾM, LỌC & SẮP XẾP =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishListScreen(
    dishes: List<Dish>,
    categories: List<DishCategory>,
    isVi: Boolean,
    isOnline: Boolean,
    searchQuery: String, onSearchChange: (String) -> Unit,
    selectedCatId: Int, onCatChange: (Int) -> Unit,
    onlyAvailable: Boolean, onAvailChange: (Boolean) -> Unit,
    minPriceStr: String, onMinPriceChange: (String) -> Unit,
    maxPriceStr: String, onMaxPriceChange: (String) -> Unit,
    sortOption: Int, onSortChange: (Int) -> Unit,
    onToggleLang: () -> Unit,
    onAddClick: () -> Unit,
    onDishClick: (Int) -> Unit,
    onManageCatClick: () -> Unit
) {
    val t = { k: String -> AppStrings.get(k, isVi) }
    val minP = minPriceStr.toDoubleOrNull()
    val maxP = maxPriceStr.toDoubleOrNull()
    val isPriceRangeInvalid = minP != null && maxP != null && minP > maxP

    // Phối hợp đồng thời các điều kiện lọc và sắp xếp
    val filteredDishes = remember(dishes, searchQuery, selectedCatId, onlyAvailable, minP, maxP, sortOption) {
        dishes.filter { d ->
            val matchName = d.name.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCatId == 0 || d.dishCategoryId == selectedCatId
            val matchAvail = !onlyAvailable || d.isAvailable
            val matchMin = minP == null || d.price >= minP
            val matchMax = maxP == null || d.price <= maxP
            matchName && matchCat && matchAvail && matchMin && matchMax
        }.let { list ->
            when (sortOption) {
                1 -> list.sortedBy { it.price }
                2 -> list.sortedByDescending { it.price }
                3 -> list.sortedBy { it.preparationTime }
                else -> list
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(t("app_title"), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (isOnline) "Online" else "Offline Mode",
                            fontSize = 11.sp,
                            color = if (isOnline) Color(0xFF2E7D32) else Color.Red
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onManageCatClick) { Text(t("categories")) }
                    Button(onClick = onToggleLang, modifier = Modifier.padding(end = 8.dp)) {
                        Text(if (isVi) "VI" else "EN")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = t("add_dish"))
            }
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).padding(10.dp).fillMaxSize()) {
            // Ô tìm kiếm
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                label = { Text(t("search")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(6.dp))

            // Bộ lọc Loại món ăn + Trạng thái đang bán
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                var catMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { catMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        val catName = categories.find { it.id == selectedCatId }?.name ?: t("all_cat")
                        Text(catName, maxLines = 1)
                    }
                    DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                        DropdownMenuItem(text = { Text(t("all_cat")) }, onClick = { onCatChange(0); catMenu = false })
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { onCatChange(c.id); catMenu = false })
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = onlyAvailable, onCheckedChange = onAvailChange)
                    Text(t("only_avail"), fontSize = 13.sp)
                }
            }

            // Lọc khoảng giá & Sắp xếp
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = minPriceStr, onValueChange = onMinPriceChange,
                    label = { Text(t("min_price"), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
                Spacer(Modifier.width(6.dp))
                OutlinedTextField(
                    value = maxPriceStr, onValueChange = onMaxPriceChange,
                    label = { Text(t("max_price"), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
                Spacer(Modifier.width(6.dp))
                var sortMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1.2f)) {
                    OutlinedButton(onClick = { sortMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        val sLabel = when (sortOption) {
                            1 -> t("sort_price_asc")
                            2 -> t("sort_price_desc")
                            3 -> t("sort_time_asc")
                            else -> t("sort_none")
                        }
                        Text(sLabel, fontSize = 11.sp, maxLines = 1)
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        DropdownMenuItem(text = { Text(t("sort_none")) }, onClick = { onSortChange(0); sortMenu = false })
                        DropdownMenuItem(text = { Text(t("sort_price_asc")) }, onClick = { onSortChange(1); sortMenu = false })
                        DropdownMenuItem(text = { Text(t("sort_price_desc")) }, onClick = { onSortChange(2); sortMenu = false })
                        DropdownMenuItem(text = { Text(t("sort_time_asc")) }, onClick = { onSortChange(3); sortMenu = false })
                    }
                }
            }

            if (isPriceRangeInvalid) {
                Text(t("err_price_range"), color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(8.dp))

            // Danh sách món ăn
            if (filteredDishes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(t("empty_list"), color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredDishes) { dish ->
                        val thumbUrl = dish.dishImages.find { it.isThumbnail }?.imageUrl
                            ?: dish.dishImages.firstOrNull()?.imageUrl ?: ""
                        val catName = categories.find { it.id == dish.dishCategoryId }?.name ?: ""
                        // Hiển thị Quick / Nhanh khi PreparationTime <= 15 phút
                        val timeBadge = if (dish.preparationTime <= 15) t("quick") else t("regular")

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onDishClick(dish.id) },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = thumbUrl,
                                    contentDescription = dish.name,
                                    modifier = Modifier.size(72.dp).background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        if (dish.isSynced == 0) {
                                            Text(" [Offline]", color = Color.Red, fontSize = 11.sp)
                                        }
                                    }
                                    Text("$catName • ${dish.price} VNĐ", color = Color(0xFF1565C0), fontSize = 14.sp)
                                    Text("${dish.preparationTime} mins ($timeBadge)", fontSize = 13.sp)
                                    Text(
                                        if (dish.isAvailable) t("avail") else t("unavail"),
                                        color = if (dish.isAvailable) Color(0xFF2E7D32) else Color.Gray,
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= 2. MÀN HÌNH CHI TIẾT MÓN ĂN, ĐỔI ẢNH ĐẠI DIỆN & XÓA =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreen(
    dish: Dish?,
    categoryName: String,
    isVi: Boolean,
    onBack: () -> Unit,
    onEdit: (Dish) -> Unit,
    onDelete: (Int) -> Unit,
    onSelectThumbnail: (Dish, Int) -> Unit
) {
    val t = { k: String -> AppStrings.get(k, isVi) }
    var showDelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dish?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    if (dish != null) {
                        IconButton(onClick = { onEdit(dish) }) { Icon(Icons.Default.Edit, null) }
                        IconButton(onClick = { showDelDialog = true }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    }
                }
            )
        }
    ) { pad ->
        // Xử lý lỗi khi Id không tồn tại
        if (dish == null) {
            Box(modifier = Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text(t("err_not_found"), color = Color.Red)
            }
            return@Scaffold
        }

        val timeBadge = if (dish.preparationTime <= 15) t("quick") else t("regular")

        Column(modifier = Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("${t("cat")}: $categoryName", fontSize = 15.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text("${dish.price} VNĐ", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
            Spacer(Modifier.height(4.dp))
            Text("⏱ ${dish.preparationTime} mins — $timeBadge", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (dish.isAvailable) t("avail") else t("unavail"),
                color = if (dish.isAvailable) Color(0xFF2E7D32) else Color.Red,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(dish.description)
            Spacer(Modifier.height(16.dp))

            // Hiển thị nhiều ảnh & cho phép click chọn đổi ảnh đại diện (tối đa 01 IsThumbnail = true)
            Text("${t("set_thumb")} (Tap image to set):", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (dish.dishImages.isEmpty()) {
                Text(t("no_images"), color = Color.Gray)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(dish.dishImages.size) { index ->
                        val img = dish.dishImages[index]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .border(if (img.isThumbnail) 3.dp else 1.dp, if (img.isThumbnail) Color(0xFF2E7D32) else Color.Gray)
                                .clickable { onSelectThumbnail(dish, index) }
                                .padding(4.dp)
                        ) {
                            AsyncImage(
                                model = img.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(130.dp).background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                            if (img.isThumbnail) {
                                Text("★ ${t("set_thumb")}", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showDelDialog) {
            AlertDialog(
                onDismissRequest = { showDelDialog = false },
                title = { Text(t("delete")) },
                text = { Text(t("confirm_del")) },
                confirmButton = {
                    Button(
                        onClick = { showDelDialog = false; onDelete(dish.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text(t("delete")) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDelDialog = false }) { Text(t("cancel")) }
                }
            )
        }
    }
}

// ================= 3. MÀN HÌNH THÊM / SỬA MÓN ĂN & VALIDATION =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishFormScreen(
    initialDish: Dish?,
    categories: List<DishCategory>,
    isVi: Boolean,
    db: DatabaseHelper,
    onBack: () -> Unit,
    onSave: (Dish) -> Unit
) {
    val t = { k: String -> AppStrings.get(k, isVi) }
    var name by remember { mutableStateOf(initialDish?.name ?: "") }
    var priceStr by remember { mutableStateOf(initialDish?.price?.toString() ?: "") }
    var timeStr by remember { mutableStateOf(initialDish?.preparationTime?.toString() ?: "") }
    var desc by remember { mutableStateOf(initialDish?.description ?: "") }
    var isAvail by remember { mutableStateOf(initialDish?.isAvailable ?: true) }
    var catId by remember { mutableStateOf(initialDish?.dishCategoryId ?: (categories.firstOrNull()?.id ?: 0)) }
    var images by remember { mutableStateOf(initialDish?.dishImages ?: emptyList()) }
    var newImgUrl by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialDish == null) t("add_dish") else t("edit_dish")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(t("name")) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = priceStr, onValueChange = { priceStr = it }, label = { Text(t("price")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = timeStr, onValueChange = { timeStr = it }, label = { Text(t("time")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Chọn loại món ăn từ dữ liệu lưu trữ
            var catMenu by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { catMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    val catName = categories.find { it.id == catId }?.name ?: t("cat")
                    Text("${t("cat")}: $catName")
                }
                DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                    categories.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = { catId = c.id; catMenu = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isAvail, onCheckedChange = { isAvail = it })
                Text(t("avail"))
            }

            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(t("desc")) }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            // Quản lý thêm nhiều ImageUrl và chọn 1 ảnh đại diện
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newImgUrl, onValueChange = { newImgUrl = it },
                    label = { Text(t("img_url")) }, modifier = Modifier.weight(1f), singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (newImgUrl.isNotBlank()) {
                        val isFirst = images.isEmpty()
                        images = images + DishImage(newImgUrl.trim(), isFirst)
                        newImgUrl = ""
                    }
                }) { Text("+") }
            }

            Spacer(Modifier.height(8.dp))
            images.forEachIndexed { idx, img ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    RadioButton(
                        selected = img.isThumbnail,
                        onClick = {
                            images = images.mapIndexed { i, item -> item.copy(isThumbnail = i == idx) }
                        }
                    )
                    Text(img.imageUrl, modifier = Modifier.weight(1f), maxLines = 1, fontSize = 12.sp)
                    IconButton(onClick = {
                        val wasThumb = img.isThumbnail
                        val updated = images.filterIndexed { i, _ -> i != idx }
                        images = if (wasThumb && updated.isNotEmpty()) {
                            updated.mapIndexed { i, itm -> itm.copy(isThumbnail = i == 0) }
                        } else {
                            updated
                        }
                    }) {
                        Icon(Icons.Default.Close, null, tint = Color.Red)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val p = priceStr.toDoubleOrNull()
                    val tm = timeStr.toIntOrNull()
                    val validCat = categories.any { it.id == catId }

                    // Kiểm tra đầy đủ các điều kiện Validation theo đề bài
                    when {
                        name.isBlank() -> errorMsg = t("err_empty_name")
                        p == null || p <= 0 -> errorMsg = t("err_price")
                        tm == null || tm <= 0 -> errorMsg = t("err_time")
                        !validCat -> errorMsg = t("err_cat")
                        db.isDuplicateDishName(name, catId, initialDish?.id ?: 0) -> errorMsg = t("err_dup_name")
                        else -> {
                            onSave(
                                Dish(
                                    id = initialDish?.id ?: 0,
                                    name = name,
                                    price = p,
                                    preparationTime = tm,
                                    isAvailable = isAvail,
                                    description = desc,
                                    dishCategoryId = catId,
                                    dishImages = images
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(t("save"))
            }
        }
    }
}

// ================= 4. HỘP THOẠI QUẢN LÝ LOẠI MÓN =================
@Composable
fun CategoryDialog(
    categories: List<DishCategory>,
    isVi: Boolean,
    onDismiss: () -> Unit,
    onAddCategory: (String, String) -> Unit,
    onDeleteCategory: (Int) -> Boolean
) {
    val context = LocalContext.current
    val t = { k: String -> AppStrings.get(k, isVi) }
    var newCatName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("categories")) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        placeholder = { Text(if (isVi) "Thêm loại mới..." else "New Category...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(6.dp))
                    Button(onClick = {
                        if (newCatName.isNotBlank()) {
                            onAddCategory(newCatName.trim(), "")
                            newCatName = ""
                        }
                    }) { Text("+") }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    items(categories) { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.Bold)
                                if (c.description.isNotEmpty()) {
                                    Text(c.description, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = {
                                val ok = onDeleteCategory(c.id)
                                if (!ok) {
                                    Toast.makeText(context, t("err_del_cat"), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}