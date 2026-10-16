package app.pentastic.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.AppCalendar
import app.pentastic.data.AppLanguage
import app.pentastic.ui.theme.appRadioButtonColors
import app.pentastic.ui.theme.AppTheme
import app.pentastic.ui.theme.titleFontFamily

/**
 * First-run language choice, shown before anything else is created or saved.
 * Both cards are always labeled in BOTH languages, so the very first screen is
 * understandable no matter which language the user ends up picking.
 */
@Composable
fun WelcomeScreen(
    onDone: (language: AppLanguage, calendar: AppCalendar) -> Unit,
) {
    val colors = AppTheme.colors

    var language by remember { mutableStateOf(AppLanguage.FARSI) }
    var calendar by remember { mutableStateOf(AppCalendar.JALALI) }

    fun label(language: AppLanguage, farsi: String, english: String) =
        if (language == AppLanguage.FARSI) farsi else english

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label(language, "به کاره خوش اومدی!", "Welcome to Pentastic!"),
                color = colors.pageTitle,
                fontSize = 30.sp,
                fontFamily = titleFontFamily()
            )
            Spacer(Modifier.height(28.dp))

            // Language
            Text(
                text = "زبان / Language",
                color = colors.hint,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            AppLanguage.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { language = option }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = language == option,
                        onClick = { language = option },
                        colors = appRadioButtonColors(),
                    )
                    Text(
                        text = option.label,
                        color = colors.primaryText,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Calendar
            Text(
                text = label(language, "تقویم", "Calendar"),
                color = colors.hint,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            AppCalendar.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { calendar = option }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = calendar == option,
                        onClick = { calendar = option },
                        colors = appRadioButtonColors(),
                    )
                    Text(
                        text = label(
                            language,
                            if (option == AppCalendar.JALALI) "شمسی" else "میلادی",
                            if (option == AppCalendar.JALALI) "Jalali (Persian)" else "Gregorian"
                        ),
                        color = colors.primaryText,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onDone(language, calendar) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryText,
                    contentColor = colors.menuBackground
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = label(language, "شروع کنیم", "Let's go"),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}
