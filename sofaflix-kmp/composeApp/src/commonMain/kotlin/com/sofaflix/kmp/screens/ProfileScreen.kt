package com.sofaflix.kmp.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sofaflix.kmp.SofaFlixApi
import com.sofaflix.kmp.firstText
import com.sofaflix.kmp.jsonObjectOrNull
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    api: SofaFlixApi,
    token: String,
    userProfileName: String,
    onLoginSuccess: (token: String, name: String) -> Unit,
    onLogout: () -> Unit
) {
    val isLoggedIn = token.isNotBlank()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191B24))
    ) {
        if (isLoggedIn) {
            LoggedInContent(
                username = userProfileName,
                onLogout = onLogout
            )
        } else {
            AuthContent(
                api = api,
                onLoginSuccess = onLoginSuccess
            )
        }
    }
}

@Composable
fun LoggedInContent(
    username: String,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 18.dp)
        ) {
            Text(
                text = "Cá nhân",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        
        // Profile Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                ) {
                    AsyncImage(
                        model = "https://upload.wikimedia.org/wikipedia/commons/0/0b/Netflix-avatar.png",
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Profile Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = username.ifBlank { "Thành viên SofaFlix" },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100 Xu • 0 XP",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Logout button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onLogout() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Đăng xuất",
                        color = Color(0xFFEF4444),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(30.dp))
        
        // Menu Sections
        MenuSection(
            title = "Cài đặt",
            items = listOf(
                MenuItemData(Icons.Default.Settings, "Giao diện", "Tối"),
                MenuItemData(Icons.Default.Menu, "Ngôn ngữ", "Tiếng Việt"),
                MenuItemData(Icons.Default.Notifications, "Thông báo", "Bật")
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        MenuSection(
            title = "Khác",
            items = listOf(
                MenuItemData(Icons.Default.Delete, "Xóa bộ nhớ đệm", ""),
                MenuItemData(Icons.Default.Star, "Đánh giá ứng dụng", ""),
                MenuItemData(Icons.Default.Info, "Phiên bản", "1.0.0")
            )
        )
    }
}

data class MenuItemData(
    val icon: ImageVector,
    val label: String,
    val value: String
)

@Composable
fun MenuSection(
    title: String,
    items: List<MenuItemData>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.02f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
        ) {
            items.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Action */ }
                        .padding(vertical = 15.dp, horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Text(
                        text = item.label,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (item.value.isNotBlank()) {
                        Text(
                            text = item.value,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Forward",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                if (index < items.size - 1) {
                    Divider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun AuthContent(
    api: SofaFlixApi,
    onLoginSuccess: (String, String) -> Unit
) {
    var isLoginTab by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun handleAuth() {
        errorMsg = null
        successMsg = null
        
        if (isLoginTab) {
            if (email.isBlank() || password.isBlank()) {
                errorMsg = "Vui lòng nhập đầy đủ email và mật khẩu."
                return
            }
        } else {
            if (username.isBlank() || email.isBlank() || name.isBlank() || password.isBlank()) {
                errorMsg = "Vui lòng điền đầy đủ thông tin đăng ký."
                return
            }
        }
        
        loading = true
        scope.launch {
            try {
                if (isLoginTab) {
                    val loginRes = api.login(email.trim(), password)
                    val token = loginRes.firstText("token")
                    if (token.isNotBlank()) {
                        val profileRes = api.profile()
                        val profileObj = profileRes.jsonObjectOrNull?.get("user") ?: profileRes
                        val displayName = profileObj.firstText("name", "username").ifBlank { email }
                        onLoginSuccess(token, displayName)
                    } else {
                        errorMsg = "Đăng nhập không thành công, vui lòng thử lại."
                    }
                } else {
                    api.register(username.trim(), password, email.trim(), name.trim())
                    successMsg = "Đăng ký thành công. Hãy chuyển qua tab đăng nhập."
                    isLoginTab = true
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: "Có lỗi xảy ra, vui lòng thử lại."
            } finally {
                loading = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 90.dp)
        ) {
            // App Branding
            Text(
                text = "SOFAFLIX",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Companion.Black,
                letterSpacing = 1.sp
            )
            
            Text(
                text = if (isLoginTab) "Đăng nhập để đồng bộ lịch sử và tủ phim" else "Tạo tài khoản SofaFlix mới",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 32.dp)
                    .padding(horizontal = 12.dp)
            )
            
            // Auth Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Tabs Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        TabHeader(
                            title = "Đăng nhập",
                            isActive = isLoginTab,
                            onClick = {
                                isLoginTab = true
                                errorMsg = null
                                successMsg = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TabHeader(
                            title = "Đăng ký",
                            isActive = !isLoginTab,
                            onClick = {
                                isLoginTab = false
                                errorMsg = null
                                successMsg = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(22.dp))
                    
                    // Messages
                    errorMsg?.let {
                        Text(
                            text = it,
                            color = Color(0xFFF87171),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF87171).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFF87171).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    successMsg?.let {
                        Text(
                            text = it,
                            color = Color(0xFF2DD4BF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2DD4BF).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Fields
                    if (isLoginTab) {
                        AuthTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Email đăng nhập",
                            icon = Icons.Default.Email
                        )
                    } else {
                        AuthTextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = "Tên tài khoản",
                            icon = Icons.Default.Person
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AuthTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Địa chỉ email",
                            icon = Icons.Default.Email
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AuthTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Họ tên hiển thị",
                            icon = Icons.Default.Face
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Mật khẩu",
                        icon = Icons.Default.Lock,
                        isPassword = true
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Submit button
                    Button(
                        onClick = { handleAuth() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !loading
                    ) {
                        if (loading) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = if (isLoginTab) "ĐĂNG NHẬP" else "ĐĂNG KÝ TÀI KHOẢN",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabHeader(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isActive) Color.Black else Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = placeholder,
                tint = Color.White.copy(alpha = 0.48f),
                modifier = Modifier.size(18.dp)
            )
            
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 15.sp
                ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
        }
        
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 28.dp)
            )
        }
    }
}
