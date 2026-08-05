package zhiqiu.app.mk

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.Navigator
import zhiqiu.app.mk.ui.home.HomeScreen
import zhiqiu.app.mk.ui.theme.AppTheme

@Composable
@Preview
fun App() {
    AppTheme {
        Navigator(HomeScreen())
    }
}
