package io.correcta.assessment.models.response

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val sortBy: String? = null,
    val sortDirection: String? = null
)
