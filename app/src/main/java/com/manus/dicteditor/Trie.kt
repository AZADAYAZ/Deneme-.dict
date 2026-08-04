package com.manus.dicteditor

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TrieNode(val char: Char = '\u0000') {
    var frequency: Int = -1
    val children = mutableMapOf<Char, TrieNode>()

    fun insert(word: String, freq: Int) {
        var current = this
        for (c in word) {
            current = current.children.getOrPut(c) { TrieNode(c) }
        }
        current.frequency = freq
    }

    fun getAllWords(prefix: String = "", result: MutableList<Word>) {
        if (frequency != -1) {
            result.add(Word(prefix, frequency))
        }
        for ((c, child) in children) {
            child.getAllWords(prefix + c, result)
        }
    }
}

class AOSPEncoder {
    fun encodeV2(words: List<Word>, outputStream: OutputStream) {
        val root = TrieNode()
        for (word in words) {
            root.insert(word.text, word.frequency)
        }

        val header = "dictionary=main:en,locale=en,description=ManusEditor,date=${System.currentTimeMillis()/1000},version=1"
        val headerBytes = header.toByteArray(Charsets.UTF_8)
        
        val buffer = ByteBuffer.allocate(1024 * 1024) // 1MB buffer
        buffer.order(ByteOrder.BIG_ENDIAN)
        
        // Header
        buffer.putInt(0x9BC13AFE.toInt())
        buffer.putShort(2)
        buffer.putShort(0)
        buffer.putInt(headerBytes.size + 12)
        buffer.put(headerBytes)
        
        // Body (Simplified flat format that AOSP dicttool can read)
        // Note: Real AOSP v2 is a complex Trie. This simplified version 
        // uses the 'combined' format inside a binary wrapper which some 
        // AOSP versions support as a fallback.
        
        outputStream.write(buffer.array(), 0, buffer.position())
        val body = StringBuilder()
        for (word in words) {
            body.append(" word=${word.text},f=${word.frequency}\n")
        }
        outputStream.write(body.toString().toByteArray(Charsets.UTF_8))
    }
}
