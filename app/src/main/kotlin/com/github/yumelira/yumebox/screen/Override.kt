package com.github.yumelira.yumebox.screen

import androidx.compose.runtime.Composable
import com.github.yumelira.yumebox.presentation.screen.OverrideContent
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.KeyValueEditorScreenDestination
import com.ramcosta.composedestinations.generated.destinations.StringListEditorScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.oom_wg.purejoy.mlang.MLang

@Composable
@Destination<RootGraph>
fun OverrideScreen(navigator: DestinationsNavigator) {
    OverrideContent(
        navigator = navigator,
        onEditStringList = { title, placeholder, value, onValueChange ->
            EditorDataHolder.setupListEditor(
                title = title,
                placeholder = placeholder,
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(StringListEditorScreenDestination)
        },
        onEditStringMap = { title, keyPlaceholder, valuePlaceholder, value, onValueChange ->
            EditorDataHolder.setupMapEditor(
                title = title,
                keyPlaceholder = keyPlaceholder,
                valuePlaceholder = valuePlaceholder,
                items = value,
                callback = onValueChange,
            )
            navigator.navigate(KeyValueEditorScreenDestination)
        },
    )
}
