@file:OptIn(ExperimentalTime::class)

package app.pentastic.ui.composables

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pentastic.data.Page
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.ExperimentalTime

@Composable
fun IndexPage(
    pages: List<Page>,
    notesCountByPage: Map<Long, Int>,
    priorityNotesCountByPage: Map<Long, Int>,
    onPageClick: (Long) -> Unit,
    onPageNameChange: (Page, String) -> Unit,
) {

    var showDialog by remember { mutableStateOf(false) }
    var selectedPage: Page? by remember { mutableStateOf(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 17.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Index",
                style = TextStyle(
                    color = Color(0xFFA52A2A),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light
                )
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(pages) { page ->
                if (page.id == 0L) return@items // Skip page 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onPageClick(page.id) },
                            onLongClick = {
                                selectedPage = page
                                showDialog = true
                            }
                        ),
                ) {
                    Text(text = "${page.id}.", color = Color.Gray, modifier = Modifier.defaultMinSize(minWidth = 32.dp))
                    Spacer(Modifier.width(8.dp))

                    Text(text = page.name)

                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "................................................................................................................... ",
                        color = Color.LightGray,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        modifier = Modifier.defaultMinSize(minWidth = 16.dp),
                        text = (
                                if ((priorityNotesCountByPage[page.id] ?: 0) > 0)
                                    priorityNotesCountByPage[page.id]
                                else
                                    (notesCountByPage[page.id] ?: 0)
                                ).toString(),
                        color = if ((priorityNotesCountByPage[page.id] ?: 0) > 0) Color.Red
                        else if ((notesCountByPage[page.id] ?: 0) > 0) Color.Gray
                        else Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (showDialog && selectedPage != null) {
            EditPageNameDialog(
                page = selectedPage!!,
                onDismiss = { showDialog = false },
                onConfirm = { newName ->
                    onPageNameChange(selectedPage!!, newName.ifBlank { "Untitled" })
                    showDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPageNameDialog(
    page: Page,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(page.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit page name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Page ${page.id}") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                )
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
fun IndexPagePreview() {
    Surface(color = Color.White) {
        IndexPage(
            listOf(Page(1, name = "Default"), Page(2, name = "Todo later"), Page(5, name = "Pro Launcher")),
            mapOf(
                1L to 5,
                2L to 3,
                5L to 18,
                8L to 1,
                10L to 12
            ),
            mapOf(1L to 4),
            onPageClick = {}
        ) { page, name -> }
    }
}
