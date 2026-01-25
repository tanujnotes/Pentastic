package app.pentastic.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.ui.theme.AppTheme

@Composable
fun CommonInput(
    text: String,
    onTextChange: (String) -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
//    actionIcon: ImageVector = Icons.Default.Add,
    actionIconContentDescription: String = "Add",
) {
    var isInputFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { isInputFocused = it.isFocused },
            value = text,
            onValueChange = { if (it.length <= 300) onTextChange(it) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedTextColor = AppTheme.colors.primaryText.copy(alpha = 0.9f),
                unfocusedTextColor = AppTheme.colors.primaryText.copy(alpha = 0.9f),
                cursorColor = AppTheme.colors.cursor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            textStyle = TextStyle(
                lineHeight = 20.sp,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp
            )
        )
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onActionClick()
                } else {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
        ) {
            Icon(
                modifier = Modifier.size(42.dp),
                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Add,
                contentDescription = actionIconContentDescription,
                tint = AppTheme.colors.icon
            )
        }
    }
}
