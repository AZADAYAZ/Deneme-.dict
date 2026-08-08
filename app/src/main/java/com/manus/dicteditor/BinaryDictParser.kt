package com.manus.dicteditor

import com.android.inputmethod.latin.makedict.AospDictionaryReader
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reads Combined text dictionaries and AOSP binary dictionaries. */
class BinaryDictParser {
    var lastError: String? = null
        private set

    companion object {
        private const val MAGIC_V2 = 0x9BC13AFE.toInt()
        private const val MAGIC_V4 = 0x78B13458.toInt()

        internal fun isValidWord(text: String): Boolean {
            val word = text.trim()
            if (word.length < 2) return false
            if (word.first().isDigit()) return false
            if (!word.any { it.isLetter() }) return false
            return word.all { it.isLetter() || it == '-' || it == '\'' || it == '’' }
        }
    }

    fun parse(file: File): List<Word> {
        lastError = null
        val bytes = file.readBytes()
        if (bytes.size < 4) return parseFlat(bytes)

        val magic = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).int
        return if (magic == MAGIC_V2 || magic == MAGIC_V4) {
            parseBinaryWithAospDecoder(file)
        } else {
            parseFlat(bytes)
        }
    }

    private fun parseBinaryWithAospDecoder(file: File): List<Word> {
        return try {
            AospDictionaryReader.read(file)
                .asSequence()
                .map { Word(it.word, it.frequency) }
                .filter { isValidWord(it.text) }
                .distinctBy { it.text.lowercase() }
                .toList()
        } catch (error: Throwable) {
            // The bundled off-device decoder may require a native library that
            // is not available on every Android device. Never let that crash the app.
            lastError = error.message ?: error.javaClass.simpleName
            // Never fall back to scanning arbitrary binary bytes as text.
            // Returning an empty list is safer than showing fabricated words.
            emptyList()
        }
    }

    private fun parseFlat(bytes: ByteArray): List<Word> {
        val words = mutableListOf<Word>()
        val content = String(bytes, Charsets.UTF_8)

        for (line in content.split('\n')) {
            val trimmed = line.trim()
            if (trimmed.startsWith("word=")) {
                val wordPart = trimmed.substringAfter("word=").substringBefore(',').trim()
                val freqPart = trimmed.substringAfter("f=", "").substringBefore(',').trim()
                val freq = freqPart.toIntOrNull() ?: continue
                if (isValidWord(wordPart)) words.add(Word(wordPart, freq))
            } else if (trimmed.contains(',')) {
                val parts = trimmed.split(',', limit = 2)
                val text = parts[0].trim()
                val freq = parts[1].trim().toIntOrNull() ?: continue
                if (isValidWord(text)) words.add(Word(text, freq))
            }
        }

        return words.distinctBy { it.text.lowercase() }
    }
}
