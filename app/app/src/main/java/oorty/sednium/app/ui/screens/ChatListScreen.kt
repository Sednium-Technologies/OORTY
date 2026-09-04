package oorty.sednium.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import oorty.sednium.app.model.ChatSession
import oorty.sednium.app.ui.components.ChatListRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import oorty.sednium.app.ui.theme.SedniumRadii
import oorty.sednium.app.ui.theme.SedniumColors
import oorty.sednium.app.ui.theme.OrangeAlpha

/**
 * PAGE 2 / 4 — Chat List Screen.
 * Slides in from the left (mirrors ChatListDrawer.tsx, which used
 * `translate-x-0` / `-translate-x-full` over 300ms). Host this inside a
 * ModalNavigationDrawer / ModalDrawerSheet from Material3.
 */
@Composable
fun ChatListScreen(
    chats: List<ChatSession>,
    currentChatId: String,
    onSelectChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onClose: () -> Unit,
    onDeleteChat: (String) -> Unit,
    onDeleteMultiple: (List<String>) -> Unit,
    onRenameChat: (String, String) -> Unit,
    onTogglePin: (String) -> Unit,
    onOpenPromptLab: () -> Unit = {}
) {
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var chatToRename by remember { mutableStateOf<ChatSession?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }
    var showMultiDeleteConfirmDialog by remember { mutableStateOf(false) }

    val sortedChats = remember(chats) {
        chats.sortedWith(compareByDescending<ChatSession> { it.isPinned }.thenByDescending { it.updatedAt })
    }

    val filteredChats = remember(sortedChats, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedChats
        } else {
            val query = searchQuery.lowercase()
            sortedChats.filter { chat ->
                chat.title.lowercase().contains(query) || 
                chat.messages.any { it.content.lowercase().contains(query) }
            }
        }
    }

    val isDark = oorty.sednium.app.ui.theme.LocalSedniumIsDark.current
    val accentColor = if (isDark) SedniumColors.DarkOrange else SedniumColors.Orange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) SedniumColors.DarkBackground else SedniumColors.Milk)
            .systemBarsPadding()
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(oorty.sednium.app.ui.theme.SedniumRadii.squircle))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = oorty.sednium.app.R.drawable.logo),
                        contentDescription = "Oorty Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Text("Oorty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accentColor)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    isSelectionMode = !isSelectionMode
                    selectedIds = emptySet()
                }) {
                    Text(if (isSelectionMode) "Cancel" else "Select", color = accentColor)
                }
                IconButton(onClick = onClose) {
                    Icon(oorty.sednium.app.ui.theme.OortyIcons.Close, contentDescription = "Close", tint = accentColor)
                }
            }
        }

        // --- New Chat button ---
        Button(
            onClick = onNewChat,
            enabled = !isSelectionMode,
            shape = RoundedCornerShape(oorty.sednium.app.ui.theme.SedniumRadii.pill),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = SedniumColors.Milk,
                disabledContainerColor = OrangeAlpha.a50,
                disabledContentColor = SedniumColors.Milk.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        ) {
            Icon(oorty.sednium.app.ui.theme.OortyIcons.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" New Chat", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        // --- Prompt Lab entry point ---
        androidx.compose.material3.OutlinedButton(
            onClick = onOpenPromptLab,
            enabled = !isSelectionMode,
            shape = RoundedCornerShape(oorty.sednium.app.ui.theme.SedniumRadii.pill),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) SedniumColors.Charcoal700 else OrangeAlpha.a30),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(oorty.sednium.app.ui.theme.OortyIcons.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(" Prompt Lab", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }

        // --- Search Bar (Pill Shape) ---
        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search chats...", style = MaterialTheme.typography.bodyMedium, color = if (isDark) SedniumColors.Gray500 else OrangeAlpha.a40) },
            leadingIcon = { 
                Icon(oorty.sednium.app.ui.theme.OortyIcons.Search, contentDescription = "Search", tint = if (isDark) SedniumColors.Gray500 else OrangeAlpha.a50, modifier = Modifier.size(18.dp)) 
            },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(oorty.sednium.app.ui.theme.OortyIcons.Close, contentDescription = "Clear search", tint = if (isDark) SedniumColors.Gray400 else OrangeAlpha.a60, modifier = Modifier.size(16.dp))
                    }
                }
            },
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = if (isDark) SedniumColors.Charcoal700 else OrangeAlpha.a30,
                focusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                unfocusedTextColor = if (isDark) SedniumColors.Gray100 else SedniumColors.Orange,
                focusedContainerColor = if (isDark) SedniumColors.Charcoal800 else Color.Transparent,
                unfocusedContainerColor = if (isDark) SedniumColors.Charcoal800 else Color.Transparent
            ),
            shape = RoundedCornerShape(oorty.sednium.app.ui.theme.SedniumRadii.pill),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
        )

        // --- List ---
        LazyColumn(
            modifier = Modifier.weight(1f).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredChats, key = { it.id }) { chat ->
                ChatListRow(
                    chat = chat,
                    isCurrent = chat.id == currentChatId,
                    isSelectionMode = isSelectionMode,
                    isChecked = selectedIds.contains(chat.id),
                    onClick = {
                        if (isSelectionMode) {
                            selectedIds = if (selectedIds.contains(chat.id)) selectedIds - chat.id else selectedIds + chat.id
                        } else {
                            onSelectChat(chat.id)
                        }
                    },
                    onTogglePin = { onTogglePin(chat.id) },
                    onRename = { 
                        chatToRename = chat
                        newTitle = chat.title
                    },
                    onDelete = { showDeleteConfirmDialog = chat.id }
                )
            }
        }

        if (chatToRename != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { chatToRename = null },
                shape = RoundedCornerShape(12.dp),
                title = { Text("Rename Chat", color = SedniumColors.Orange) },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SedniumColors.Orange,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        chatToRename?.let { chat ->
                            onRenameChat(chat.id, newTitle)
                        }
                        chatToRename = null
                    }) {
                        Text("Save", color = SedniumColors.Orange)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { chatToRename = null }) {
                        Text("Cancel", color = SedniumColors.Orange)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (showDeleteConfirmDialog != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                shape = RoundedCornerShape(12.dp),
                title = { Text("Delete Chat", color = SedniumColors.Orange) },
                text = { Text("Are you sure you want to delete this chat?", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirmDialog?.let { id -> onDeleteChat(id) }
                        showDeleteConfirmDialog = null
                    }) {
                        Text("Delete", color = SedniumColors.Red600)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text("Cancel", color = SedniumColors.Orange)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (showMultiDeleteConfirmDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showMultiDeleteConfirmDialog = false },
                shape = RoundedCornerShape(12.dp),
                title = { Text("Delete Chats", color = SedniumColors.Orange) },
                text = { Text("Are you sure you want to delete ${selectedIds.size} selected chats?", color = MaterialTheme.colorScheme.onSurface) },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteMultiple(selectedIds.toList())
                        selectedIds = emptySet()
                        isSelectionMode = false
                        showMultiDeleteConfirmDialog = false
                    }) {
                        Text("Delete", color = SedniumColors.Red600)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMultiDeleteConfirmDialog = false }) {
                        Text("Cancel", color = SedniumColors.Orange)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // --- Delete selected bar ---
        AnimatedVisibility(visible = isSelectionMode && selectedIds.isNotEmpty(), enter = slideInVertically { it }) {
            Button(
                onClick = {
                    showMultiDeleteConfirmDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = SedniumColors.Red600, contentColor = SedniumColors.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Text(" Delete Selected (${selectedIds.size})", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
