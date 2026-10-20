package com.example.bit240172_foodmanagement

// 1. Các Model dữ liệu theo đúng yêu cầu
data class DishCategory(
    val id: Int = 0,
    val name: String,
    val description: String
)

data class DishImage(
    val imageUrl: String,
    val isThumbnail: Boolean
)

data class Dish(
    val id: Int = 0,
    val name: String,
    val price: Double,
    val preparationTime: Int,
    val isAvailable: Boolean,
    val description: String,
    val dishCategoryId: Int,
    val dishImages: List<DishImage>,
    val isSynced: Int = 1 // 0: Lưu offline chờ đồng bộ, 1: Đã đồng bộ MockAPI
)

// 2. Bộ từ điển Đa ngôn ngữ (English mặc định <-> Tiếng Việt)
object AppStrings {
    fun get(key: String, isVi: Boolean): String {
        val en = mapOf(
            "app_title" to "BIT240172 - Food Manager",
            "search" to "Search dish name...",
            "all_cat" to "All Categories",
            "only_avail" to "Available Only",
            "min_price" to "Min Price",
            "max_price" to "Max Price",
            "sort_none" to "Sort: Default",
            "sort_price_asc" to "Price: Low -> High",
            "sort_price_desc" to "Price: High -> Low",
            "sort_time_asc" to "Prep Time: Fast -> Slow",
            "quick" to "Quick",
            "regular" to "Regular",
            "avail" to "Selling",
            "unavail" to "Stopped",
            "add_dish" to "Add Dish",
            "edit_dish" to "Edit Dish",
            "categories" to "Categories",
            "save" to "Save",
            "cancel" to "Cancel",
            "delete" to "Delete",
            "confirm_del" to "Are you sure you want to delete this dish?",
            "name" to "Dish Name",
            "price" to "Price (> 0)",
            "time" to "Prep Time in mins (> 0)",
            "desc" to "Description",
            "cat" to "Category",
            "img_url" to "Add Image URL",
            "set_thumb" to "Thumbnail",
            "err_empty_name" to "Error: Name cannot be empty!",
            "err_price" to "Error: Price must be > 0!",
            "err_time" to "Error: Preparation time must be > 0!",
            "err_cat" to "Error: Invalid category!",
            "err_dup_name" to "Error: Dish name already exists in this category!",
            "err_del_cat" to "Cannot delete: Category is currently in use by a dish!",
            "err_price_range" to "Invalid price range (Min > Max)!",
            "err_not_found" to "Error: Dish ID not found!",
            "empty_list" to "No dishes match your filter.",
            "no_images" to "No images available.",
            "sync_offline" to "Offline: Saved locally (Pending Sync)",
            "sync_done" to "Online: Auto-synced local changes to MockAPI!"
        )
        val vi = mapOf(
            "app_title" to "BIT240172 - Quản Lý Món Ăn",
            "search" to "Tìm tên món ăn...",
            "all_cat" to "Tất cả loại",
            "only_avail" to "Đang bán",
            "min_price" to "Giá từ",
            "max_price" to "Đến giá",
            "sort_none" to "Sắp xếp: Mặc định",
            "sort_price_asc" to "Giá: Tăng dần",
            "sort_price_desc" to "Giá: Giảm dần",
            "sort_time_asc" to "Thời gian: Tăng dần",
            "quick" to "Nhanh",
            "regular" to "Thông thường",
            "avail" to "Đang bán",
            "unavail" to "Ngừng bán",
            "add_dish" to "Thêm Món",
            "edit_dish" to "Sửa Món",
            "categories" to "Loại Món",
            "save" to "Lưu",
            "cancel" to "Hủy",
            "delete" to "Xóa",
            "confirm_del" to "Bạn có chắc chắn muốn xóa món ăn này?",
            "name" to "Tên món ăn",
            "price" to "Giá bán (> 0)",
            "time" to "Thời gian chế biến - phút (> 0)",
            "desc" to "Mô tả",
            "cat" to "Loại món ăn",
            "img_url" to "Thêm link ảnh (ImageUrl)",
            "set_thumb" to "Ảnh đại diện",
            "err_empty_name" to "Lỗi: Tên món không được để trống!",
            "err_price" to "Lỗi: Giá món ăn phải > 0!",
            "err_time" to "Lỗi: Thời gian chế biến phải > 0!",
            "err_cat" to "Lỗi: Loại món ăn không hợp lệ!",
            "err_dup_name" to "Lỗi: Tên món đã tồn tại trong cùng loại món này!",
            "err_del_cat" to "Không thể xóa: Loại món này đang có món ăn sử dụng!",
            "err_price_range" to "Khoảng giá lọc không hợp lệ (Giá từ > Đến giá)!",
            "err_not_found" to "Lỗi: Id món ăn không tồn tại!",
            "empty_list" to "Danh sách trống / Không có món phù hợp.",
            "no_images" to "Món này chưa có ảnh.",
            "sync_offline" to "Offline: Đã lưu vào máy (Chờ đồng bộ)",
            "sync_done" to "Có mạng: Đã tự động đồng bộ lên MockAPI!"
        )
        return if (isVi) vi[key] ?: key else en[key] ?: key
    }
}