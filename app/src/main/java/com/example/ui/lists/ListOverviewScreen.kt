package com.example.ui.lists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShoppingList
import com.example.data.model.UserAccount
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenOnContainer
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ShoppingActionMessage
import com.example.ui.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListOverviewScreen(
    currentUser: UserAccount,
    shoppingViewModel: ShoppingViewModel,
    authViewModel: AuthViewModel,
    onSelectList: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val personalLists by shoppingViewModel.personalLists.collectAsStateWithLifecycle()
    val sharedLists by shoppingViewModel.sharedLists.collectAsStateWithLifecycle()
    val archivedLists by shoppingViewModel.archivedLists.collectAsStateWithLifecycle()
    val actionMessage by shoppingViewModel.actionMessage.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Minhas Listas, 1: Compartilhadas, 2: Histórico
    var selectedCategoryFilter by remember { mutableStateOf("Todas") }

    // Dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var newListTitle by remember { mutableStateOf("") }
    var newListCategory by remember { mutableStateOf("Supermercado") }

    // Share dialog state from overview
    var listToShare by remember { mutableStateOf<ShoppingList?>(null) }
    var shareEmailInput by remember { mutableStateOf("") }

    // Delete confirmation
    var listToDelete by remember { mutableStateOf<ShoppingList?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            when (msg) {
                is ShoppingActionMessage.Success -> snackbarHostState.showSnackbar(msg.message)
                is ShoppingActionMessage.Error -> snackbarHostState.showSnackbar(msg.message)
            }
            shoppingViewModel.clearActionMessage()
        }
    }

    val categories = listOf("Todas", "Supermercado", "Feira", "Farmácia", "Geral")

    val currentListCollection = when (selectedTab) {
        0 -> personalLists
        1 -> sharedLists
        else -> archivedLists
    }

    val filteredLists = if (selectedCategoryFilter == "Todas") {
        currentListCollection
    } else {
        currentListCollection.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Lista de Compras",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Olá, ${currentUser.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " • ${currentUser.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { authViewModel.logout() },
                        modifier = Modifier.testTag("btn_logout")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sair da conta"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newListTitle = ""
                    newListCategory = "Supermercado"
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_create_list")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova Lista")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Nova Lista", fontWeight = FontWeight.Bold)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Minhas (${personalLists.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_personal_lists")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Compartilhadas (${sharedLists.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_shared_lists")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Histórico (${archivedLists.size})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_archived_lists")
                )
            }

            // Categories horizontal scroll
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategoryFilter == category,
                        onClick = { selectedCategoryFilter = category },
                        label = { Text(category) },
                        leadingIcon = if (selectedCategoryFilter == category) {
                            {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            // Lists Content
            if (filteredLists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (selectedTab) {
                                    1 -> Icons.Default.Share
                                    2 -> Icons.Default.Archive
                                    else -> Icons.Default.ShoppingCart
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = when (selectedTab) {
                                1 -> "Nenhuma lista compartilhada com você"
                                2 -> "Nenhuma lista no histórico"
                                else -> "Você ainda não tem listas nesta categoria"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when (selectedTab) {
                                1 -> "Quando outro usuário cadastrado compartilhar uma lista com ${currentUser.email}, ela aparecerá aqui automaticamente com permissão de edição."
                                2 -> "Quando você terminar as compras e conferir no caixa, pode arquivar a lista para consultar os valores depois."
                                else -> "Toque no botão '+ Nova Lista' abaixo para criar sua lista de supermercado, farmácia ou feira!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLists, key = { it.id }) { list ->
                        val isOwner = list.ownerId == currentUser.id

                        ShoppingListCard(
                            list = list,
                            isOwner = isOwner,
                            onClick = { onSelectList(list.id) },
                            onShareClick = {
                                listToShare = list
                                shareEmailInput = ""
                            },
                            onArchiveClick = {
                                shoppingViewModel.toggleArchiveList(list.id)
                            },
                            onDeleteClick = {
                                listToDelete = list
                            }
                        )
                    }
                }
            }
        }
    }

    // CREATE LIST DIALOG
    if (showCreateDialog) {
        val listCategories = listOf("Supermercado", "Feira", "Farmácia", "Geral")
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "Criar Nova Lista",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Dê um nome para a sua lista e selecione a categoria:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newListTitle,
                        onValueChange = { newListTitle = it },
                        label = { Text("Nome da Lista") },
                        placeholder = { Text("Ex: Compras da Semana, Feira de Domingo") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_new_list_title")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Categoria:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listCategories.forEach { cat ->
                            FilterChip(
                                selected = newListCategory == cat,
                                onClick = { newListCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListTitle.isNotBlank()) {
                            shoppingViewModel.createList(newListTitle, newListCategory)
                            showCreateDialog = false
                        }
                    },
                    enabled = newListTitle.isNotBlank(),
                    modifier = Modifier.testTag("btn_confirm_create_list")
                ) {
                    Text("Criar Lista")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // SHARE LIST DIALOG
    listToShare?.let { list ->
        AlertDialog(
            onDismissRequest = { listToShare = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = ForestGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Compartilhar Lista", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Compartilhe a lista \"${list.title}\" com outra pessoa cadastrada:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A pessoa receberá acesso para visualizar e editar os itens. Somente você (criador) poderá remover participantes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = shareEmailInput,
                        onValueChange = { shareEmailInput = it },
                        label = { Text("E-mail do usuário cadastrado") },
                        placeholder = { Text("amigo@exemplo.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_share_email")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shoppingViewModel.selectList(list.id)
                        shoppingViewModel.shareList(shareEmailInput)
                        listToShare = null
                    },
                    enabled = shareEmailInput.isNotBlank() && shareEmailInput.contains("@"),
                    modifier = Modifier.testTag("btn_confirm_share")
                ) {
                    Text("Compartilhar")
                }
            },
            dismissButton = {
                TextButton(onClick = { listToShare = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DELETE CONFIRMATION DIALOG
    listToDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { listToDelete = null },
            title = { Text(text = "Excluir Lista", fontWeight = FontWeight.Bold) },
            text = {
                Text("Tem certeza de que deseja excluir permanentemente a lista \"${list.title}\"? Todos os itens associados serão apagados.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        shoppingViewModel.deleteList(list.id)
                        listToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("btn_confirm_delete_list")
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ShoppingListCard(
    list: ShoppingList,
    isOwner: Boolean,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("list_card_${list.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (list.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (list.isArchived) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon and title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                when (list.category) {
                                    "Farmácia" -> MaterialTheme.colorScheme.secondaryContainer
                                    "Feira" -> ForestGreenContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (list.category) {
                                "Farmácia" -> Icons.Default.LocalPharmacy
                                "Feira" -> Icons.Default.Store
                                else -> Icons.Default.ShoppingCart
                            },
                            contentDescription = list.category,
                            tint = ForestGreenOnContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = list.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = list.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = if (isOwner) "Sua lista" else "Por: ${list.ownerEmail}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Arrow indicator
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Abrir lista",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOwner) {
                    TextButton(
                        onClick = onShareClick,
                        modifier = Modifier.testTag("btn_share_list_${list.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartilhar", fontSize = 13.sp)
                    }
                }

                TextButton(onClick = onArchiveClick) {
                    Icon(
                        imageVector = if (list.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = "Arquivar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (list.isArchived) "Reativar" else "Arquivar", fontSize = 13.sp)
                }

                if (isOwner) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
