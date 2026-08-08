package com.android.inputmethod.latin.makedict;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Uses AOSP's own off-device dictionary decoder instead of scanning binary
 * dictionary bytes as text.
 */
public final class AospDictionaryReader {
    private AospDictionaryReader() {}

    public static final class Entry {
        public final String word;
        public final int frequency;

        public Entry(final String word, final int frequency) {
            this.word = word;
            this.frequency = frequency;
        }
    }

    public static ArrayList<Entry> read(final File file) throws IOException {
        final DictDecoder decoder = BinaryDictIOUtils.getDictDecoder(file, 0, file.length());
        if (decoder == null || !decoder.hasValidRawBinaryDictionary()) {
            throw new IOException("Geçersiz veya desteklenmeyen AOSP sözlük biçimi");
        }

        final FusionDictionary dictionary;
        try {
            dictionary = decoder.readDictionaryBinary(false);
        } catch (UnsupportedFormatException e) {
            throw new IOException("AOSP sözlük biçimi desteklenmiyor", e);
        }

        final ArrayList<Entry> result = new ArrayList<>();
        for (final WordProperty property : dictionary) {
            if (property == null || property.mIsNotAWord) continue;
            if (property.mWord == null || property.mWord.length() == 0) continue;
            result.add(new Entry(property.mWord, property.getProbability()));
        }
        return result;
    }
}
