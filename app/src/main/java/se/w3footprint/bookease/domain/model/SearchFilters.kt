package se.w3footprint.bookease.domain.model

data class SearchFilters(
    val query: String = "",
    val category: String = "All",
    val city: String = "",
    val sortBy: SortOption = SortOption.DEFAULT
)

enum class SortOption {
    DEFAULT,
    TOP_RATED,
    NEAREST,
    MOST_BOOKED
}
