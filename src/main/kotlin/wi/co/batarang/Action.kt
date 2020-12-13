package wi.co.batarang

data class Action(
    val label: String,
    val strings: List<String>,
    val action: Runnable
) {
    fun matches(search: List<String>): Boolean {
        return search
            .map { it.toLowerCase() }
            .all { searchItem ->
                strings.any { it.toLowerCase().contains(searchItem) }
            }
    }
}
