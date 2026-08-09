package app.pentastic.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
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
    placeholder: String = "",
    actionIconContentDescription: String = "Add",
    showPriorityButton: Boolean = false,
    onPriorityActionClick: () -> Unit = {},
    maxLength: Int = 1000,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        // This bar owns the bottom inset rather than taking it from the caller, so it
        // reaches the screen edge instead of stopping short of the navigation bar.
        // union() takes whichever inset is larger, so the bar sits on the keyboard
        // once it opens.
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom)
            )
            .height(80.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            value = text,
            onValueChange = { if (it.length <= maxLength) onTextChange(it) },
            cursorBrush = SolidColor(AppTheme.colors.cursor),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            // Beyond what fits the fixed 80.dp bar the field scrolls, rather than
            // growing its text up over the list above
            maxLines = 3,
            textStyle = TextStyle(
                color = AppTheme.colors.primaryText.copy(alpha = 0.9f),
                lineHeight = 20.sp,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp
            ),
            decorationBox = { innerTextField ->
                // No vertical padding: the row's CenterVertically is what centers the text
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, color = AppTheme.colors.hint, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            }
        )
        Box(contentAlignment = Alignment.Center) {
            if (showPriorityButton && text.isNotBlank() && !isEditing) {
                IconButton(
                    onClick = { onPriorityActionClick() },
                    modifier = Modifier.size(42.dp).offset(y = (-42).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Add with priority",
                        modifier = Modifier.size(24.dp),
                        tint = AppTheme.colors.priorityText.copy(alpha = 0.5f)
                    )
                }
            }
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
                if (isEditing) {
                    Icon(
                        modifier = Modifier.size(42.dp),
                        imageVector = Icons.Default.Check,
                        contentDescription = actionIconContentDescription,
                        tint = AppTheme.colors.icon
                    )
                } else {
                    val iconColor = AppTheme.colors.icon
                    Canvas(modifier = Modifier.size(48.dp)) {
                        val strokeWidth = 1.5.dp.toPx()
                        val center = size.width / 2
                        val lineLength = size.width * 0.6f
                        drawLine(
                            color = iconColor,
                            start = Offset(center - lineLength / 2, center),
                            end = Offset(center + lineLength / 2, center),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = iconColor,
                            start = Offset(center, center - lineLength / 2),
                            end = Offset(center, center + lineLength / 2),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
