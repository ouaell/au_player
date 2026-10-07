package com.example.auplayer.library

/** MediaStore/SAF browsing lands in MS-020. */
interface LibrarySource {
    fun folders(): List<String>
}

class FakeLibrarySource : LibrarySource {
    override fun folders(): List<String> = emptyList()
}
