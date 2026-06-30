package com.smartjam.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smartjam.app.ui.theme.BrandCyan

@Composable
fun UserIdentityBlock(
    firstName: String?,
    lastName: String?,
    username: String,
    modifier: Modifier = Modifier,
    isCyanUsername: Boolean = false,
) {
    val fullName = "${firstName ?: ""} ${lastName ?: ""}".trim()

    Column(modifier = modifier) {
        if (fullName.isNotEmpty()) {
            Text(
                text = fullName,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "@$username",
                color = if (isCyanUsername) BrandCyan else Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
            )
        } else {
            Text(
                text = username,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
