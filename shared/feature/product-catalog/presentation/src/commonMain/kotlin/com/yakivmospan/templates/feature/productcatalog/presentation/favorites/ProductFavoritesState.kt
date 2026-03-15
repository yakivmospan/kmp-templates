package com.yakivmospan.templates.feature.productcatalog.presentation.favorites

import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ProductFavoritesState(
    val favorites: ImmutableList<ProductViewData> = persistentListOf(),
    val searchResult: ImmutableList<ProductViewData> = persistentListOf(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
)