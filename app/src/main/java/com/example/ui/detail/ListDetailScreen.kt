package com.example.ui.detail

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingList
import com.example.data.model.UserAccount
import com.example.ui.theme.ForestGreenContainer
import com.example.ui.theme.ForestGreenOnContainer
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.viewmodel.ShoppingActionMessage
import com.example.ui.viewmodel.ShoppingViewModel
import com.example.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    currentUser: UserAccount,
    shoppingViewModel: ShoppingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val selectedList by shoppingViewModel.currentSelectedList.collectAsStateWithLifecycle()
    val items by shoppingViewModel.currentItems.collectAsStateWithLifecycle()
    val collaborators by shoppingViewModel.currentCollaborators.collectAsStateWithLifecycle()
    val actionMessage by shoppingViewModel.actionMessage.collectAsStateWithLifecycle()
    val conferenceResult by shoppingViewModel.conferenceResult.collectAsStateWithLifecycle()

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

    // Calculations
    val totalGeral by remember(items) {
        derivedStateOf { items.sumOf { it.totalValue } }
    }
    val totalComprados by remember(items) {
        derivedStateOf { items.filter { it.isPurchased }.sumOf { it.totalValue } }
    }
    val totalPendentes by remember(items) {
        derivedStateOf { items.filter { !it.isPurchased }.sumOf { it.totalValue } }
    }
    val purchasedCount by remember(items) {
        derivedStateOf { items.count { it.isPurchased } }
    }
    val totalCount = items.size
    val progress = if (totalCount > 0) purchasedCount.toFloat() / totalCount.toFloat() else 0f

    // Dialog states
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ShoppingItem?>(null) }
    var showCashierConference by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    val currentList = selectedList
    if (currentList == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isOwner = currentList.ownerId == currentUser.id

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentList.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = currentList.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOwner) "Sua lista" else "Compartilhada por: ${currentList.ownerEmail}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_to_overview")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.testTag("btn_open_share_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar Lista"
                        )
                    }
                    IconButton(
                        onClick = { shoppingViewModel.toggleArchiveList(currentList.id) }
                    ) {
                        Icon(
                            imageVector = if (currentList.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = "Arquivar ou reativar"
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
                    itemToEdit = null
                    showAddItemDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_item")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Adicionar Produto", fontWeight = FontWeight.Bold)
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
            // PROMINENT SUMMARY BANNER
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Highlights: VALOR TOTAL GERAL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "VALOR TOTAL GERAL",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = CurrencyUtils.formatBrl(totalGeral),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("text_total_geral")
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$purchasedCount de $totalCount itens",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(progress * 100).toInt()}% concluído",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ForestGreenPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail rows: No Carrinho vs Pendente
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Comprados: ${CurrencyUtils.formatBrl(totalComprados)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Falta: ${CurrencyUtils.formatBrl(totalPendentes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // PRIMARY ACTION BUTTON: "CONFERIR NO CAIXA"
                    Button(
                        onClick = { showCashierConference = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_conferir_no_caixa"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ForestGreenPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✅ Conferir no Caixa",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ITEMS LIST
            if (items.isEmpty()) {
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
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sua lista está vazia",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toque no botão 'Adicionar Produto' abaixo para colocar nome, quantidade e valor unitário.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ShoppingItemRow(
                            item = item,
                            onTogglePurchased = {
                                shoppingViewModel.togglePurchased(item)
                            },
                            onEditClick = {
                                itemToEdit = item
                                showAddItemDialog = true
                            },
                            onDeleteClick = {
                                shoppingViewModel.deleteItem(item.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // ADD / EDIT ITEM DIALOG (Strictly 3 basic fields)
    if (showAddItemDialog) {
        ItemFormDialog(
            initialItem = itemToEdit,
            onSave = { name, qty, price, unit ->
                if (itemToEdit != null) {
                    shoppingViewModel.updateItem(
                        itemToEdit!!.copy(
                            name = name,
                            quantity = qty,
                            unitPrice = price,
                            unit = unit
                        )
                    )
                } else {
                    shoppingViewModel.addItem(name, qty, price, unit)
                }
                showAddItemDialog = false
                itemToEdit = null
            },
            onDismiss = {
                showAddItemDialog = false
                itemToEdit = null
            }
        )
    }

    // CASHIER CONFERENCE MODAL
    if (showCashierConference) {
        CashierConferenceDialog(
            totalGeral = totalGeral,
            totalComprados = totalComprados,
            lastResult = conferenceResult,
            onPerformCheck = { cashierValue, useOnlyPurchased ->
                shoppingViewModel.performCashierConference(cashierValue, useOnlyPurchased)
            },
            onDismiss = {
                showCashierConference = false
                shoppingViewModel.dismissConference()
            }
        )
    }

    // SHARE LIST MODAL
    if (showShareDialog) {
        ShareListDialog(
            list = currentList,
            isOwner = isOwner,
            collaborators = collaborators,
            onAddCollaborator = { email ->
                shoppingViewModel.shareList(email)
            },
            onRemoveCollaborator = { email ->
                shoppingViewModel.removeCollaborator(email)
            },
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onTogglePurchased: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTogglePurchased)
            .testTag("item_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPurchased) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isPurchased) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox with minimum 48dp touch target
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onTogglePurchased() },
                colors = CheckboxDefaults.colors(checkedColor = ForestGreenPrimary),
                modifier = Modifier.testTag("checkbox_item_${item.id}")
            )

            // Name and Quantity/Unit price
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 8.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (item.isPurchased) FontWeight.Normal else FontWeight.Bold,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${CurrencyUtils.formatQuantity(item.quantity, item.unit)} × ${CurrencyUtils.formatBrl(item.unitPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Total per item (Quantity x Unit price)
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = CurrencyUtils.formatBrl(item.totalValue),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("text_item_total_${item.id}")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_edit_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar item",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_delete_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir item",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// 3 BASIC FIELDS FORM DIALOG
@Composable
fun ItemFormDialog(
    initialItem: ShoppingItem?,
    onSave: (name: String, quantity: Double, unitPrice: Double, unit: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var quantityInput by remember {
        mutableStateOf(
            if (initialItem != null) {
                if (initialItem.quantity % 1.0 == 0.0) initialItem.quantity.toLong().toString()
                else initialItem.quantity.toString().replace(".", ",")
            } else "1"
        )
    }
    var unitPriceInput by remember {
        mutableStateOf(
            if (initialItem != null) {
                String.format(java.util.Locale("pt", "BR"), "%.2f", initialItem.unitPrice)
            } else ""
        )
    }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "un") }

    val parsedQty = CurrencyUtils.parseQuantity(quantityInput) ?: 0.0
    val parsedPrice = CurrencyUtils.parseAmount(unitPriceInput) ?: 0.0
    val computedTotal = parsedQty * parsedPrice

    val units = listOf("un", "kg", "g", "L", "pct", "cx")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("dialog_item_form"),
        title = {
            Text(
                text = if (initialItem == null) "Adicionar Produto" else "Editar Produto",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Preencha os 3 campos básicos do produto:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo 1: Nome do produto
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("1. Nome do produto *") },
                    placeholder = { Text("Ex: Arroz 5kg, Leite, Tomate") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_item_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Campo 2: Quantidade (inteiro ou decimal ex: 2, 1,5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("2. Quantidade *") },
                        placeholder = { Text("Ex: 2 ou 1,5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_item_quantity")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Unit selector chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            units.take(3).forEach { u ->
                                FilterChip(
                                    selected = unit == u,
                                    onClick = { unit = u },
                                    label = { Text(u, fontSize = 12.sp) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Campo 3: Valor unitário (R$ com centavos)
                OutlinedTextField(
                    value = unitPriceInput,
                    onValueChange = { unitPriceInput = it },
                    label = { Text("3. Valor unitário (R$) *") },
                    placeholder = { Text("Ex: 5,90") },
                    prefix = { Text("R$ ", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_item_unit_price")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Automatic calculation preview
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total deste item:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${CurrencyUtils.formatQuantity(parsedQty, unit)} × ${CurrencyUtils.formatBrl(parsedPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = CurrencyUtils.formatBrl(computedTotal),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("text_preview_item_total")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && parsedQty > 0.0 && parsedPrice >= 0.0) {
                        onSave(name.trim(), parsedQty, parsedPrice, unit)
                    }
                },
                enabled = name.isNotBlank() && parsedQty > 0.0,
                modifier = Modifier.testTag("btn_save_item")
            ) {
                Text(if (initialItem == null) "Adicionar Item" else "Salvar Alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
