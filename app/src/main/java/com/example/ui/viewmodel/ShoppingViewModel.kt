package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CashierAudit
import com.example.data.model.ListCollaborator
import com.example.data.model.ShoppingItem
import com.example.data.model.ShoppingList
import com.example.data.model.UserAccount
import com.example.data.repository.ShoppingRepository
import com.example.util.CurrencyUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class CashierStatus {
    NONE,
    MATCH,    // Valores batem! Diferença: R$ 0,00
    SURPLUS,  // Sobra R$ X,XX (valor do caixa maior que o esperado)
    SHORTAGE  // Falta R$ X,XX (valor do caixa menor que o esperado)
}

data class CashierConferenceResult(
    val status: CashierStatus = CashierStatus.NONE,
    val cashierAmount: Double = 0.0,
    val expectedAmount: Double = 0.0,
    val difference: Double = 0.0,
    val badgeMessage: String = "",
    val detailMessage: String = "",
    val checkedAt: Long = 0L
)

sealed interface ShoppingActionMessage {
    data class Success(val message: String) : ShoppingActionMessage
    data class Error(val message: String) : ShoppingActionMessage
}

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingViewModel(
    private val shoppingRepository: ShoppingRepository,
    private val currentUserFlow: StateFlow<UserAccount?>
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<ShoppingActionMessage?>(null)
    val actionMessage: StateFlow<ShoppingActionMessage?> = _actionMessage.asStateFlow()

    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = _selectedListId.asStateFlow()

    // Personal lists
    val personalLists: StateFlow<List<ShoppingList>> = currentUserFlow.flatMapLatest { user ->
        if (user != null) shoppingRepository.getActivePersonalLists(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shared with me lists
    val sharedLists: StateFlow<List<ShoppingList>> = currentUserFlow.flatMapLatest { user ->
        if (user != null) shoppingRepository.getSharedLists(user.id, user.email)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Archived lists
    val archivedLists: StateFlow<List<ShoppingList>> = currentUserFlow.flatMapLatest { user ->
        if (user != null) shoppingRepository.getArchivedLists(user.id, user.email)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected list entity
    val currentSelectedList: StateFlow<ShoppingList?> = _selectedListId.flatMapLatest { listId ->
        if (listId != null) shoppingRepository.observeList(listId)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Items for currently selected list
    val currentItems: StateFlow<List<ShoppingItem>> = _selectedListId.flatMapLatest { listId ->
        if (listId != null) shoppingRepository.getItemsForList(listId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Collaborators for currently selected list
    val currentCollaborators: StateFlow<List<ListCollaborator>> = _selectedListId.flatMapLatest { listId ->
        if (listId != null) shoppingRepository.getCollaborators(listId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cashier audits history for current list
    val currentAudits: StateFlow<List<CashierAudit>> = _selectedListId.flatMapLatest { listId ->
        if (listId != null) shoppingRepository.getAuditsForList(listId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cashier verification state
    private val _conferenceResult = MutableStateFlow<CashierConferenceResult?>(null)
    val conferenceResult: StateFlow<CashierConferenceResult?> = _conferenceResult.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun selectList(listId: Long?) {
        _selectedListId.value = listId
        _conferenceResult.value = null
    }

    fun createList(title: String, category: String) {
        val user = currentUserFlow.value ?: return
        viewModelScope.launch {
            val id = shoppingRepository.createList(
                ownerId = user.id,
                ownerEmail = user.email,
                title = title,
                category = category
            )
            _selectedListId.value = id
            _actionMessage.value = ShoppingActionMessage.Success("Lista criada com sucesso!")
        }
    }

    fun updateListDetails(listId: Long, title: String, category: String) {
        viewModelScope.launch {
            shoppingRepository.updateListTitleAndCategory(listId, title, category)
            _actionMessage.value = ShoppingActionMessage.Success("Lista atualizada!")
        }
    }

    fun toggleArchiveList(listId: Long) {
        viewModelScope.launch {
            shoppingRepository.toggleArchiveList(listId)
            _actionMessage.value = ShoppingActionMessage.Success("Status da lista atualizado!")
        }
    }

    fun deleteList(listId: Long) {
        val user = currentUserFlow.value ?: return
        viewModelScope.launch {
            val result = shoppingRepository.deleteList(listId, user.id)
            result.onSuccess {
                if (_selectedListId.value == listId) {
                    _selectedListId.value = null
                }
                _actionMessage.value = ShoppingActionMessage.Success("Lista excluída.")
            }.onFailure {
                _actionMessage.value = ShoppingActionMessage.Error(it.localizedMessage ?: "Erro ao excluir.")
            }
        }
    }

    // Items
    fun addItem(name: String, quantity: Double, unitPrice: Double, unit: String = "un") {
        val listId = _selectedListId.value ?: return
        viewModelScope.launch {
            val res = shoppingRepository.addItem(listId, name, quantity, unitPrice, unit)
            res.onSuccess {
                _actionMessage.value = ShoppingActionMessage.Success("Item '$name' adicionado!")
            }.onFailure {
                _actionMessage.value = ShoppingActionMessage.Error(it.localizedMessage ?: "Erro ao adicionar item.")
            }
        }
    }

    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            val res = shoppingRepository.updateExistingItem(item)
            res.onSuccess {
                _actionMessage.value = ShoppingActionMessage.Success("Item atualizado!")
            }.onFailure {
                _actionMessage.value = ShoppingActionMessage.Error(it.localizedMessage ?: "Erro ao atualizar item.")
            }
        }
    }

    fun togglePurchased(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingRepository.toggleItemPurchased(item)
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            shoppingRepository.deleteItem(itemId)
            _actionMessage.value = ShoppingActionMessage.Success("Item removido.")
        }
    }

    // Collaborators
    fun shareList(targetEmail: String, canEdit: Boolean = true) {
        val listId = _selectedListId.value ?: return
        val user = currentUserFlow.value ?: return
        viewModelScope.launch {
            val res = shoppingRepository.shareList(
                listId = listId,
                currentUserId = user.id,
                targetEmail = targetEmail,
                canEdit = canEdit
            )
            res.onSuccess {
                _actionMessage.value = ShoppingActionMessage.Success("Lista compartilhada com $targetEmail!")
            }.onFailure {
                _actionMessage.value = ShoppingActionMessage.Error(it.localizedMessage ?: "Erro ao compartilhar.")
            }
        }
    }

    fun removeCollaborator(targetEmail: String) {
        val listId = _selectedListId.value ?: return
        val user = currentUserFlow.value ?: return
        viewModelScope.launch {
            val res = shoppingRepository.removeCollaborator(
                listId = listId,
                currentUserId = user.id,
                targetEmail = targetEmail
            )
            res.onSuccess {
                _actionMessage.value = ShoppingActionMessage.Success("Acesso de $targetEmail removido.")
            }.onFailure {
                _actionMessage.value = ShoppingActionMessage.Error(it.localizedMessage ?: "Erro ao remover permissão.")
            }
        }
    }

    // Cashier verification:
    // User types the machine value.
    // Checks against expectedTotal (either total of all items or total of checked items).
    fun performCashierConference(
        cashierValue: Double,
        useOnlyPurchasedItems: Boolean = false
    ) {
        val listId = _selectedListId.value ?: return
        val items = currentItems.value
        val targetTotal = if (useOnlyPurchasedItems) {
            items.filter { it.isPurchased }.sumOf { it.totalValue }
        } else {
            items.sumOf { it.totalValue }
        }

        val diff = cashierValue - targetTotal
        val absDiff = abs(diff)

        val result = when {
            absDiff < 0.01 -> {
                CashierConferenceResult(
                    status = CashierStatus.MATCH,
                    cashierAmount = cashierValue,
                    expectedAmount = targetTotal,
                    difference = 0.0,
                    badgeMessage = "Valores batem! Diferença: R$ 0,00",
                    detailMessage = "O valor cobrado na máquina do supermercado confere exatamente com o cálculo da sua lista de compras. Tudo certinho para pagar!",
                    checkedAt = System.currentTimeMillis()
                )
            }
            diff > 0.00 -> {
                // Sobra no caixa (caixa maior que o esperado -> cobrando a mais)
                val formattedDiff = CurrencyUtils.formatBrl(absDiff)
                CashierConferenceResult(
                    status = CashierStatus.SURPLUS,
                    cashierAmount = cashierValue,
                    expectedAmount = targetTotal,
                    difference = absDiff,
                    badgeMessage = "Sobra $formattedDiff",
                    detailMessage = "Atenção: A máquina do caixa cobrou $formattedDiff a mais do que o calculado na sua lista. Verifique na nota fiscal se algum produto passou com preço diferente ou foi bipado duas vezes!",
                    checkedAt = System.currentTimeMillis()
                )
            }
            else -> {
                // Falta no caixa (caixa menor que o esperado -> economia ou desconto)
                val formattedDiff = CurrencyUtils.formatBrl(absDiff)
                CashierConferenceResult(
                    status = CashierStatus.SHORTAGE,
                    cashierAmount = cashierValue,
                    expectedAmount = targetTotal,
                    difference = absDiff,
                    badgeMessage = "Falta $formattedDiff",
                    detailMessage = "A máquina cobrou $formattedDiff a menos que o valor previsto na sua lista. Isso pode significar desconto especial do mercado, promoção do dia ou algum item que não passou.",
                    checkedAt = System.currentTimeMillis()
                )
            }
        }

        _conferenceResult.value = result

        // Save audit
        viewModelScope.launch {
            shoppingRepository.recordCashierAudit(
                listId = listId,
                cashierAmount = cashierValue,
                calculatedTotal = targetTotal,
                statusText = result.badgeMessage
            )
        }
    }

    fun dismissConference() {
        _conferenceResult.value = null
    }

    class Factory(
        private val shoppingRepository: ShoppingRepository,
        private val currentUserFlow: StateFlow<UserAccount?>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShoppingViewModel(shoppingRepository, currentUserFlow) as T
        }
    }
}
