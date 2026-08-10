package zhiqiu.app.mk.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.stringResource
import screenmask.shared.generated.resources.Res
import screenmask.shared.generated.resources.back
import screenmask.shared.generated.resources.ball_click_confirm_description
import screenmask.shared.generated.resources.ball_click_confirm_title
import screenmask.shared.generated.resources.cancel
import screenmask.shared.generated.resources.exit_method_four_finger_description
import screenmask.shared.generated.resources.exit_method_four_finger_title
import screenmask.shared.generated.resources.exit_method_draw_circle_description
import screenmask.shared.generated.resources.exit_method_draw_circle_title
import screenmask.shared.generated.resources.exit_methods_description
import screenmask.shared.generated.resources.exit_methods_section_title
import screenmask.shared.generated.resources.exit_method_swipe_up_description
import screenmask.shared.generated.resources.exit_method_swipe_up_title
import screenmask.shared.generated.resources.float_ball_description
import screenmask.shared.generated.resources.float_ball_title
import screenmask.shared.generated.resources.keep_alive_foreground_description
import screenmask.shared.generated.resources.keep_alive_foreground_title
import screenmask.shared.generated.resources.keep_alive_section_title
import screenmask.shared.generated.resources.keep_alive_wakelock_description
import screenmask.shared.generated.resources.keep_alive_wakelock_title
import screenmask.shared.generated.resources.options_section_title
import screenmask.shared.generated.resources.settings_language
import screenmask.shared.generated.resources.settings_language_chinese
import screenmask.shared.generated.resources.settings_language_english
import screenmask.shared.generated.resources.settings_language_follow_system
import screenmask.shared.generated.resources.settings_title
import zhiqiu.app.mk.settings.AppSettings
import zhiqiu.app.mk.ui.BlackScreenConfirmDialog
import zhiqiu.app.mk.ui.UnsupportedNotice
import zhiqiu.app.mk.ui.rememberBlackScreenViewModel
import zhiqiu.app.mk.ui.theme.appSwitchColors
import zhiqiu.app.mk.util.LanguageCodes
import zhiqiu.app.mk.util.setLanguage

