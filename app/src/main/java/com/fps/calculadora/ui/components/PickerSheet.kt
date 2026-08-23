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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius

/**
 * Uma opção da folha de seleção.
 *
 * @param trailing texto à direita — VRAM da GPU, socket da CPU, o que ajudar a
 *   escolher sem abrir outra tela.
 */
data class PickerOption(
    val id: String,
    val name: String,
    val group: String,
    val trailing: String = "",
)

private data class PickerRow(val option: PickerOption, val startsGroup: Boolean)

/**
 * `openSheet()` (`index.html:2358`) — folha de seleção com busca e itens
 * agrupados. Usada por CPU, placa-mãe, RAM, GPU e jogo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSheet(
    title: String,
    options: List<PickerOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    searchable: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val filtered = remember(options, query) {
        if (query.isBlank()) {
            options
        } else {
            options.filter { it.name.contains(query, ignoreCase = true) ||
                it.group.contains(query, ignoreCase = true) }
        }
    }

    // O cabeçalho de grupo é decidido aqui, e não dentro do `items`: a LazyColumn
    // compõe fora de ordem, então um acumulador dentro do laço mentiria.
    val rows = remember(filtered) {
        filtered.mapIndexed { index, option ->
            PickerRow(option, startsGroup = index == 0 || filtered[index - 1].group != option.group)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FpsColors.Bg1,
        contentColor = FpsColors.Tx1,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                title,
                color = FpsColors.Tx1,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (searchable) {
                SearchField(query) { query = it }
            }
        }

        LazyColumn(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "Nada encontrado para “$query”",
                        Modifier.fillMaxWidth().padding(24.dp),
                        color = FpsColors.Tx3,
                        fontSize = 12.sp,
                    )
                }
            }
            items(rows, key = { it.option.id }) { row ->
                if (row.startsGroup) GroupHeader(row.option.group)
                OptionRow(
                    option = row.option,
                    selected = row.option.id == selectedId,
                    onClick = { onSelect(row.option.id) },
                )
            }
            item { Box(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(FpsColors.Bg0)
            .border(1.dp, FpsColors.Line2, RoundedCornerShape(FpsRadius.Md))
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        if (query.isEmpty()) {
            Text("Buscar…", color = FpsColors.Tx3, fontSize = 13.sp)
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = FpsColors.Tx1, fontSize = 13.sp),
            cursorBrush = SolidColor(FpsColors.Acc),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GroupHeader(group: String) {
    Text(
        group.uppercase(),
        Modifier
            .fillMaxWidth()
            .background(FpsColors.Bg1)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        color = FpsColors.Tx3,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.em,
    )
}

@Composable
private fun OptionRow(option: PickerOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (selected) FpsColors.AccSoft else FpsColors.Bg1)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            option.name,
            Modifier.weight(1f),
            color = if (selected) FpsColors.Acc else FpsColors.Tx1,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (option.trailing.isNotEmpty()) {
            MonoLabel(option.trailing, size = 10.5.sp)
        }
        if (selected) {
            SvgIcon(
                "m5 12.5 5 5L19 7",
                Modifier.size(15.dp),
                tint = FpsColors.Acc,
                strokeWidth = 2.2f,
            )
        }
    }
}
