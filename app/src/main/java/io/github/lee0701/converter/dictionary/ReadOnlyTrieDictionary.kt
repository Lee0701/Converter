package io.github.lee0701.converter.dictionary

open class ReadOnlyTrieDictionary<T>(
    map: Map<String, T>
): TrieDictionary<T>() {

    override val root: Node<T> = Node.build(map)

    data class Node<T>(
        override val children: Map<Char, Node<T>> = mapOf(),
        override val entry: T? = null
    ): TrieDictionary.Node<T> {
        companion object {
            fun <T> build(map: Map<String, T>, key: String = ""): Node<T> {
                val filtered = map.filterKeys { it.startsWith(key) }
                val keys = filtered.keys.filter { it.length > key.length }.map { it[key.length] }.toSet()
                val children = keys.associateWith { c -> build(filtered, key + c) }
                return Node(children, map[key])
            }
        }
    }

}