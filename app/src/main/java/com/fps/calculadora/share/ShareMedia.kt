package com.fps.calculadora.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** Pasta da galeria em que o PNG aparece — a mesma que a política de privacidade declara. */
private const val GALLERY_FOLDER = "FPS Calculadora"

/** Subpasta do cache que o `FileProvider` publica — ver `res/xml/file_paths.xml`. */
private const val SHARE_CACHE_DIR = "compartilhados"

/**
 * Salva o card na galeria de fotos do aparelho, em *Imagens → FPS Calculadora*.
 *
 * Só a partir do Android 10 (API 29): dali em diante o `MediaStore` deixa um app
 * escrever na própria pasta sem pedir permissão nenhuma. Antes disso seria
 * preciso `WRITE_EXTERNAL_STORAGE` — uma permissão de disco inteiro para salvar
 * uma figura, o que é caro demais para o que o recurso entrega. Nos aparelhos
 * mais antigos sobra o compartilhamento, que não depende de permissão.
 *
 * @return `true` quando o arquivo entrou na galeria.
 */
fun saveCardToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "${Environment.DIRECTORY_PICTURES}/$GALLERY_FOLDER",
        )
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    return try {
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ?: error("sem stream de escrita")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        true
    } catch (e: Exception) {
        // Entrada pendente sem arquivo fica visível como uma foto quebrada na
        // galeria; apagar é mais barato que explicar.
        resolver.delete(uri, null, null)
        false
    }
}

/**
 * Manda o card (e o resumo em texto) para o seletor de compartilhamento do
 * Android — WhatsApp, Discord, o que a pessoa tiver.
 *
 * O PNG vai pelo cache do app, publicado por [FileProvider]: é a única forma de
 * passar um arquivo a outro app sem exigir permissão de armazenamento, e a
 * `Uri` concedida morre quando o outro app termina.
 *
 * @return `true` quando o seletor abriu.
 */
fun shareCard(context: Context, bitmap: Bitmap, fileName: String, text: String): Boolean {
    val uri = cacheCard(context, bitmap, fileName) ?: return false
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(
            Intent.createChooser(intent, "Compartilhar resultado")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (e: Exception) {
        false
    }
}

private fun cacheCard(context: Context, bitmap: Bitmap, fileName: String): Uri? = try {
    val dir = File(context.cacheDir, SHARE_CACHE_DIR).apply { mkdirs() }
    val file = File(dir, fileName)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
} catch (e: Exception) {
    null
}
