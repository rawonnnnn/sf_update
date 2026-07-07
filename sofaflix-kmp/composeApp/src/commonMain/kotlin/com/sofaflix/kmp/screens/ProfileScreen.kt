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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.sofaflix.kmp.SofaFlixApi
import com.sofaflix.kmp.firstText
import com.sofaflix.kmp.jsonObjectOrNull
import com.sofaflix.kmp.LocalLanguage
import com.sofaflix.kmp.Lang
import com.sofaflix.kmp.StorageHelpers
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    api: SofaFlixApi,
    token: String,
    userProfileName: String,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onLoginSuccess: (token: String, name: String) -> Unit,
    onLogout: () -> Unit
) {
    val isLoggedIn = token.isNotBlank()
    val lang = LocalLanguage.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191B24))
    ) {
        if (isLoggedIn) {
            LoggedInContent(
                username = userProfileName,
                currentLanguage = currentLanguage,
                onLanguageChange = onLanguageChange,
                onLogout = onLogout
            )
        } else {
            AuthContent(
                api = api,
                onLoginSuccess = onLoginSuccess
            )
        }
        
        // Language switcher for guest users
        if (!isLoggedIn) {
            var showLangDialog by remember { mutableStateOf(false) }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .clickable { showLangDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Language",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (currentLanguage == "vi") "VI" else "EN",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (showLangDialog) {
                AlertDialog(
                    onDismissRequest = { showLangDialog = false },
                    title = {
                        Text(
                            text = Lang.t("language", lang),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    containerColor = Color(0xFF1F222B),
                    confirmButton = {
                        TextButton(onClick = { showLangDialog = false }) {
                            Text(if (lang == "vi") "Đóng" else "Close", color = Color(0xFF1CC749))
                        }
                    },
                    text = {
                        Column {
                            listOf("vi" to "Tiếng Việt", "en" to "English").forEach { (code, name) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onLanguageChange(code)
                                            showLangDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, color = Color.White, fontSize = 16.sp)
                                    if (currentLanguage == code) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF1CC749),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LoggedInContent(
    username: String,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()
    val lang = LocalLanguage.current
    
    var showLangDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var cacheClearedText by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
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
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 18.dp)
        ) {
            Text(
                text = Lang.t("profile", lang),
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
                        text = username.ifBlank { Lang.t("guest_user", lang) },
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
                        text = Lang.t("logout", lang),
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
            title = Lang.t("settings", lang),
            items = listOf(
                MenuItemData(
                    icon = Icons.Default.Menu, 
                    label = Lang.t("language", lang), 
                    value = if (currentLanguage == "vi") "Tiếng Việt" else "English",
                    onClick = { showLangDialog = true }
                ),
                MenuItemData(
                    icon = Icons.Default.Notifications, 
                    label = Lang.t("notifications", lang), 
                    value = if (lang == "vi") "Bật" else "On"
                )
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        MenuSection(
            title = Lang.t("other", lang),
            items = listOf(
                MenuItemData(
                    icon = Icons.Default.Delete, 
                    label = Lang.t("clear_cache", lang), 
                    value = cacheClearedText ?: "",
                    onClick = { showCacheDialog = true }
                ),
                MenuItemData(
                    icon = Icons.Default.Star, 
                    label = Lang.t("rate_app", lang), 
                    value = ""
                ),
                MenuItemData(
                    icon = Icons.Default.Info, 
                    label = Lang.t("version", lang), 
                    value = "1.0.0"
                )
            )
        )
    }

    if (showLangDialog) {
        AlertDialog(
            onDismissRequest = { showLangDialog = false },
            title = {
                Text(
                    text = Lang.t("language", lang),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            containerColor = Color(0xFF1F222B),
            confirmButton = {
                TextButton(onClick = { showLangDialog = false }) {
                    Text(if (lang == "vi") "Đóng" else "Close", color = Color(0xFF1CC749))
                }
            },
            text = {
                Column {
                    listOf("vi" to "Tiếng Việt", "en" to "English").forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageChange(code)
                                    showLangDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, color = Color.White, fontSize = 16.sp)
                            if (currentLanguage == code) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF1CC749),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = {
                Text(
                    text = Lang.t("clear_cache", lang),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = if (lang == "vi") "Bạn có chắc chắn muốn xóa toàn bộ bộ nhớ đệm (lịch sử xem, phim đã lưu)? Hành động này không thể hoàn tác." 
                           else "Are you sure you want to clear all cache (watch history, saved movies)? This action cannot be undone.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            containerColor = Color(0xFF1F222B),
            confirmButton = {
                TextButton(
                    onClick = {
                        StorageHelpers.clearCache()
                        GuestStorage.favorites.clear()
                        GuestStorage.history.clear()
                        showCacheDialog = false
                        cacheClearedText = Lang.t("clear_cache_success", lang)
                    }
                ) {
                    Text(if (lang == "vi") "Xóa" else "Clear", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCacheDialog = false }) {
                    Text(if (lang == "vi") "Hủy" else "Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

data class MenuItemData(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val onClick: () -> Unit = {}
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
                        .clickable { item.onClick() }
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
    val lang = LocalLanguage.current
    val focusManager = LocalFocusManager.current
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
        focusManager.clearFocus()
        errorMsg = null
        successMsg = null
        
        if (isLoginTab) {
            if (email.isBlank() || password.isBlank()) {
                errorMsg = Lang.t("auth_req_email_password", lang)
                return
            }
        } else {
            if (username.isBlank() || email.isBlank() || name.isBlank() || password.isBlank()) {
                errorMsg = Lang.t("auth_req_all_fields", lang)
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
                        errorMsg = Lang.t("auth_login_failed", lang)
                    }
                } else {
                    api.register(username.trim(), password, email.trim(), name.trim())
                    successMsg = Lang.t("auth_register_success", lang)
                    isLoginTab = true
                }
            } catch (e: Exception) {
                errorMsg = e.message ?: Lang.t("auth_error_generic", lang)
            } finally {
                loading = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                text = if (isLoginTab) Lang.t("auth_login_subtitle", lang) else Lang.t("auth_register_subtitle", lang),
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
                            title = Lang.t("login", lang),
                            isActive = isLoginTab,
                            onClick = {
                                isLoginTab = true
                                errorMsg = null
                                successMsg = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TabHeader(
                            title = Lang.t("register", lang),
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
                            placeholder = Lang.t("placeholder_email", lang),
                            icon = Icons.Default.Email
                        )
                    } else {
                        AuthTextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = Lang.t("placeholder_username", lang),
                            icon = Icons.Default.Person
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AuthTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = Lang.t("placeholder_email", lang),
                            icon = Icons.Default.Email
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AuthTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = Lang.t("placeholder_username", lang),
                            icon = Icons.Default.Face
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AuthTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = Lang.t("placeholder_password", lang),
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        imeAction = ImeAction.Done,
                        onAction = { handleAuth() }
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
                                text = if (isLoginTab) Lang.t("login", lang).uppercase() else Lang.t("register", lang).uppercase(),
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
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onAction: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onAny = {
                        if (onAction != null) {
                            onAction()
                        } else if (imeAction == ImeAction.Done) {
                            focusManager.clearFocus()
                        }
                    }
                ),
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
