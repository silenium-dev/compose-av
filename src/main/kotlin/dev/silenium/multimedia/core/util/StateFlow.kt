package dev.silenium.multimedia.core.util

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.EmptyCoroutineContext

fun <T, R> StateFlow<T?>.mapState(
    coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext),
    transform: (T?) -> R,
): StateFlow<R?> {
    return map { transform(it) }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
}

@Composable
fun <T> deferredFlowStateOf(supplier: suspend () -> StateFlow<T>): State<T?> {
    var flow by remember { mutableStateOf<StateFlow<T>?>(null) }
    val state = flow?.collectAsState() ?: remember { mutableStateOf<T?>(null) }
    LaunchedEffect(supplier) {
        flow = supplier()
    }
    return state
}
