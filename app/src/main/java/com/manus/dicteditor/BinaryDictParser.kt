package com.manus.dicteditor

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryDictParser {
    companion object {
        private const val MAGIC_V2 = 0x9BC13AFE.toInt()
        private const val MAGIC_V4 = 0x78B13458.toInt()
    }

    fun parse(file: File): List<Word> {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.BIG_ENDIAN)

        val magic = buffer.int
        if (magic == MAGIC_V2) {
            return parseV2(buffer)
        } else if (magic == MAGIC_V4) {
            // V4 is complex, usually a directory or multi-file
            // For now, let's return an empty list or try a basic parse
            return listOf(Word("V4 format not fully supported in lite version", 0))
        } else {
            // Try the simple flat format I saw earlier
            return parseFlat(bytes)
        }
    }

    private fun parseV2(buffer: ByteBuffer): List<Word> {
        val words = mutableListOf<Word>()
        // Simple string extraction as a fallback for complex Tries
        val bytes = buffer.array()
        var i = 0
        while (i < bytes.size - 2) {
            if (bytes[i].toInt() == 0x20 && bytes[i+1].toInt() == 0x77) { // " w"
                // Potential word start in combined-in-binary format
                val start = i
                while (i < bytes.size && bytes[i].toInt() != 0x0A) i++
                val line = String(bytes.sliceArray(start until i), Charsets.UTF_8)
                if (line.startsWith(" word=")) {
                    val parts = line.trim().split(",")
                    if (parts.size >= 2) {
                        val text = parts[0].substring(6)
                        val freq = parts[1].substring(2).toIntOrNull() ?: 0
                        words.add(Word(text, freq))
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
        // AOSP dicttool combined format parser
        val lines = content.split("\n")
        for (line in lines) {
            if (line.startsWith(" word=")) {
                val parts = line.trim().split(",")
                val text = parts[0].substring(6)
                val freq = parts[1].substring(2).toIntOrNull() ?: 0
                words.add(Word(text, freq))
            }
        }
        return words
    }
    
    fun encodeToCombined(words: List<Word>): String {
        val sb = StringBuilder()
        sb.append("dictionary=main:en,locale=en,description=Edited,date=${System.currentTimeMillis()/1000},version=1\n")
        for (word in words) {
            sb.append(" word=${word.text},f=${word.frequency}\n")
        }
        return sb.toString()
    }
}
