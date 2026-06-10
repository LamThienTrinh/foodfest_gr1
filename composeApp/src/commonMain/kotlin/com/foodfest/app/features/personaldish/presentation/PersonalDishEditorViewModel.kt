package com.foodfest.app.features.personaldish.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodfest.app.features.personaldish.data.CreatePersonalDishRequest
import com.foodfest.app.features.personaldish.data.PersonalDishRepository
import com.foodfest.app.features.tag.data.Tag
import com.foodfest.app.features.tag.data.TagRepository
import com.foodfest.app.features.upload.data.ImageUploadRepository
import kotlin.random.Random

data class PersonalDishEditorState(
    val dishName: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val ingredients: String = "",
    val instructions: String = "",
    val prepTime: String = "",
    val cookTime: String = "",
    val serving: String = "",
    val note: String = "",
    val tags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Int> = emptySet(),
    val isLoadingTags: Boolean = false,
    val isSaving: Boolean = false,
    val savingMessage: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

private data class OptionalIntParse(
    val value: Int?,
    val isValid: Boolean
)

class PersonalDishEditorViewModel(
    private val repository: PersonalDishRepository = PersonalDishRepository(),
    private val tagRepository: TagRepository = TagRepository(),
    private val imageUploadRepository: ImageUploadRepository = ImageUploadRepository()
) {
    var state by mutableStateOf(PersonalDishEditorState())
        private set

    suspend fun loadTags() {
        if (state.isLoadingTags || state.tags.isNotEmpty()) return
        state = state.copy(isLoadingTags = true)
        tagRepository.getAllTags().fold(
            onSuccess = { tags ->
                state = state.copy(tags = tags, isLoadingTags = false)
            },
            onFailure = {
                state = state.copy(isLoadingTags = false)
            }
        )
    }

    fun updateDishName(value: String) {
        state = state.copy(dishName = value, errorMessage = null)
    }

    fun updateImageUrl(value: String) {
        state = state.copy(imageUrl = value, errorMessage = null)
    }

    fun updateDescription(value: String) {
        state = state.copy(description = value)
    }

    fun updateIngredients(value: String) {
        state = state.copy(ingredients = value)
    }

    fun updateInstructions(value: String) {
        state = state.copy(instructions = value)
    }

    fun updatePrepTime(value: String) {
        state = state.copy(prepTime = value, errorMessage = null)
    }

    fun updateCookTime(value: String) {
        state = state.copy(cookTime = value, errorMessage = null)
    }

    fun updateServing(value: String) {
        state = state.copy(serving = value, errorMessage = null)
    }

    fun updateNote(value: String) {
        state = state.copy(note = value)
    }

    fun toggleTag(tagId: Int) {
        val selected = state.selectedTagIds
        state = state.copy(
            selectedTagIds = if (tagId in selected) selected - tagId else selected + tagId
        )
    }

    suspend fun createDish(
        imageBytes: ByteArray? = null,
        fileName: String? = null
    ): Boolean {
        if (state.isSaving) return false
        val request = buildCreateRequest() ?: return false

        state = state.copy(
            isSaving = true,
            savingMessage = if (imageBytes != null) "Đang upload ảnh..." else "Đang lưu món...",
            errorMessage = null,
            successMessage = null
        )

        val finalRequest = if (imageBytes != null) {
            val uploadedUrl = imageUploadRepository.uploadImage(
                imageBytes = imageBytes,
                fileName = fileName ?: "personal_dish_${Random.nextInt(100000, 999999)}.jpg",
                folder = "personal-dishes"
            ).fold(
                onSuccess = { it },
                onFailure = { error ->
                    state = state.copy(
                        isSaving = false,
                        savingMessage = null,
                        errorMessage = error.message ?: "Upload ảnh thất bại"
                    )
                    return false
                }
            )
            // Uploaded file wins over manual URL so the dish always points to the newest chosen image.
            request.copy(imageUrl = uploadedUrl)
        } else {
            request
        }

        state = state.copy(savingMessage = "Đang tạo món...")
        return repository.create(finalRequest).fold(
            onSuccess = {
                state = state.copy(
                    isSaving = false,
                    savingMessage = null,
                    successMessage = "Đã tạo món của tôi"
                )
                true
            },
            onFailure = { error ->
                state = state.copy(
                    isSaving = false,
                    savingMessage = null,
                    errorMessage = error.message ?: "Không tạo được món"
                )
                false
            }
        )
    }

    private fun buildCreateRequest(): CreatePersonalDishRequest? {
        val name = state.dishName.trim()
        if (name.isBlank()) {
            state = state.copy(errorMessage = "Vui lòng nhập tên món")
            return null
        }

        val prep = parsePositiveIntField(state.prepTime, "Thời gian chuẩn bị")
        if (!prep.isValid) return null
        val cook = parsePositiveIntField(state.cookTime, "Thời gian nấu")
        if (!cook.isValid) return null
        val serving = parsePositiveIntField(state.serving, "Khẩu phần")
        if (!serving.isValid) return null

        // Blank form fields become null so the backend stores optional recipe data cleanly.
        return CreatePersonalDishRequest(
            dishName = name,
            imageUrl = state.imageUrl.trim().ifBlank { null },
            description = state.description.trim().ifBlank { null },
            ingredients = state.ingredients.trim().ifBlank { null },
            instructions = state.instructions.trim().ifBlank { null },
            prepTime = prep.value,
            cookTime = cook.value,
            serving = serving.value,
            note = state.note.trim().ifBlank { null },
            tagIds = state.selectedTagIds.toList().takeIf { it.isNotEmpty() }
        )
    }

    private fun parsePositiveIntField(value: String, label: String): OptionalIntParse {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return OptionalIntParse(value = null, isValid = true)

        val parsed = trimmed.toIntOrNull()
        if (parsed == null || parsed <= 0) {
            state = state.copy(errorMessage = "$label phải là số lớn hơn 0")
            return OptionalIntParse(value = null, isValid = false)
        }
        return OptionalIntParse(value = parsed, isValid = true)
    }
}
