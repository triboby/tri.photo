package com.tritv.photo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Text
import com.tritv.photo.R

/** Radii: inputs, cards, panels, and the pill every button and tag uses. */
object Radius {
    val Input = RoundedCornerShape(6.dp)
    val Card = RoundedCornerShape(12.dp)
    val Panel = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

/** Horizontal safe margin for TV overscan. */
val ScreenMargin = 64.dp

enum class PillKind { Primary, Secondary, Dark }

/**
 * Pill button. Only one [PillKind.Primary] per screen; a focused [PillKind.Secondary] fills with
 * indigo so focus is always obvious from across the room.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    kind: PillKind = PillKind.Secondary,
    modifier: Modifier = Modifier,
) {
    val (container, content) = when (kind) {
        PillKind.Primary -> PhotoColors.Primary to PhotoColors.OnPrimary
        PillKind.Secondary -> PhotoColors.Canvas to PhotoColors.Primary
        PillKind.Dark -> PhotoColors.Navy to PhotoColors.OnPrimary
    }
    val focused = if (kind == PillKind.Primary) PhotoColors.PrimaryPress else PhotoColors.Primary
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        contentPadding = PaddingValues(horizontal = 28.dp),
        shape = ButtonDefaults.shape(Radius.Pill),
        colors = ButtonDefaults.colors(
            containerColor = container,
            contentColor = content,
            focusedContainerColor = focused,
            focusedContentColor = PhotoColors.OnPrimary,
            pressedContainerColor = PhotoColors.PrimaryPress,
            pressedContentColor = PhotoColors.OnPrimary,
        ),
        border = ButtonDefaults.border(
            border = if (kind == PillKind.Secondary) Border(BorderStroke(1.5.dp, PhotoColors.Primary), shape = Radius.Pill) else Border.None,
            focusedBorder = Border(BorderStroke(3.dp, PhotoColors.Subdued), inset = (-4).dp, shape = Radius.Pill),
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.04f, pressedScale = 0.97f),
        glow = ButtonDefaults.glow(glow = Glow.None, focusedGlow = Glow.None, pressedGlow = Glow.None),
    ) {
        Text(text, style = PhotoType.Button)
    }
}

/** Soft indigo tag, e.g. "3 年前" or "142 张". */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = PhotoType.Tag.tabular(),
        color = PhotoColors.PrimaryDeep,
        modifier = modifier
            .background(PhotoColors.Subdued, Radius.Pill)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** The pastel gradient band across the top third of a light screen; fades into the white canvas. */
@Composable
fun BoxScope.MeshBand() {
    Image(
        painter = painterResource(R.drawable.mesh_band),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.62f).align(Alignment.TopCenter),
    )
}

/**
 * Text field for a remote: focusing it does not open the keyboard. OK starts editing and shows
 * the keyboard; Down hides it and moves to [next]; the keyboard's Go calls [onDone].
 */
@Composable
fun LinkField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    next: FocusRequester,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(editing) { if (editing) keyboard?.show() else keyboard?.hide() }

    fun leave() {
        editing = false
        keyboard?.hide()
        runCatching { next.requestFocus() }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        readOnly = !editing,
        textStyle = PhotoType.Body.copy(color = PhotoColors.InkSecondary),
        cursorBrush = SolidColor(if (editing) PhotoColors.Primary else Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = {
            editing = false
            onDone()
        }),
        modifier = modifier
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) editing = false
            }
            .onPreviewKeyEvent { event ->
                val down = event.type == KeyEventType.KeyDown
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> when {
                        editing -> false
                        // Consume both halves of the press; start editing on release.
                        !down -> {
                            editing = true
                            true
                        }
                        else -> true
                    }
                    Key.DirectionDown -> {
                        if (down) leave()
                        true
                    }
                    else -> false
                }
            }
            .height(56.dp)
            .background(PhotoColors.Canvas, Radius.Input)
            .border(if (focused) 2.dp else 1.dp, if (focused) PhotoColors.Primary else PhotoColors.Hairline, Radius.Input)
            .padding(horizontal = 18.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = PhotoType.Body, color = PhotoColors.InkMute)
                inner()
            }
        },
    )
}

/** A row of controls with the standard 16dp gap. */
@Composable
fun ButtonRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}
