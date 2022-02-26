package io.github.lee0701.converter.dictionary

abstract class TrieDictionary<T>: Dictionary<T> {

    abstract val root: Node<T>

    override fun search(key: String): T? {
        var p = root
        for(c in key) {
            p = p.children[c] ?: return null
        }
        return p.entry
    }

    interface Node<T> {
        val children: Map<Char, Node<T>>
        val entry: T?
    }

}