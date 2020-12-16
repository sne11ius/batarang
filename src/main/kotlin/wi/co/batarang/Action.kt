package wi.co.batarang

data class Action(
    val label: String,
    val tags: List<String>,
    val action: () -> String
) {
    fun matches(search: List<String>): Boolean {
        return search
            .map { it.toLowerCase() }
            .all { searchItem ->
                tags.any { it.toLowerCase().contains(searchItem) }
            }
    }
}
