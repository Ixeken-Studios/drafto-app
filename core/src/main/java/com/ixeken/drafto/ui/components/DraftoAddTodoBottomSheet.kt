package com.ixeken.drafto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ixeken.drafto.core.R
import com.ixeken.drafto.domain.model.DraftoCollection
import com.ixeken.drafto.domain.model.TodoItem
import com.ixeken.drafto.domain.model.TodoSubtask
import com.ixeken.drafto.ui.utils.DraftoDateFormatter
import com.ixeken.drafto.ui.theme.BookmarkActionIconSize
import com.ixeken.drafto.ui.theme.BookmarkDragHandleHeight
import com.ixeken.drafto.ui.theme.BookmarkDragHandlePaddingBottom
import com.ixeken.drafto.ui.theme.BookmarkDragHandlePaddingTop
import com.ixeken.drafto.ui.theme.BookmarkDragHandleWidth
import com.ixeken.drafto.ui.theme.BookmarkFormButtonHeight
import com.ixeken.drafto.ui.theme.BookmarkFormCapsuleHeight
import com.ixeken.drafto.ui.theme.BookmarkFormSectionSpacing
import com.ixeken.drafto.ui.theme.BorderWidthThin
import com.ixeken.drafto.ui.theme.DraftoShapeBookmarkFormInput
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.FrauncesFontFamily
import com.ixeken.drafto.ui.theme.NavBarContextualButtonSize
import com.ixeken.drafto.ui.theme.PaddingExtraLarge
import com.ixeken.drafto.ui.theme.PaddingLarge
import com.ixeken.drafto.ui.theme.PaddingMedium
import com.ixeken.drafto.ui.theme.PaddingScreenHorizontal
import com.ixeken.drafto.ui.theme.PaddingSmall
import com.ixeken.drafto.ui.theme.PoppinsFontFamily
import com.ixeken.drafto.ui.theme.PureWhite

