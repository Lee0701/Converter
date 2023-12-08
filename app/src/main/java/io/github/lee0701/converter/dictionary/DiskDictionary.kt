package io.github.lee0701.converter.dictionary

import java.io.InputStream
import java.lang.StringBuilder
import java.nio.ByteBuffer

class DiskDictionary(input: InputStream): HanjaDictionary,
    PredictingDictionary<HanjaDictionary.Entry> {

    private val data = ByteBuffer.wrap(input.readBytes())
    private val root get() = data.getInt(data.capacity() - 4)

    override fun search(key: String): List<HanjaDictionary.Entry> {
        // root
        var p = Node(root)
        for(c in key) {
            p = p.children[c] ?: return listOf()
        }
        return p.entries
    }

    override fun predict(key: String): List<Pair<String, HanjaDictionary.Entry>> {
        var p = Node(root)
        for(c in key) {
            p = p.children[c] ?: return listOf()
        }
        return getEntriesRecursive(p, key, key.length * 2)
    }

    private fun getEntriesRecursive(p: Node, string: String, depth: Int): List<Pair<String, HanjaDictionary.Entry>> {
        if(depth <= 0) return listOf()
        return p.entries.map { string to it } + p.children.flatMap { getEntriesRecursive(it.value, string + it.key, depth-1) }
    }

    private fun getChars(bb: ByteBuffer, idx: Int): String {
        val sb = StringBuilder()
        var i = 0
        while(true) {
            val c = bb.getChar(idx + i)
            if(c.code == 0) break
            sb.append(c)
            i += 2
        }
        return sb.toString()
    }

    inner class Node(
        val address: Int,
    ) {
        private val childrenCount: Short = data.getShort(address)
        private val entryAddress: Int = address + 2 + childrenCount*6
        private val entryCount: Short = data.getShort(entryAddress)

        val children: Map<Char, Node> get() = (0 until childrenCount).associate { i->
            val addr = address + 2 + i*6
            data.getChar(addr) to Node(data.getInt(addr + 2))
        }
        val entries: List<HanjaDictionary.Entry> get() = run {
            var p = entryAddress + 2
            (0 until entryCount).map { i ->
                val result = getChars(data, p)
                p += result.length*2 + 2
                val extra = getChars(data, p)
                p += extra.length*2 + 2
                val frequency = data.getShort(p).toInt()
                p += 2
                HanjaDictionary.Entry(result, extra, frequency)
            }
        }
    }

}
