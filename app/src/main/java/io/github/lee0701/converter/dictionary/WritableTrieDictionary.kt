package io.github.lee0701.converter.dictionary

abstract class WritableTrieDictionary<T>: TrieDictionary<T>(), WritableDictionary<T> {

    override val root: Node<T> = Node()

    override fun put(key: String, value: T) {
        var p = root
        for(c in key) {
            p = p.children.getOrPut(c) { Node() }
        }
        p.entry = value
    }

    override fun remove(key: String) {
        var p = root
        for(c in key) {
            p = p.children[c] ?: return
        }
        p.entry = null
    }

    data class Node<T>(
        override val children: MutableMap<Char, Node<T>> = mutableMapOf(),
        override var entry: T? = null,
    ): TrieDictionary.Node<T>

}