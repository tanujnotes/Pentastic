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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.ExperimentalTime

@Composable
fun IndexPage(
    noOfPages: Int,
    notesCountByPage: Map<Int, Int>,
    priorityNotesCountByPage: Map<Int, Int>,
    pageNames: Map<Int, String>,
    onPageClick: (Int) -> Unit,
    onPageNameChange: (Int, String) -> Unit,
) {

    var showDialog by remember { mutableStateOf(false) }
    var selectedPageIndex by remember { mutableStateOf(-1) }

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
            items(noOfPages) { pageIndex ->
                if (pageIndex == 0)
                    Spacer(Modifier.height(8.dp))
                else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onPageClick(pageIndex) },
                                onLongClick = {
                                    selectedPageIndex = pageIndex
                                    showDialog = true
                                }
                            ),
                    ) {
                        Text(text = "$pageIndex.", color = Color.Gray, modifier = Modifier.defaultMinSize(minWidth = 32.dp))
                        Spacer(Modifier.width(8.dp))

                        Text(text = pageNames[pageIndex] ?: "Page $pageIndex")

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
                                    if ((priorityNotesCountByPage[pageIndex] ?: 0) > 0)
                                        priorityNotesCountByPage[pageIndex]
                                    else
                                        (notesCountByPage[pageIndex] ?: 0)
                                    ).toString(),
                            color = if ((priorityNotesCountByPage[pageIndex] ?: 0) > 0) Color.Red
                            else if ((notesCountByPage[pageIndex] ?: 0) > 0) Color.Gray
                            else Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        if (showDialog) {
            EditPageNameDialog(
                pageIndex = selectedPageIndex,
                currentPageName = pageNames[selectedPageIndex] ?: "",
                onDismiss = { showDialog = false },
                onConfirm = { newName ->
                    onPageNameChange(selectedPageIndex, newName.ifBlank { "Page $selectedPageIndex" })
                    showDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPageNameDialog(
    pageIndex: Int,
    currentPageName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(currentPageName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit page name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Page $pageIndex") },
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
            11,
            mapOf(
                1 to 5,
                2 to 3,
                5 to 18,
                8 to 1,
                10 to 12
            ),
            mapOf(1 to 4),
            mapOf(
                1 to "Default",
                2 to "Todo later",
                5 to "Pro Launcher",
            ),
            onPageClick = {}
        ) { page, name -> }
    }
}