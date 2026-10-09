package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.ShoppingRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.detail.ListDetailScreen
import com.example.ui.lists.ListOverviewScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.ShoppingViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        val authRepo = AuthRepository(
            userDao = db.userDao(),
            shoppingDao = db.shoppingDao(),
            context = applicationContext
        )
        AuthViewModel.Factory(authRepo)
    }

    private val shoppingViewModel: ShoppingViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        val shoppingRepo = ShoppingRepository(
            shoppingDao = db.shoppingDao(),
            userDao = db.userDao()
        )
        ShoppingViewModel.Factory(
            shoppingRepository = shoppingRepo,
            currentUserFlow = authViewModel.currentUser
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationRoot(
                        authViewModel = authViewModel,
                        shoppingViewModel = shoppingViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigationRoot(
    authViewModel: AuthViewModel,
    shoppingViewModel: ShoppingViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val selectedListId by shoppingViewModel.selectedListId.collectAsStateWithLifecycle()

    val user = currentUser
    if (user == null) {
        AuthScreen(
            authViewModel = authViewModel,
            modifier = modifier
        )
    } else {
        if (selectedListId != null) {
            ListDetailScreen(
                currentUser = user,
                shoppingViewModel = shoppingViewModel,
                onBack = {
                    shoppingViewModel.selectList(null)
                },
                modifier = modifier
            )
        } else {
            ListOverviewScreen(
                currentUser = user,
                shoppingViewModel = shoppingViewModel,
                authViewModel = authViewModel,
                onSelectList = { listId ->
                    shoppingViewModel.selectList(listId)
                },
                modifier = modifier
            )
        }
    }
}
