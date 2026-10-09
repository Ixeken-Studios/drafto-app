package com.ixeken.drafto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.ixeken.drafto.ui.theme.BorderWidthThin
import com.ixeken.drafto.ui.theme.DraftoShapeCardSmall
import com.ixeken.drafto.ui.theme.DraftoShapeCircle
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.EditorBulletDotSize
import com.ixeken.drafto.ui.theme.EditorQuoteBarHeight
import com.ixeken.drafto.ui.theme.EditorQuoteBarWidth
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.utils.formatInlineMarkdown

/**
 * Representación inmutable de los bloques semánticos de sintaxis Markdown soportados.
 * Anotada con [Immutable] para garantizar smart skipping en compose-compiler y renderizado a 120 FPS.
 */
@Immutable
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String? = null) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data object EmptyLine : MarkdownBlock
}

/**
 * Analiza una cadena de texto sin procesar y la transforma en una lista inmutable de bloques Markdown.
 * Esta función es pura y no genera asignaciones pesadas de objetos en el ciclo de recomposición.
 */
fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    if (rawText.isBlank()) return emptyList()

    val blocks = ArrayList<MarkdownBlock>()
    val lines = rawText.lines()
    var inCodeBlock = false
    val codeBlockBuilder = StringBuilder()
    var codeBlockLang: String? = null

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(codeBlockBuilder.toString().trimEnd(), codeBlockLang))
                codeBlockBuilder.clear()
                codeBlockLang = null
                inCodeBlock = false
            } else {
                inCodeBlock = true
                codeBlockLang = trimmed.removePrefix("```").trim().ifEmpty { null }
            }
            continue
        }

        if (inCodeBlock) {
            codeBlockBuilder.append(line).append("\n")
            continue
        }

        when {
            trimmed.isEmpty() -> {
                if (blocks.isNotEmpty() && blocks.lastOrNull() !is MarkdownBlock.EmptyLine) {
                    blocks.add(MarkdownBlock.EmptyLine)
                }
            }
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Heading(level = 3, text = trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Heading(level = 2, text = trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Heading(level = 1, text = trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("#### ") -> {
                blocks.add(MarkdownBlock.Heading(level = 4, text = trimmed.removePrefix("#### ").trim()))
            }
            trimmed.startsWith("> ") || trimmed == ">" -> {
                blocks.add(MarkdownBlock.Blockquote(text = trimmed.removePrefix(">").trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                blocks.add(MarkdownBlock.BulletItem(text = trimmed.substring(2).trim()))
            }
            else -> {
                blocks.add(MarkdownBlock.Paragraph(text = line))
            }
        }
    }

    if (inCodeBlock && codeBlockBuilder.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeBlockBuilder.toString().trimEnd(), codeBlockLang))
    }

    return blocks
}

/**
 * Componente modular centralizado para visualizar contenido Markdown con diseño Nothing OS.
 * Soporta deduplicación automática de encabezado si coincide con el título principal de la nota.
 *
 * @param content Cadena de texto en formato Markdown a renderizar.
 * @param modifier Modificador visual para el contenedor raíz.
 * @param titleToDeduplicate Título opcional para evitar que un encabezado idéntico se repita al inicio.
 */
@Composable
fun DraftoMarkdownContent(
    content: String,
    modifier: Modifier = Modifier,
    titleToDeduplicate: String? = null
) {
    val blocks = remember(content, titleToDeduplicate) {
        val parsed = parseMarkdownBlocks(content)
        val trimmedTitle = titleToDeduplicate?.trim()
        if (!trimmedTitle.isNullOrBlank() && parsed.isNotEmpty()) {
            val firstBlock = parsed.first()
            val isDuplicate = when (firstBlock) {
                is MarkdownBlock.Heading -> firstBlock.text.trim().equals(trimmedTitle, ignoreCase = true)
                is MarkdownBlock.Paragraph -> firstBlock.text.trim().equals(trimmedTitle, ignoreCase = true)
                else -> false
            }
            if (isDuplicate) {
                val remaining = parsed.drop(1)
                if (remaining.firstOrNull() is MarkdownBlock.EmptyLine) {
                    remaining.drop(1)
                } else {
                    remaining
                }
            } else {
                parsed
            }
        } else {
            parsed
        }
    }

    MarkdownBlocksRenderer(
        blocks = blocks,
        modifier = modifier
    )
}

/**
 * Renderizador visual para la lista de bloques Markdown analizados.
 * Desacoplado para uso autónomo o integrado en editores y previsualizaciones.
 */
@Composable
fun MarkdownBlocksRenderer(
    blocks: List<MarkdownBlock>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PaddingSmall)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Heading -> {
                    val headingStyle = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        2 -> MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        3 -> MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                        else -> MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = formatInlineMarkdown(
                            text = block.text,
                            codeBackground = DraftoTheme.colors.cardSurface,
                            codeColor = DraftoTheme.colors.accent
                        ),
                        style = headingStyle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DraftoShapeCardSmall)
                            .background(DraftoTheme.colors.cardSurface.copy(alpha = 0.5f))
                            .padding(horizontal = PaddingMedium, vertical = PaddingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(EditorQuoteBarWidth)
                                .height(EditorQuoteBarHeight)
                                .clip(DraftoShapePill)
                                .background(DraftoTheme.colors.accent)
                        )
                        Spacer(modifier = Modifier.width(PaddingSmall))
                        Text(
                            text = formatInlineMarkdown(
                                text = block.text,
                                codeBackground = DraftoTheme.colors.cardSurface,
                                codeColor = DraftoTheme.colors.accent
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = PoppinsFontFamily,
                                fontStyle = FontStyle.Italic
                            ),
                            color = DraftoTheme.colors.textSecondary
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DraftoShapeCardSmall)
                            .background(DraftoTheme.colors.cardSurface)
                            .border(BorderWidthThin, DraftoTheme.colors.cardBorder, DraftoShapeCardSmall)
                            .padding(PaddingMedium)
                    ) {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = DraftoTheme.colors.accent
                            )
                        )
                    }
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = PaddingSmall),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = PaddingSmall)
                                .size(EditorBulletDotSize)
                                .clip(DraftoShapeCircle)
                                .background(DraftoTheme.colors.accent)
                        )
                        Text(
                            text = formatInlineMarkdown(
                                text = block.text,
                                codeBackground = DraftoTheme.colors.cardSurface,
                                codeColor = DraftoTheme.colors.accent
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = PoppinsFontFamily,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(
                            text = block.text,
                            codeBackground = DraftoTheme.colors.cardSurface,
                            codeColor = DraftoTheme.colors.accent
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = PoppinsFontFamily,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is MarkdownBlock.EmptyLine -> {
                    Spacer(modifier = Modifier.height(PaddingSmall))
                }
            }
        }
    }
}
