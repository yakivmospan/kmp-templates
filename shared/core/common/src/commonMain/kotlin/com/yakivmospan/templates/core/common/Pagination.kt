package com.yakivmospan.templates.core.common

data class PaginatedData<T>(
    val items: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
)

data class PageRequest(
    val page: Int = 1,
    val pageSize: Int = 20,
    val sortBy: String? = null,
    val sortDirection: SortDirection = SortDirection.ASC
)

enum class SortDirection {
    ASC, DESC
}