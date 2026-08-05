package zhiqiu.app.mk.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.stringResource
import screenmask.shared.generated.resources.Res
import screenmask.shared.generated.resources.app_name
import screenmask.shared.generated.resources.black_screen_description
import screenmask.shared.generated.resources.black_screen_start
import screenmask.shared.generated.resources.black_screen_status_running
import screenmask.shared.generated.resources.black_screen_status_stopped
import screenmask.shared.generated.resources.black_screen_stop
import screenmask.shared.generated.resources.black_screen_title
import screenmask.shared.generated.resources.settings_title
import zhiqiu.app.mk.ui.BlackScreenConfirmDialog
import zhiqiu.app.mk.ui.UnsupportedNotice
import zhiqiu.app.mk.ui.rememberBlackScreenViewModel
import zhiqiu.app.mk.ui.settings.SettingsScreen

/** Landing screen: a big button to start/stop the black screen, with a link to the settings. */
class HomeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberBlackScreenViewModel()

        if (!viewModel.supported) {
            UnsupportedNotice()
            return
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.app_name)) },
                    actions = {
                        TextButton(onClick = { navigator.push(SettingsScreen()) }) {
                            Text(stringResource(Res.string.settings_title))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.black_screen_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.black_screen_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        if (viewModel.isRunning) {
                            OutlinedButton(onClick = { viewModel.onToggle(false) }) {
                                Text(stringResource(Res.string.black_screen_stop))
                            }
                        } else {
                            Button(onClick = { viewModel.onToggle(true) }) {
                                Text(stringResource(Res.string.black_screen_start))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(
                                if (viewModel.isRunning) {
                                    Res.string.black_screen_status_running
                                } else {
                                    Res.string.black_screen_status_stopped
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (viewModel.isRunning) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        viewModel.errorMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        BlackScreenConfirmDialog(viewModel)
    }
}
