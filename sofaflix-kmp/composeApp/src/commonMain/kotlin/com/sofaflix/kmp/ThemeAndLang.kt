package com.sofaflix.kmp

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

class AppColors {
    val background = Color(0xFF191B24)
    val surface = Color(0xFF20232D)
    val cardBg = Color.White.copy(alpha = 0.08f)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.6f)
    val border = Color.White.copy(alpha = 0.08f)
    val primary = Color(0xFF1CC749)
}

val LocalAppColors = staticCompositionLocalOf { AppColors() }
val LocalLanguage = staticCompositionLocalOf { "vi" }

object Lang {
    private val translations = mapOf(
        "home" to mapOf("vi" to "Trang chủ", "en" to "Home"),
        "search" to mapOf("vi" to "Tìm kiếm", "en" to "Search"),
        "library" to mapOf("vi" to "Thư viện", "en" to "Library"),
        "profile" to mapOf("vi" to "Hồ sơ", "en" to "Profile"),
        "latest" to mapOf("vi" to "Mới cập nhật", "en" to "New Releases"),
        "series" to mapOf("vi" to "Phim bộ", "en" to "TV Series"),
        "movies" to mapOf("vi" to "Phim lẻ", "en" to "Movies"),
        "animation" to mapOf("vi" to "Hoạt hình", "en" to "Animation"),
        "categories" to mapOf("vi" to "Thể loại", "en" to "Categories"),
        "see_all" to mapOf("vi" to "Xem tất cả", "en" to "See All"),
        "trending" to mapOf("vi" to "Đang hot", "en" to "Trending"),
        "explore_categories" to mapOf("vi" to "Khám phá theo thể loại", "en" to "Explore by Category"),
        "explore" to mapOf("vi" to "Khám phá", "en" to "Explore"),
        "view_details" to mapOf("vi" to "Xem chi tiết", "en" to "View Details"),
        "load_movies_error" to mapOf("vi" to "Đã xảy ra lỗi tải phim", "en" to "Failed to load movies"),
        "phim_moi" to mapOf("vi" to "Phim mới", "en" to "New Releases"),
        "saved" to mapOf("vi" to "Đã lưu", "en" to "Saved"),
        "history" to mapOf("vi" to "Lịch sử", "en" to "History"),
        "empty_saved" to mapOf("vi" to "Thư viện phim đã lưu trống.", "en" to "Your saved library is empty."),
        "empty_history" to mapOf("vi" to "Chưa có lịch sử xem phim.", "en" to "No watch history found."),
        "library" to mapOf("vi" to "Thư viện", "en" to "Library"),
        "unknown" to mapOf("vi" to "Chưa rõ", "en" to "Unknown"),
        "library_error" to mapOf("vi" to "Lỗi tải thư viện", "en" to "Failed to load library"),
        "settings" to mapOf("vi" to "Cài đặt", "en" to "Settings"),
        "theme" to mapOf("vi" to "Giao diện", "en" to "Theme"),
        "language" to mapOf("vi" to "Ngôn ngữ", "en" to "Language"),
        "notifications" to mapOf("vi" to "Thông báo", "en" to "Notifications"),
        "clear_cache" to mapOf("vi" to "Xóa bộ nhớ đệm", "en" to "Clear Cache"),
        "rate_app" to mapOf("vi" to "Đánh giá ứng dụng", "en" to "Rate App"),
        "version" to mapOf("vi" to "Phiên bản", "en" to "Version"),
        "logout" to mapOf("vi" to "Đăng xuất", "en" to "Log Out"),
        "login" to mapOf("vi" to "Đăng nhập", "en" to "Sign In"),
        "register" to mapOf("vi" to "Đăng ký", "en" to "Sign Up"),
        "forgot_password" to mapOf("vi" to "Quên mật khẩu?", "en" to "Forgot Password?"),
        "placeholder_email" to mapOf("vi" to "Địa chỉ Email", "en" to "Email Address"),
        "placeholder_password" to mapOf("vi" to "Mật khẩu", "en" to "Password"),
        "placeholder_confirm_password" to mapOf("vi" to "Nhập lại mật khẩu", "en" to "Confirm Password"),
        "placeholder_username" to mapOf("vi" to "Tên hiển thị", "en" to "Display Name"),
        "other_methods" to mapOf("vi" to "Hoặc đăng nhập bằng", "en" to "Or sign in with"),
        "no_account" to mapOf("vi" to "Bạn chưa có tài khoản? Đăng ký ngay", "en" to "Don't have an account? Sign Up"),
        "has_account" to mapOf("vi" to "Bạn đã có tài khoản? Đăng nhập ngay", "en" to "Already have an account? Sign In"),
        "back_to_login" to mapOf("vi" to "Quay lại đăng nhập", "en" to "Back to Sign In"),
        "send_reset" to mapOf("vi" to "Gửi yêu cầu đặt lại", "en" to "Send Reset Request"),
        "reset_sent" to mapOf("vi" to "Đã gửi email khôi phục mật khẩu!", "en" to "Password reset email sent!"),
        "loading_detail" to mapOf("vi" to "Đang tải chi tiết...", "en" to "Loading details..."),
        "loading_detail_error" to mapOf("vi" to "Có lỗi xảy ra khi tải chi tiết.", "en" to "Failed to load movie details."),
        "try_again" to mapOf("vi" to "Thử lại", "en" to "Try Again"),
        "director" to mapOf("vi" to "Đạo diễn: ", "en" to "Director: "),
        "writer" to mapOf("vi" to "Biên kịch: ", "en" to "Writers: "),
        "actor" to mapOf("vi" to "Diễn viên: ", "en" to "Cast: "),
        "episodes_list" to mapOf("vi" to "Danh sách tập", "en" to "Episodes"),
        "select_server" to mapOf("vi" to "Chọn nguồn phát", "en" to "Select Stream Source"),
        "comments" to mapOf("vi" to "Bình luận", "en" to "Comments"),
        "login_to_comment" to mapOf("vi" to "💬 Đăng nhập để tham gia bình luận", "en" to "💬 Sign in to join the conversation"),
        "no_comments" to mapOf("vi" to "Chưa có bình luận nào. Hãy là người đầu tiên!", "en" to "No comments yet. Be the first to comment!"),
        "spoiler" to mapOf("vi" to "👁️ Spoiler", "en" to "👁️ Spoiler"),
        "send" to mapOf("vi" to "Gửi", "en" to "Send"),
        "write_comment" to mapOf("vi" to "Viết bình luận...", "en" to "Write a comment..."),
        "search_placeholder" to mapOf("vi" to "Tìm tên phim, đạo diễn, diễn viên...", "en" to "Search movies, directors, actors..."),
        "filters" to mapOf("vi" to "Bộ lọc", "en" to "Filters"),
        "format" to mapOf("vi" to "Định dạng:", "en" to "Format:"),
        "genre_label" to mapOf("vi" to "Thể loại:", "en" to "Genre:"),
        "country_label" to mapOf("vi" to "Quốc gia:", "en" to "Country:"),
        "year_label" to mapOf("vi" to "Năm:", "en" to "Year:"),
        "no_movies_found" to mapOf("vi" to "Không tìm thấy phim phù hợp.", "en" to "No movies found matching criteria."),
        "anonymous" to mapOf("vi" to "Ẩn danh", "en" to "Anonymous"),
        "spoiler_warning" to mapOf("vi" to "Bình luận chứa spoiler! Nhấn để xem 👁️", "en" to "Spoiler alert! Click to reveal 👁️"),
        "load_more_comments" to mapOf("vi" to "Xem thêm bình luận", "en" to "Load more comments"),
        "updating" to mapOf("vi" to "Đang cập nhật", "en" to "Updating..."),
        "show_more" to mapOf("vi" to "Xem thêm ▼", "en" to "Show more ▼"),
        "show_less" to mapOf("vi" to "Thu gọn ▲", "en" to "Show less ▲"),
        "now_playing" to mapOf("vi" to "ĐANG PHÁT", "en" to "NOW PLAYING"),
        "select_episode" to mapOf("vi" to "Chọn tập phim", "en" to "Select Episode"),
        "watched" to mapOf("vi" to "Đã xem", "en" to "Watched"),
        "xem_ngay" to mapOf("vi" to "Xem ngay", "en" to "Watch Now"),
        "xem_tiep" to mapOf("vi" to "Xem tiếp", "en" to "Resume"),
        "other" to mapOf("vi" to "Khác", "en" to "Other"),
        "clear_cache_success" to mapOf("vi" to "Đã xóa bộ nhớ đệm thành công!", "en" to "Cache cleared successfully!"),
        "theme_light" to mapOf("vi" to "Sáng", "en" to "Light"),
        "theme_dark" to mapOf("vi" to "Tối", "en" to "Dark"),
        "auth_req_email_password" to mapOf("vi" to "Vui lòng nhập đầy đủ email và mật khẩu.", "en" to "Please enter both email and password."),
        "auth_req_all_fields" to mapOf("vi" to "Vui lòng điền đầy đủ thông tin đăng ký.", "en" to "Please fill in all registration fields."),
        "auth_password_mismatch" to mapOf("vi" to "Mật khẩu nhập lại không khớp!", "en" to "Passwords do not match!"),
        "auth_login_failed" to mapOf("vi" to "Đăng nhập không thành công, vui lòng thử lại.", "en" to "Login failed, please try again."),
        "auth_register_success" to mapOf("vi" to "Đăng ký thành công. Hãy chuyển qua tab đăng nhập.", "en" to "Registration successful. Please sign in."),
        "auth_error_generic" to mapOf("vi" to "Có lỗi xảy ra, vui lòng thử lại.", "en" to "An error occurred, please try again."),
        "auth_login_subtitle" to mapOf("vi" to "Đăng nhập để đồng bộ lịch sử và tủ phim", "en" to "Sign in to sync history and library"),
        "auth_register_subtitle" to mapOf("vi" to "Tạo tài khoản SofaFlix mới", "en" to "Create a new SofaFlix account"),
        "auth_no_account_prefix" to mapOf("vi" to "Nếu bạn chưa có tài khoản, ", "en" to "If you don't have an account, "),
        "auth_has_account_prefix" to mapOf("vi" to "Nếu bạn đã có tài khoản, ", "en" to "If you already have an account, "),
        "auth_register_action" to mapOf("vi" to "đăng ký ngay", "en" to "sign up now"),
        "auth_login_action" to mapOf("vi" to "đăng nhập ngay", "en" to "sign in now"),
        "auth_placeholder_email_your" to mapOf("vi" to "Nhập email của bạn", "en" to "Enter your email"),
        "auth_placeholder_username_login" to mapOf("vi" to "Tên tài khoản đăng nhập", "en" to "Username"),
        "auth_other_methods" to mapOf("vi" to "Phương thức khác", "en" to "Other methods"),
        "auth_google_login" to mapOf("vi" to "Đăng nhập với Google", "en" to "Sign in with Google"),
        "auth_hide" to mapOf("vi" to "Ẩn", "en" to "Hide"),
        "auth_show" to mapOf("vi" to "Hiện", "en" to "Show"),
        "profile" to mapOf("vi" to "Cá nhân", "en" to "Profile"),
        "guest_user" to mapOf("vi" to "Thành viên SofaFlix", "en" to "SofaFlix Member")
    )

    fun t(key: String, lang: String): String {
        return translations[key]?.get(lang) ?: key
    }
}
