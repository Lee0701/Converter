package io.github.lee0701.converter.dictionary

import io.github.lee0701.converter.HanjaDictionary
import java.io.InputStream
import java.lang.StringBuilder
import java.nio.ByteBuffer

class DiskDictionary(input: InputStream): ListDictionary<HanjaDictionary.Entry> {
    private val data = ByteBuffer.wrap(input.readBytes())

    override fun search(key: String): List<HanjaDictionary.Entry> {
        // root
        var p = data.getInt(data.capacity() - 4)
        for(c in key) {
            // children count
            val children = data.getShort(p)
            for(i in 0 until children) {
                val ch = data.getChar(p + 2 + i*6)
                val addr = data.getInt(p + 2 + i*6 + 2)
                if(ch == c) {
                    p = addr
                    break
                } else if(i == children - 1) {
                    return listOf()
                }
            }
        }
        val children = data.getShort(p)
        p += 2 + children*6
        val entries = data.getShort(p)
        p += 2
        return (0 until entries).map {
            val result = getChars(data, p)
            p += result.length*2 + 1
            val extra = getChars(data, p)
            p += extra.length*2 + 1
            val frequency = data.getShort(p).toInt()
            p += 2
            HanjaDictionary.Entry(result, extra, frequency)
        }
    }

    private fun getChars(bb: ByteBuffer, idx: Int): String {
        val sb = StringBuilder()
        var i = 0
        while(true) {
            if(bb.get(idx + i).toInt() == 0) break
            val c = bb.getChar(idx + i)
            sb.append(c)
            i += 2
        }
        return sb.toString()
    }

}
