package com.example.bit240172_foodmanagement

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "BIT240172_FoodDB.db", null, 1) {

    companion object {
        // QUY ĐỊNH BẮT BUỘC: Tiền tố BIT240172_ cho toàn bộ Bảng và Trường dữ liệu
        const val TABLE_CAT = "BIT240172_DishCategory"
        const val COL_CAT_ID = "BIT240172_Id"
        const val COL_CAT_NAME = "BIT240172_Name"
        const val COL_CAT_DESC = "BIT240172_Description"

        const val TABLE_DISH = "BIT240172_Dish"
        const val COL_DISH_ID = "BIT240172_Id"
        const val COL_DISH_NAME = "BIT240172_Name"
        const val COL_DISH_PRICE = "BIT240172_Price"
        const val COL_DISH_TIME = "BIT240172_PreparationTime"
        const val COL_DISH_AVAIL = "BIT240172_IsAvailable"
        const val COL_DISH_DESC = "BIT240172_Description"
        const val COL_DISH_CAT_ID = "BIT240172_DishCategoryId"
        const val COL_DISH_IMAGES = "BIT240172_DishImage" // Lưu JSON danh sách {ImageUrl, IsThumbnail}
        const val COL_DISH_SYNC = "BIT240172_IsSynced"    // Phục vụ yêu cầu nâng cao Offline Sync

        // Link MockAPI (Bạn có thể thay link MockAPI thật của bạn vào đây nếu có)
        const val MOCK_API_URL = "https://661000000000000000000000.mockapi.io/api/v1/BIT240172_Dish"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CAT (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NAME TEXT NOT NULL,
                $COL_CAT_DESC TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_DISH (
                $COL_DISH_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DISH_NAME TEXT NOT NULL,
                $COL_DISH_PRICE REAL NOT NULL,
                $COL_DISH_TIME INTEGER NOT NULL,
                $COL_DISH_AVAIL INTEGER NOT NULL,
                $COL_DISH_DESC TEXT,
                $COL_DISH_CAT_ID INTEGER NOT NULL,
                $COL_DISH_IMAGES TEXT,
                $COL_DISH_SYNC INTEGER DEFAULT 1
            )
        """)

        // Khởi tạo dữ liệu tối thiểu: 03 loại món ăn và 05 món ăn
        seedInitialData(db)
    }

    fun addCategory(name: String, desc: String): Long {
        val cv = ContentValues().apply {
            put(COL_CAT_NAME, name.trim())
            put(COL_CAT_DESC, desc.trim())
        }
        return writableDatabase.insert(TABLE_CAT, null, cv)
    }

    private fun seedInitialData(db: SQLiteDatabase) {
        val cats = listOf(
            Pair("Main Course", "Traditional main dishes"),
            Pair("Fast Food", "Quick and tasty snacks"),
            Pair("Beverages", "Refreshing drinks and coffee")
        )
        val catIds = cats.map {
            val cv = ContentValues().apply {
                put(COL_CAT_NAME, it.first)
                put(COL_CAT_DESC, it.second)
            }
            db.insert(TABLE_CAT, null, cv).toInt()
        }

        val sampleImg1 = encodeImages(listOf(
            DishImage("https://picsum.photos/seed/pho/400/300", true),
            DishImage("https://picsum.photos/seed/pho2/400/300", false)
        ))
        val sampleImg2 = encodeImages(listOf(DishImage("https://picsum.photos/seed/food/400/300", true)))

        val dishes = listOf(
            Dish(0, "Beef Pho", 45000.0, 20, true, "Traditional Hanoi Beef Noodle Soup", catIds[0], decodeImages(sampleImg1)),
            Dish(0, "Broken Rice (Com Tam)", 40000.0, 25, true, "Grilled pork chop with rice", catIds[0], decodeImages(sampleImg2)),
            Dish(0, "Crispy Banh Mi", 25000.0, 10, true, "Vietnamese baguette with pate and meat", catIds[1], decodeImages(sampleImg2)),
            Dish(0, "French Fries", 30000.0, 12, false, "Crispy salted potato fries (Stopped selling)", catIds[1], decodeImages(sampleImg2)),
            Dish(0, "Iced Milk Coffee", 29000.0, 5, true, "Strong Vietnamese Robusta coffee", catIds[2], decodeImages(sampleImg2))
        )
        dishes.forEach { d ->
            val cv = ContentValues().apply {
                put(COL_DISH_NAME, d.name)
                put(COL_DISH_PRICE, d.price)
                put(COL_DISH_TIME, d.preparationTime)
                put(COL_DISH_AVAIL, if (d.isAvailable) 1 else 0)
                put(COL_DISH_DESC, d.description)
                put(COL_DISH_CAT_ID, d.dishCategoryId)
                put(COL_DISH_IMAGES, encodeImages(d.dishImages))
                put(COL_DISH_SYNC, 1)
            }
            db.insert(TABLE_DISH, null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DISH")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CAT")
        onCreate(db)
    }

    // Chuyển đổi danh sách ảnh <-> JSON String
    private fun encodeImages(list: List<DishImage>): String {
        val arr = JSONArray()
        // Đảm bảo tối đa 01 ảnh có IsThumbnail = true
        var hasThumb = false
        list.forEach { img ->
            val isThumb = if (img.isThumbnail && !hasThumb) { hasThumb = true; true } else false
            arr.put(JSONObject().apply {
                put("ImageUrl", img.imageUrl)
                put("IsThumbnail", isThumb)
            })
        }
        return arr.toString()
    }

    private fun decodeImages(json: String?): List<DishImage> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<DishImage>()
        return try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(DishImage(obj.optString("ImageUrl", ""), obj.optBoolean("IsThumbnail", false)))
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    // --- QUẢN LÝ LOẠI MÓN ĂN ---
    fun getAllCategories(): List<DishCategory> {
        val list = mutableListOf<DishCategory>()
        val c = readableDatabase.rawQuery("SELECT * FROM $TABLE_CAT", null)
        while (c.moveToNext()) {
            list.add(DishCategory(
                c.getInt(c.getColumnIndexOrThrow(COL_CAT_ID)),
                c.getString(c.getColumnIndexOrThrow(COL_CAT_NAME)),
                c.getString(c.getColumnIndexOrThrow(COL_CAT_DESC))
            ))
        }
        c.close()
        return list
    }

    // Ràng buộc: Không cho xóa loại món đang được sử dụng
    fun deleteCategory(catId: Int): Boolean {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_DISH WHERE $COL_DISH_CAT_ID = ?", arrayOf(catId.toString()))
        c.moveToFirst()
        val count = c.getInt(0)
        c.close()
        if (count > 0) return false
        writableDatabase.delete(TABLE_CAT, "$COL_CAT_ID = ?", arrayOf(catId.toString()))
        return true
    }

    // --- QUẢN LÝ MÓN ĂN & VALIDATION ---
    fun getAllDishes(): List<Dish> {
        val list = mutableListOf<Dish>()
        val c = readableDatabase.rawQuery("SELECT * FROM $TABLE_DISH", null)
        while (c.moveToNext()) {
            list.add(Dish(
                id = c.getInt(c.getColumnIndexOrThrow(COL_DISH_ID)),
                name = c.getString(c.getColumnIndexOrThrow(COL_DISH_NAME)),
                price = c.getDouble(c.getColumnIndexOrThrow(COL_DISH_PRICE)),
                preparationTime = c.getInt(c.getColumnIndexOrThrow(COL_DISH_TIME)),
                isAvailable = c.getInt(c.getColumnIndexOrThrow(COL_DISH_AVAIL)) == 1,
                description = c.getString(c.getColumnIndexOrThrow(COL_DISH_DESC)) ?: "",
                dishCategoryId = c.getInt(c.getColumnIndexOrThrow(COL_DISH_CAT_ID)),
                dishImages = decodeImages(c.getString(c.getColumnIndexOrThrow(COL_DISH_IMAGES))),
                isSynced = c.getInt(c.getColumnIndexOrThrow(COL_DISH_SYNC))
            ))
        }
        c.close()
        return list
    }

    // Kiểm tra trùng tên món trong cùng một loại
    fun isDuplicateDishName(name: String, catId: Int, excludeDishId: Int = 0): Boolean {
        val c = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_DISH WHERE LOWER($COL_DISH_NAME) = LOWER(?) AND $COL_DISH_CAT_ID = ? AND $COL_DISH_ID != ?",
            arrayOf(name.trim(), catId.toString(), excludeDishId.toString())
        )
        c.moveToFirst()
        val count = c.getInt(0)
        c.close()
        return count > 0
    }

    fun saveDish(dish: Dish, isOnline: Boolean): Long {
        val cv = ContentValues().apply {
            put(COL_DISH_NAME, dish.name.trim())
            put(COL_DISH_PRICE, dish.price)
            put(COL_DISH_TIME, dish.preparationTime)
            put(COL_DISH_AVAIL, if (dish.isAvailable) 1 else 0)
            put(COL_DISH_DESC, dish.description.trim())
            put(COL_DISH_CAT_ID, dish.dishCategoryId)
            put(COL_DISH_IMAGES, encodeImages(dish.dishImages))
            put(COL_DISH_SYNC, if (isOnline) 1 else 0) // Nếu Offline -> đánh dấu 0 để chờ đồng bộ
        }
        return if (dish.id == 0) {
            writableDatabase.insert(TABLE_DISH, null, cv)
        } else {
            writableDatabase.update(TABLE_DISH, cv, "$COL_DISH_ID = ?", arrayOf(dish.id.toString())).toLong()
        }
    }

    fun deleteDish(id: Int) {
        writableDatabase.delete(TABLE_DISH, "$COL_DISH_ID = ?", arrayOf(id.toString()))
    }

    // --- YÊU CẦU NÂNG CAO: TỰ ĐỘNG ĐỒNG BỘ LOCAL <-> MOCKAPI ---
    fun autoSyncWithMockApi(onComplete: (Int) -> Unit) {
        Thread {
            val unsynced = getAllDishes().filter { it.isSynced == 0 }
            if (unsynced.isEmpty()) {
                onComplete(0)
                return@Thread
            }

            var successCount = 0

            unsynced.forEach { dish ->
                var conn: HttpURLConnection? = null
                try {
                    // Nếu dish.id đã tồn tại trên mock server (hoặc quy ước sync), có thể phân nhánh POST hoặc PUT
                    // Để an toàn với MockAPI dạng đơn giản: gửi toàn bộ thông tin đối tượng
                    val url = URL(MOCK_API_URL)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        connectTimeout = 5000
                        readTimeout = 5000
                        doOutput = true
                    }

                    val payload = JSONObject().apply {
                        put("localId", dish.id)
                        put("BIT240172_Name", dish.name)
                        put("BIT240172_Price", dish.price)
                        put("BIT240172_PreparationTime", dish.preparationTime)
                        put("BIT240172_IsAvailable", dish.isAvailable)
                        put("BIT240172_Description", dish.description)
                        put("BIT240172_DishCategoryId", dish.dishCategoryId)
                        put("BIT240172_DishImage", encodeImages(dish.dishImages))
                    }

                    OutputStreamWriter(conn.outputStream, "UTF-8").use {
                        it.write(payload.toString())
                        it.flush()
                    }

                    val code = conn.responseCode
                    // CHỈ ĐÁNH DẤU LÀ SYNCED KHI SERVER THỰC SỰ TRẢ VỀ 200 HOẶC 201
                    if (code in 200..201) {
                        val cv = ContentValues().apply { put(COL_DISH_SYNC, 1) }
                        writableDatabase.update(TABLE_DISH, cv, "$COL_DISH_ID = ?", arrayOf(dish.id.toString()))
                        successCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace() // Không nuốt lỗi, giữ nguyên isSynced = 0 để lần sau thử lại
                } finally {
                    conn?.disconnect()
                }
            }
            onComplete(successCount)
        }.start()
    }
}