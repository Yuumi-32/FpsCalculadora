package com.fps.calculadora.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fps.calculadora.share.SHARE_CARD_HEIGHT
import com.fps.calculadora.share.SHARE_CARD_WIDTH
import com.fps.calculadora.ui.theme.FpsColors
import com.fps.calculadora.ui.theme.FpsRadius

/**
 * "Compartilhar" — porta o `shareImage()` do `index.html` (:3095).
 *
 * Mostra o card antes de mandar, de propósito: o que sai daqui vai para um
 * grupo de amigos, e ninguém deveria descobrir o que compartilhou depois de
 * compartilhar. As quatro ações cobrem os formatos que a mesma estimativa
 * assume — imagem, arquivo, texto e código importável.
 *
 * @param card `null` enquanto o PNG está sendo desenhado.
 * @param canSaveToGallery `false` abaixo do Android 10, onde salvar exigiria
 *   permissão de armazenamento — ver `saveCardToGallery`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    card: Bitmap?,
    canSaveToGallery: Boolean,
    feedback: String?,
    onShare: () -> Unit,
    onSaveToGallery: () -> Unit,
    onCopyText: () -> Unit,
    onCopyCode: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FpsColors.Bg1,
        contentColor = FpsColors.Tx1,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "Compartilhar",
                color = FpsColors.Tx1,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(SHARE_CARD_WIDTH.toFloat() / SHARE_CARD_HEIGHT)
                    .clip(RoundedCornerShape(FpsRadius.Md))
                    .background(FpsColors.Bg0)
                    .border(1.dp, FpsColors.Line, RoundedCornerShape(FpsRadius.Md)),
                contentAlignment = Alignment.Center,
            ) {
                if (card == null) {
                    Text("Desenhando o card…", color = FpsColors.Tx3, fontSize = 12.sp)
                } else {
                    Image(
                        bitmap = card.asImageBitmap(),
                        contentDescription = "Prévia do card do resultado",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            val enabled = card != null
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShareButton(
                    icon = Icons.SHARE,
                    label = "Compartilhar",
                    primary = true,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = onShare,
                )
                if (canSaveToGallery) {
                    ShareButton(
                        icon = Icons.DOWNLOAD,
                        label = "Baixar PNG",
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        onClick = onSaveToGallery,
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShareButton(
                    icon = Icons.COPY_CODE,
                    label = "Copiar texto",
                    modifier = Modifier.weight(1f),
                    onClick = onCopyText,
                )
                ShareButton(
                    icon = Icons.CODE,
                    label = "Código do build",
                    modifier = Modifier.weight(1f),
                    onClick = onCopyCode,
                )
            }

            Text(
                feedback ?: if (canSaveToGallery) {
                    "O PNG salvo vai para Imagens → FPS Calculadora na galeria."
                } else {
                    "Salvar na galeria exige Android 10 ou mais recente; " +
                        "compartilhar funciona em qualquer versão."
                },
                color = if (feedback != null) FpsColors.Ok else FpsColors.Tx3,
                fontSize = 10.5.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Box(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShareButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val tint = when {
        !enabled -> FpsColors.Tx3
        primary -> FpsColors.Acc
        else -> FpsColors.Tx1
    }
    Row(
        modifier
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(FpsRadius.Md))
            .background(if (primary) FpsColors.AccSoft else FpsColors.Bg2)
            .border(
                1.dp,
                if (primary) FpsColors.Acc.copy(alpha = 0.4f) else FpsColors.Line2,
                RoundedCornerShape(FpsRadius.Md),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        SvgIcon(icon, Modifier.size(15.dp), tint = tint, strokeWidth = 1.8f)
        Text(
            label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
