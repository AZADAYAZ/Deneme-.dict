package com.manus.dicteditor

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryDictParser {
    companion object {
        private const val MAGIC_V2 = 0x9BC13AFE.toInt()
        private const val MAGIC_V4 = 0x78B13458.toInt()
    }

    fun parse(file: File): List<Word> {
        val bytes = file.readBytes()
        if (bytes.size < 4) return emptyList()
        
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.BIG_ENDIAN)

        val magic = buffer.int
        return when (magic) {
            MAGIC_V2 -> parseV2(buffer)
            MAGIC_V4 -> listOf(Word("V4 format not fully supported yet", 0))
            else -> parseFlat(bytes)
        }
    }

    private fun parseV2(buffer: ByteBuffer): List<Word> {
        val words = mutableListOf<Word>()
        val bytes = buffer.array()
        
        // AOSP V2 Binary Dictionary is a Trie structure.
        // However, many .dict files also contain a 'combined' text header or fallback sections.
        // We will use a hybrid approach:
        // 1. Try to find the " word=" pattern which is common in many tools' output.
        // 2. If that fails, we'll do a basic string scan for dictionary entries.
        
        val content = String(bytes, Charsets.ISO_8859_1) // Use ISO to avoid multi-byte issues during scan
        
        // Pattern 1: Standard combined format lines " word=...,f=..."
        val regex = Regex("word=([^,]+),f=(\\d+)")
        val matches = regex.findAll(content)
        for (match in matches) {
            val text = match.groups[1]?.value ?: continue
            val freq = match.groups[2]?.value?.toIntOrNull() ?: 0
            words.add(Word(text, freq))
        }
        
        if (words.isNotEmpty()) return words

        // Pattern 2: Fallback for some older binary formats where words are null-terminated or space-separated
        // This is a very basic heuristic parser
        var i = 12 // Skip header
        while (i < bytes.size - 4) {
            if (bytes[i] >= 32 && bytes[i] <= 126) {
                val start = i
                while (i < bytes.size && bytes[i] >= 32 && bytes[i] <= 126) i++
                val potentialWord = String(bytes.sliceArray(start until i), Charsets.UTF_8)
                if (potentialWord.length > 1) {
                    val freq = bytes.getOrElse(i + 1) { 0 }.toInt() and 0xFF
                    if (freq > 0) {
                        words.add(Word(potentialWord, freq))
                    }
                }
            }
            i++
        }
        
        return words
    }

    private fun parseFlat(bytes: ByteArray): List<Word> {
        val words = mutableListOf<Word>()
        val content = String(bytes, Charsets.UTF_8)
        val lines = content.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("word=")) {
                // Format: word=hello,f=255
                val parts = trimmed.split(",")
                if (parts.size >= 2) {
                    val text = parts[0].substringAfter("word=")
                    val freq = parts[1].substringAfter("f=").toIntOrNull() ?: 0
                    words.add(Word(text, freq))
                }
            } else if (trimmed.contains(",")) {
                // Format: hello,255
                val parts = trimmed.split(",")
                val text = parts[0].trim()
                val freq = parts[1].trim().toIntOrNull() ?: 0
                if (text.isNotEmpty()) {
                    words.add(Word(text, freq))
                }
            }
        }
        return words
    }
}
