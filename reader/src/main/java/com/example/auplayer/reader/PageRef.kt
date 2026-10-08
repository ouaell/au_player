package com.example.auplayer.reader

/** Stable page reference (MS-033). Index is validated against the provider. */
data class PageRef(val index: Int, val id: String)

fun PageProvider.ref(index: Int): PageRef {
    if (index < 0 || index >= pageCount) {
        throw IndexOutOfBoundsException("page $index of $pageCount")
    }
    return PageRef(index, "$title#$index")
}
