package com.foodfest.app.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.foodfest.app.theme.AppColors

/**
 * Shared form field for create/edit screens that need single-line, multiline, or number inputs.
 */
@Composable
fun FoodFestFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 56.dp,
    maxLines: Int = if (singleLine) 1 else 6,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    errorText: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = {
                if (placeholder.isNotBlank()) {
                    Text(placeholder, color = AppColors.GrayPlaceholder)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Orange,
                unfocusedBorderColor = AppColors.GrayPlaceholder.copy(alpha = 0.35f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = AppColors.Brown
            )
        )
        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                color = AppColors.Error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
