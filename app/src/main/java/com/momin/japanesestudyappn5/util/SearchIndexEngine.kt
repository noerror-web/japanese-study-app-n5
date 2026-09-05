package com.momin.japanesestudyappn5.util

import com.momin.japanesestudyappn5.data.model.JMdictEntry

object SearchIndexEngine {

    private fun getJlptRank(entry: JMdictEntry): Int {
        val level = entry.jlptLevel?.lowercase() ?: ""
        return when {
            level.contains("n5") -> 1
            level.contains("n4") -> 2
            level.contains("n3") -> 3
            level.contains("n2") -> 4
            level.contains("n1") -> 5
            entry.isCommon -> 6
            else -> 7
        }
    }

    /**
     * Performs fast indexed search across JMdict entries with strict JLPT N5 -> N1 priority ordering.
     */
    fun search(entries: List<JMdictEntry>, query: String, maxResults: Int = 50): List<JMdictEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return entries.sortedBy { getJlptRank(it) }.take(maxResults)

        val exactMatches = mutableListOf<JMdictEntry>()
        val prefixMatches = mutableListOf<JMdictEntry>()
        val containsMatches = mutableListOf<JMdictEntry>()

        for (entry in entries) {
            val kanji = entry.kanji.lowercase()
            val reading = entry.reading.lowercase()
            val furigana = entry.furigana.lowercase()
            val romaji = entry.romaji.lowercase()
            val bangla = entry.bangla?.lowercase() ?: ""

            // Combine sense meanings
            val meanings = entry.senses.flatMap { it.meanings }.joinToString(" ").lowercase()
            val meaningsBn = entry.senses.flatMap { it.glossesBn ?: emptyList() }.joinToString(" ").lowercase()

            val isExact = kanji == q || reading == q || furigana == q || romaji == q
            val isPrefix = kanji.startsWith(q) || reading.startsWith(q) || furigana.startsWith(q) || romaji.startsWith(q)
            val isContains = kanji.contains(q) || reading.contains(q) || romaji.contains(q) ||
                    meanings.contains(q) || bangla.contains(q) || meaningsBn.contains(q)

            when {
                isExact -> exactMatches.add(entry)
                isPrefix -> prefixMatches.add(entry)
                isContains -> containsMatches.add(entry)
            }
        }

        // Sort each bucket by JLPT level priority N5 -> N4 -> N3 -> N2 -> N1
        val comparator = Comparator<JMdictEntry> { a, b -> getJlptRank(a).compareTo(getJlptRank(b)) }
        exactMatches.sortWith(comparator)
        prefixMatches.sortWith(comparator)
        containsMatches.sortWith(comparator)

        val results = ArrayList<JMdictEntry>(maxResults)
        results.addAll(exactMatches)
        if (results.size < maxResults) results.addAll(prefixMatches)
        if (results.size < maxResults) results.addAll(containsMatches)

        return results.distinctBy { it.id }.take(maxResults)
    }
}
