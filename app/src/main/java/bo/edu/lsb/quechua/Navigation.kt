package bo.edu.lsb.quechua

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import bo.edu.lsb.quechua.engine.MultimodalMode
import bo.edu.lsb.quechua.ui.screens.CameraVisionScreen
import bo.edu.lsb.quechua.ui.screens.HomeScreen
import bo.edu.lsb.quechua.ui.screens.LexiconScreen
import bo.edu.lsb.quechua.ui.screens.SplashScreen
import bo.edu.lsb.quechua.ui.screens.TestProtocolScreen
import bo.edu.lsb.quechua.ui.screens.TranslatorScreen
import bo.edu.lsb.quechua.viewmodel.AppStartMode
import bo.edu.lsb.quechua.viewmodel.MainViewModel

/**
 * Grafo de Navegación de la Aplicación.
 * Gestiona la pila de pantallas (BackStack) y el arranque directo configurable:
 * - Splash (Carga inicial si el inicio es Preguntar)
 * - Home (Dashboard minimalista y selección de modo)
 * - Translator (Guante BLE + Motor Morfológico + TTS)
 * - CameraVision (Cámara MediaPipe 3D + Híbrido + Teleprompter)
 * - Lexicon (Catálogo de 146 glosas y sufijos)
 * - TestProtocol (Protocolo de pruebas Fase 7)
 */
@Composable
fun MainNavigation(viewModel: MainViewModel) {
    val startMode = remember { viewModel.appStartMode.value }
    val backStack = when (startMode) {
        AppStartMode.ASK_ON_STARTUP -> rememberNavBackStack(SplashKey)
        AppStartMode.GLOVE -> rememberNavBackStack(HomeKey, TranslatorKey)
        AppStartMode.CAMERA, AppStartMode.HYBRID -> rememberNavBackStack(HomeKey, CameraVisionKey)
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            } else if (backStack.lastOrNull() != HomeKey) {
                backStack.add(HomeKey)
            }
        },
        entryProvider = entryProvider {
            entry<SplashKey> {
                SplashScreen(
                    onContinue = {
                        backStack.removeLastOrNull()
                        if (backStack.isEmpty()) {
                            backStack.add(HomeKey)
                        }
                    }
                )
            }

            entry<HomeKey> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTranslator = {
                        viewModel.setMultimodalMode(MultimodalMode.GLOVE_ONLY)
                        if (backStack.lastOrNull() != TranslatorKey) {
                            backStack.add(TranslatorKey)
                        }
                    },
                    onNavigateToCamera = {
                        viewModel.setMultimodalMode(MultimodalMode.CAMERA_ONLY)
                        if (backStack.lastOrNull() != CameraVisionKey) {
                            backStack.add(CameraVisionKey)
                        }
                    },
                    onNavigateToHybrid = {
                        viewModel.setMultimodalMode(MultimodalMode.HYBRID_FUSION)
                        if (backStack.lastOrNull() != CameraVisionKey) {
                            backStack.add(CameraVisionKey)
                        }
                    },
                    onNavigateToLexicon = { backStack.add(LexiconKey) },
                    onNavigateToTestProtocol = { backStack.add(TestProtocolKey) }
                )
            }

            entry<TranslatorKey> {
                TranslatorScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        if (backStack.size > 1) backStack.removeLastOrNull()
                    },
                    onSwitchToCamera = { targetMode ->
                        viewModel.setMultimodalMode(targetMode)
                        if (backStack.lastOrNull() == TranslatorKey) {
                            backStack.removeLastOrNull()
                        }
                        if (backStack.lastOrNull() != CameraVisionKey) {
                            backStack.add(CameraVisionKey)
                        }
                    }
                )
            }

            entry<CameraVisionKey> {
                CameraVisionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        if (backStack.size > 1) backStack.removeLastOrNull()
                    },
                    onSwitchToGlove = {
                        viewModel.setMultimodalMode(MultimodalMode.GLOVE_ONLY)
                        if (backStack.lastOrNull() == CameraVisionKey) {
                            backStack.removeLastOrNull()
                        }
                        if (backStack.lastOrNull() != TranslatorKey) {
                            backStack.add(TranslatorKey)
                        }
                    }
                )
            }

            entry<LexiconKey> {
                LexiconScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<TestProtocolKey> {
                TestProtocolScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
