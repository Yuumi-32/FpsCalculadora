package com.fps.calculadora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fps.calculadora.core.PcProfile
import com.fps.calculadora.core.ProfileBook
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius

/**
 * "Perfis de PC" — porta o `openProfilesSheet()` do `index.html` (:3411).
 *
 * Um perfil é um PC inteiro guardado: o da pessoa, o do irmão, o que ela pensa
 * em montar. Diferente do histórico, que é um retrato datado, o perfil ativo
 * acompanha tudo o que muda na calculadora — por isso a folha só troca de
 * perfil, renomeia e exclui; não existe "salvar".
 *
 * Excluir pede dois toques na lixeira em vez de um diálogo de confirmação: é
 * uma folha, e abrir um diálogo por cima dela para uma ação local seria pesado
 * demais para o tamanho do estrago.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesSheet(
    book: ProfileBook,
    subtitle: (PcProfile) -> String,
    onSwitch: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingId by remember { mutableStateOf<Long?>(null) }
    var confirmingDeleteId by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FpsColors.Bg1,
        contentColor = FpsColors.Tx1,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                "Perfis de PC",
                color = FpsColors.Tx1,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            FpsCard {
                for (profile in book.list) {
                    if (profile.id == editingId) {
                        RenameRow(
                            active = profile.id == book.activeId,
                            initialName = profile.name,
                            onConfirm = { name ->
                                onRename(profile.id, name)
                                editingId = null
                            },
                        )
                    } else {
                        ProfileRow(
                            name = profile.name,
                            subtitle = subtitle(profile),
                            active = profile.id == book.activeId,
                            // A lixeira some quando só resta um perfil: não há
                            // estado "sem perfil nenhum" para cair.
                            deletable = book.list.size > 1,
                            confirmingDelete = profile.id == confirmingDeleteId,
                            onSwitch = {
                                onSwitch(profile.id)
                                onDismiss()
                            },
                            onRename = {
                                editingId = profile.id
                                confirmingDeleteId = null
                            },
                            onDelete = {
                                if (confirmingDeleteId == profile.id) {
                                    onDelete(profile.id)
                                    confirmingDeleteId = null
                                } else {
                                    confirmingDeleteId = profile.id
                                }
                            },
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(FpsRadius.Md))
                    .background(FpsColors.Bg2)
                    .border(1.dp, FpsColors.Line2, RoundedCornerShape(FpsRadius.Md))
                    .clickable {
                        onNew()
                        onDismiss()
                    }
                    .padding(13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+ Novo perfil (cópia do atual)",
                    color = FpsColors.Tx1,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                "O perfil ativo guarda automaticamente tudo o que você mudar na " +
                    "calculadora. Para excluir, toque duas vezes na lixeira.",
                color = FpsColors.Tx3,
                fontSize = 10.5.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Box(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileRow(
    name: String,
    subtitle: String,
    active: Boolean,
    deletable: Boolean,
    confirmingDelete: Boolean,
    onSwitch: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActiveDot(active)
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSwitch)
                .padding(vertical = 8.dp),
        ) {
            Text(
                name,
                color = if (active) FpsColors.Acc else FpsColors.Tx1,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = FpsColors.Tx3,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        IconAction(Icons.PENCIL, "Renomear", FpsColors.Tx2, onRename)
        if (deletable) {
            IconAction(
                path = if (confirmingDelete) Icons.CHECK else Icons.TRASH,
                description = if (confirmingDelete) "Confirmar exclusão" else "Excluir",
                tint = FpsColors.Bad,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun RenameRow(active: Boolean, initialName: String, onConfirm: (String) -> Unit) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActiveDot(active)
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(FpsRadius.Sm))
                .background(FpsColors.Bg0)
                .border(1.dp, FpsColors.Line2, RoundedCornerShape(FpsRadius.Sm))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(ProfileBook.NAME_MAX_LENGTH) },
                textStyle = TextStyle(color = FpsColors.Tx1, fontSize = 13.sp),
                cursorBrush = SolidColor(FpsColors.Acc),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
        IconAction(Icons.CHECK, "Salvar nome", FpsColors.Ok) { onConfirm(name) }
    }
}

@Composable
private fun ActiveDot(active: Boolean) {
    Box(
        Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(if (active) FpsColors.Acc else FpsColors.Line2),
    )
}

@Composable
private fun IconAction(path: String, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SvgIcon(path, Modifier.size(16.dp), tint = tint, strokeWidth = 1.8f)
    }
}
