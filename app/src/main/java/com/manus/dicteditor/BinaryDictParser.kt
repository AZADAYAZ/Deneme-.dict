package com.manus.dicteditor

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AOSP sözlüklerini ve Combined metin sözlüklerini okur.
 *
 * Binary .dict dosyalarında rastgele yazdırılabilir byte dizilerini kelime
 * kabul etmek güvenli değildir. Bu nedenle fallback yalnızca kelime biçimine
 * uyan ve harf içeren adayları kabul eder.
 */
class BinaryDictParser {
    companion object {
        private const val MAGIC_V2 = 0x9BC13AFE.toInt()
        private const val MAGIC_V4 = 0x78B13458.toInt()

        /**
         * Sözlük kelimesi en az bir harf içermeli ve rakamla başlayamamalıdır.
         * Türkçe karakterler Char.isLetter() ile desteklenir.
         */
        internal fun isValidWord(text: String): Boolean {
            val word = text.trim()
            if (word.length < 2) return false
            if (word.first().isDigit()) return false
            if (!word.any { it.isLetter() }) return false
            return word.all { it.isLetter() || it == '-' || it == '\'' || it == '’' }
        }
    }

    fun parse(file: File): List<Word> {
        val bytes = file.readBytes()
        if (bytes.size < 4) return emptyList()

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val magic = buffer.int
        return when (magic) {
            MAGIC_V2 -> parseV2(buffer)
            MAGIC_V4 -> emptyList() // V4 için henüz güvenilir çözümleyici yok.
            else -> parseFlat(bytes)
        }
    }

    private fun parseV2(buffer: ByteBuffer): List<Word> {
        val words = mutableListOf<Word>()
        val bytes = buffer.array()
        val content = String(bytes, Charsets.ISO_8859_1)

        // Combined format: " word=kelime,f=255"
        val regex = Regex("(?:^|\\s)word=([^,\\r\\n]+),f=(\\d+)")
        for (match in regex.findAll(content)) {
            val text = match.groups[1]?.value?.trim() ?: continue
            val freq = match.groups[2]?.value?.toIntOrNull() ?: continue
            if (isValidWord(text)) {
                words.add(Word(text, freq))
            }
        }

        if (words.isNotEmpty()) return words.distinctBy { it.text.lowercase() }

        // Eski bazı dosyalarda düz ASCII parçaları bulunabilir. Bu bölüm
        // yalnızca güçlü kelime doğrulamasını geçen adayları kabul eder.
        var i = 12
        while (i < bytes.size) {
            val value = bytes[i].toInt() and 0xFF
            if (value in 32..126) {
                val start = i
                while (i < bytes.size) {
                    val current = bytes[i].toInt() and 0xFF
                    if (current !in 32..126) break
                    i++
                }

                val candidate = String(bytes, start, i - start, Charsets.UTF_8).trim()
                val freqIndex = i + 1
                val freq = if (freqIndex < bytes.size) bytes[freqIndex].toInt() and 0xFF else 0
                if (freq > 0 && isValidWord(candidate)) {
                    words.add(Word(candidate, freq))
                }
            }
            i++
        }

        return words.distinctBy { it.text.lowercase() }
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