/**
 * Hoja modal interactiva para la creación y edición estructurada de listas de tareas (To-do).
 *
 * Decisiones de diseño y arquitectura:
 * - Estética Nothing OS unificada alineada al 100% con [DraftoBookmarkFormBottomSheet].
 * - Uso exclusivo de tokens centralizados de color, espaciado y curvatura.
 * - Despliegue de título editorial en [FrauncesFontFamily] centrado con manija de arrastre superior.
 * - Creación y edición interactiva de subtareas en línea sin anidar submenús modales.
 * - Eliminación del switch de fijado dentro del formulario para delegarlo exclusivamente a acciones contextuales.
 * - Botones inferiores simétricos tipo píldora (Cancelar en rojo coral y Guardar en verde esmeralda).
 * - Validación reactiva que deshabilita el guardado hasta contar con título y al menos una subtarea válida.
 *
 * @param onDismissRequest Acción para descartar o cerrar la hoja modal.
 * @param onSaveTodo Callback de compatibilidad general para persistencia básica.
 * @param modifier Modificador Compose opcional.
 * @param todoToEdit Tarea existente si se invoca en modo edición, o nulo si es una nueva lista.
 * @param availableCollections Lista de colecciones registradas disponibles para vincular.
 * @param initialCollectionId Identificador predeterminado de la colección a preseleccionar.
 * @param onSaveTodoList Callback especializado para guardar listas jerárquicas con subtareas y colección.
 * @param onSaveTodoWithCollection Callback alternativo con soporte de colección.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftoAddTodoBottomSheet(
    onDismissRequest: () -> Unit,
    onSaveTodo: (title: String, description: String, isPinned: Boolean, dueDate: Long?) -> Unit,
    modifier: Modifier = Modifier,
    todoToEdit: TodoItem? = null,
    availableCollections: List<DraftoCollection> = emptyList(),
    initialCollectionId: String? = todoToEdit?.collectionId,
    onSaveTodoList: ((title: String, description: String, isPinned: Boolean, dueDate: Long?, collectionId: String?, subtasks: List<TodoSubtask>) -> Unit)? = null,
    onSaveTodoWithCollection: ((title: String, description: String, isPinned: Boolean, dueDate: Long?, collectionId: String?) -> Unit)? = null
) {
    var title by remember(todoToEdit) { mutableStateOf(todoToEdit?.title ?: "") }
    var description by remember(todoToEdit) { mutableStateOf(todoToEdit?.description ?: "") }
    var subtasks by remember(todoToEdit) { mutableStateOf(todoToEdit?.subtasks ?: emptyList()) }
    val isPinned = remember(todoToEdit) { todoToEdit?.isPinned ?: false }
    var dueDate by remember(todoToEdit) { mutableStateOf(todoToEdit?.dueDate) }
    var selectedCollectionId by remember(todoToEdit, initialCollectionId) {
        mutableStateOf(todoToEdit?.collectionId ?: initialCollectionId)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingFocusSubtaskId by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val inputBg = DraftoTheme.colors.primaryContainer

    LaunchedEffect(todoToEdit) {
        if (todoToEdit == null) {
            focusRequester.requestFocus()
        }
    }

    // Configuración de opciones de colección en cápsula
    val isDarkTheme = DraftoTheme.colors.isDark
    val noneLabel = stringResource(R.string.bookmark_none_label)
    val collectionOptions = remember(availableCollections, noneLabel, isDarkTheme) {
        val list = ArrayList<DraftoCapsuleOption<String?>>(availableCollections.size + 1)
        list.add(
            DraftoCapsuleOption(
                key = null,
                label = noneLabel,
                icon = Icons.Rounded.Close
            )
        )
        for (col in availableCollections) {
            list.add(
                DraftoCapsuleOption(
                    key = col.id,
                    label = col.name,
                    icon = getCollectionIcon(col.iconName),
                    badgeContainerColor = parseCollectionColor(col.colorHex, isDark = isDarkTheme)
                )
            )
        }
        list
    }

    DraftoModalBottomSheet(
        onDismissRequest = onDismissRequest,
        isFullScreen = true,
        modifier = modifier
    ) {
        DraftoSheetHeader(
            title = stringResource(
                if (todoToEdit != null) R.string.title_edit_todo_list else R.string.title_new_todo_list
            ),
            modifier = Modifier.padding(horizontal = PaddingScreenHorizontal)
        )

        Spacer(modifier = Modifier.height(PaddingSmall))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = PaddingScreenHorizontal)
                .verticalScroll(rememberScrollState())
        ) {

            // 3. SECCIÓN: List name
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.todo_list_name_label),
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(PaddingSmall))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DraftoShapeBookmarkFormInput)
                        .background(inputBg)
                        .padding(horizontal = PaddingLarge, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (title.isEmpty()) {
                        Text(
                            text = stringResource(R.string.todo_list_name_placeholder),
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = DraftoTheme.colors.textMuted
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        cursorBrush = SolidColor(DraftoTheme.colors.accent),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 4. SECCIÓN: Description (Optional)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.todo_description_label),
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.bookmark_optional),
                        style = MaterialTheme.typography.labelMedium,
                        color = DraftoTheme.colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(PaddingSmall))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(DraftoShapeBookmarkFormInput)
                        .background(inputBg)
                        .padding(horizontal = PaddingLarge, vertical = 14.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (description.isEmpty()) {
                        Text(
                            text = stringResource(R.string.todo_description_placeholder),
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DraftoTheme.colors.textMuted
                        )
                    }
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        minLines = 3,
                        maxLines = 6,
                        cursorBrush = SolidColor(DraftoTheme.colors.accent),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FrauncesFontFamily,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 5. SECCIÓN: Subtasks
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.todo_subtasks_header),
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(PaddingSmall))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PaddingSmall)
                ) {
                    subtasks.forEachIndexed { index, subtask ->
                        val subtaskFocusRequester = remember(subtask.id) { FocusRequester() }
                        if (pendingFocusSubtaskId == subtask.id) {
                            LaunchedEffect(subtask.id) {
                                subtaskFocusRequester.requestFocus()
                                pendingFocusSubtaskId = null
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(DraftoShapeBookmarkFormInput)
                                .background(inputBg)
                                .padding(start = PaddingLarge, end = PaddingSmall, top = PaddingSmall, bottom = PaddingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (subtask.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.todo_add_subtask_hint),
                                        fontFamily = PoppinsFontFamily,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DraftoTheme.colors.textMuted
                                    )
                                }
                                BasicTextField(
                                    value = subtask.text,
                                    onValueChange = { newText ->
                                        subtasks = subtasks.toMutableList().also { list ->
                                            list[index] = subtask.copy(text = newText)
                                        }
                                    },
                                    singleLine = true,
                                    cursorBrush = SolidColor(DraftoTheme.colors.accent),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = PoppinsFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val nextSubtask = TodoSubtask(text = "")
                                            pendingFocusSubtaskId = nextSubtask.id
                                            subtasks = subtasks + nextSubtask
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(subtaskFocusRequester)
                                )
                            }

                            Spacer(modifier = Modifier.width(PaddingSmall))

                            Box(
                                modifier = Modifier
                                    .size(NavBarContextualButtonSize)
                                    .clip(CircleShape)
                                    .background(DraftoTheme.colors.sunsetCoral)
                                    .clickable {
                                        subtasks = subtasks.filterIndexed { i, _ -> i != index }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.todo_delete_subtask),
                                    tint = PureWhite,
                                    modifier = Modifier.size(BookmarkActionIconSize)
                                )
                            }
                        }
                    }

                    // Botón "+ Add task"
                    Button(
                        onClick = {
                            val nextSubtask = TodoSubtask(text = "")
                            pendingFocusSubtaskId = nextSubtask.id
                            subtasks = subtasks + nextSubtask
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BookmarkFormButtonHeight),
                        shape = DraftoShapePill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.todo_action_add_task),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 6. SECCIÓN: Due date (Optional)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.todo_due_date),
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.bookmark_optional),
                        style = MaterialTheme.typography.labelMedium,
                        color = DraftoTheme.colors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(PaddingSmall))

                val formattedDate = remember(dueDate) {
                    dueDate?.let { DraftoDateFormatter.formatTodoDueDate(it) }
                }

                Surface(
                    onClick = { showDatePicker = true },
                    shape = DraftoShapePill,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        width = BorderWidthThin,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BookmarkFormButtonHeight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = PaddingLarge),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formattedDate ?: stringResource(R.string.todo_set_due_date),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PaddingSmall)
                        ) {
                            if (dueDate != null) {
                                Box(
                                    modifier = Modifier
                                        .size(NavBarContextualButtonSize)
                                        .clip(CircleShape)
                                        .background(DraftoTheme.colors.sunsetCoral.copy(alpha = 0.15f))
                                        .clickable { dueDate = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.todo_clear_due_date),
                                        tint = DraftoTheme.colors.sunsetCoral,
                                        modifier = Modifier.size(BookmarkActionIconSize)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(BookmarkActionIconSize)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(BookmarkFormSectionSpacing))

            // 7. SECCIÓN: Collection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.bookmark_section_collection),
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(PaddingSmall))

                DraftoCapsuleFlowSelector(
                    options = collectionOptions,
                    selectedKey = selectedCollectionId,
                    onOptionSelected = { selectedCollectionId = it },
                    dividersAfterKeys = if (availableCollections.isNotEmpty()) setOf(null) else emptySet(),
                    height = BookmarkFormCapsuleHeight,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(PaddingExtraLarge))

            // 8. ACCIONES INFERIORES: Cancel (Rojo) y Create list / Save changes (Verde)
            val validSubtasks = remember(subtasks) {
                subtasks.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotBlank() }
            }
            val isSaveEnabled = title.isNotBlank() && validSubtasks.isNotEmpty()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PaddingLarge),
                horizontalArrangement = Arrangement.spacedBy(PaddingMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel (Rojo)
                DraftoSheetCancelButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f)
                )

                // Create list / Save changes (Verde)
                DraftoSheetPrimaryButton(
                    text = stringResource(
                        if (todoToEdit != null) R.string.action_save_changes else R.string.todo_action_create_list
                    ),
                    onClick = {
                        if (isSaveEnabled) {
                            onSaveTodoList?.invoke(
                                title.trim(),
                                description.trim(),
                                isPinned,
                                dueDate,
                                selectedCollectionId,
                                validSubtasks
                            ) ?: onSaveTodo(
                                title.trim(),
                                description.trim(),
                                isPinned,
                                dueDate
                            )
                            onDismissRequest()
                        }
                    },
                    enabled = isSaveEnabled,
                    containerColor = DraftoTheme.colors.emerald,
                    contentColor = PureWhite,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 9. Diálogo nativo DatePickerDialog para selección de fecha
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dueDate = datePickerState.selectedDateMillis
                            showDatePicker = false
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.action_apply),
                            color = DraftoTheme.colors.emerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            color = DraftoTheme.colors.textSecondary
                        )
                    }
                },
                colors = DatePickerDefaults.colors(
                    containerColor = DraftoTheme.colors.cardSurface
                )
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = DraftoTheme.colors.cardSurface,
                        selectedDayContainerColor = DraftoTheme.colors.accent,
                        todayDateBorderColor = DraftoTheme.colors.accent
                    )
                )
            }
        }
    }
}