/** Settings page reached from the home screen top bar. */
class SettingsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberBlackScreenViewModel()
        val settings = remember { AppSettings() }

        if (!viewModel.supported) {
            UnsupportedNotice()
            return
        }

        var showFloatBall by remember { mutableStateOf(settings.showFloatingBall) }
        var ballConfirm by remember { mutableStateOf(settings.ballClickConfirm) }
        var fourFinger by remember { mutableStateOf(settings.exitMethodFourFinger) }
        var swipeUp by remember { mutableStateOf(settings.exitMethodSwipeUp) }
        var drawCircle by remember { mutableStateOf(settings.exitMethodDrawCircle) }
        var keepAliveForeground by remember { mutableStateOf(settings.keepAliveForeground) }
        var keepAliveWakeLock by remember { mutableStateOf(settings.keepAliveWakeLock) }
        var showLanguageDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings_title)) },
                    navigationIcon = {
                        TextButton(onClick = { navigator.pop() }) {
                            Text(stringResource(Res.string.back))
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
                Text(
                    text = stringResource(Res.string.exit_methods_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.exit_methods_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            title = stringResource(Res.string.exit_method_four_finger_title),
                            description = stringResource(Res.string.exit_method_four_finger_description),
                            checked = fourFinger,
                            onCheckedChange = { checked ->
                                fourFinger = checked
                                settings.exitMethodFourFinger = checked
                            }
                        )
                        SettingsSwitchRow(
                            title = stringResource(Res.string.exit_method_swipe_up_title),
                            description = stringResource(Res.string.exit_method_swipe_up_description),
                            checked = swipeUp,
                            onCheckedChange = { checked ->
                                swipeUp = checked
                                settings.exitMethodSwipeUp = checked
                            }
                        )
                        SettingsSwitchRow(
                            title = stringResource(Res.string.exit_method_draw_circle_title),
                            description = stringResource(Res.string.exit_method_draw_circle_description),
                            checked = drawCircle,
                            onCheckedChange = { checked ->
                                drawCircle = checked
                                settings.exitMethodDrawCircle = checked
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.options_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        SettingItem(
                            title = stringResource(Res.string.settings_language),
                            summary = when (settings.language) {
                                AppSettings.VALUE_LANG_ZH_CN ->
                                    stringResource(Res.string.settings_language_chinese)
                                AppSettings.VALUE_LANG_EN ->
                                    stringResource(Res.string.settings_language_english)
                                else ->
                                    stringResource(Res.string.settings_language_follow_system)
                            },
                            onClick = { showLanguageDialog = true }
                        )
                        SettingsSwitchRow(
                            title = stringResource(Res.string.float_ball_title),
                            description = stringResource(Res.string.float_ball_description),
                            checked = showFloatBall,
                            onCheckedChange = { checked ->
                                showFloatBall = checked
                                settings.showFloatingBall = checked
                                viewModel.setFloatingBallVisible(checked)
                            }
                        )
                        SettingsSwitchRow(
                            title = stringResource(Res.string.ball_click_confirm_title),
                            description = stringResource(Res.string.ball_click_confirm_description),
                            checked = ballConfirm,
                            onCheckedChange = { checked ->
                                ballConfirm = checked
                                settings.ballClickConfirm = checked
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(Res.string.keep_alive_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            title = stringResource(Res.string.keep_alive_foreground_title),
                            description = stringResource(Res.string.keep_alive_foreground_description),
                            checked = keepAliveForeground,
                            onCheckedChange = { checked ->
                                keepAliveForeground = checked
                                settings.keepAliveForeground = checked
                                viewModel.applyKeepAlive()
                            }
                        )
                        SettingsSwitchRow(
                            title = stringResource(Res.string.keep_alive_wakelock_title),
                            description = stringResource(Res.string.keep_alive_wakelock_description),
                            checked = keepAliveWakeLock,
                            onCheckedChange = { checked ->
                                keepAliveWakeLock = checked
                                settings.keepAliveWakeLock = checked
                                viewModel.applyKeepAlive()
                            }
                        )
                    }
                }
            }
        }

        BlackScreenConfirmDialog(viewModel)

        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLang = settings.language,
                onSelect = { lang ->
                    settings.language = lang
                    setLanguage(
                        when (lang) {
                            AppSettings.VALUE_LANG_ZH_CN -> LanguageCodes.ZH_CN
                            AppSettings.VALUE_LANG_EN -> LanguageCodes.EN
                            AppSettings.VALUE_FOLLOW_SYSTEM -> null
                            else -> lang
                        }
                    )
                    showLanguageDialog = false
                },
                onDismiss = { showLanguageDialog = false }
            )
        }
    }
}

/** A labelled switch row used on the settings page. */
@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = appSwitchColors()
        )
    }
}

/** A clickable labelled row used on the settings page. */
@Composable
private fun SettingItem(
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLang: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_language)) },
        text = {
            Column {
                LanguageDialogOption(
                    text = stringResource(Res.string.settings_language_follow_system),
                    selected = currentLang == AppSettings.VALUE_FOLLOW_SYSTEM,
                    onClick = { onSelect(AppSettings.VALUE_FOLLOW_SYSTEM) }
                )
                LanguageDialogOption(
                    text = stringResource(Res.string.settings_language_chinese),
                    selected = currentLang == AppSettings.VALUE_LANG_ZH_CN,
                    onClick = { onSelect(AppSettings.VALUE_LANG_ZH_CN) }
                )
                LanguageDialogOption(
                    text = stringResource(Res.string.settings_language_english),
                    selected = currentLang == AppSettings.VALUE_LANG_EN,
                    onClick = { onSelect(AppSettings.VALUE_LANG_EN) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun LanguageDialogOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // null because Row handles the click
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
