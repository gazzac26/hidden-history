package com.hiddenhistory.database.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvParserUtil {

    /**
     * Safely splits a CSV line while respecting double-quotes (e.g. "Ford, Focus", 2024).
     */
    fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Smart row reader wrapper that automatically handles fuzzy header matching,
     * missing fields, and regex-based number extraction from messy formats.
     */
    class CsvRowReader(
        private val headerMap: Map<String, Int>,
        private val tokens: List<String>
    ) {
        /**
         * Safely finds a string column value using exact matching or fuzzy substring search.
         */
        fun getString(vararg keywords: String, default: String = "Unspecified"): String {
            // 1. Try exact match first
            for (keyword in keywords) {
                val index = headerMap[keyword.lowercase()]
                if (index != null && index < tokens.size) {
                    val value = tokens[index]
                    if (value.isNotBlank() && !value.equals("null", ignoreCase = true) && !value.equals("Unspecified", ignoreCase = true)) {
                        return value
                    }
                }
            }

            // 2. Fallback to fuzzy/substring matching across headers
            for (keyword in keywords) {
                val match = headerMap.entries.find { it.key.contains(keyword.lowercase()) }
                if (match != null) {
                    val index = match.value
                    if (index < tokens.size) {
                        val value = tokens[index]
                        if (value.isNotBlank() && !value.equals("null", ignoreCase = true) && !value.equals("Unspecified", ignoreCase = true)) {
                            return value
                        }
                    }
                }
            }
            return default
        }

        /**
         * Extracts an integer safely using regex from any format (e.g., "150 BHP" -> 150).
         */
        fun getInt(vararg keywords: String, default: Int): Int {
            val raw = getString(*keywords, default = "")
            if (raw.isBlank()) return default
            return Regex("\\d+").find(raw)?.value?.toIntOrNull() ?: default
        }

        /**
         * Extracts a double value safely using regex.
         */
        fun getDouble(vararg keywords: String, default: Double): Double {
            val raw = getString(*keywords, default = "")
            if (raw.isBlank()) return default
            return Regex("\\d+(\\.\\d+)?").find(raw)?.value?.toDoubleOrNull() ?: default
        }
    }

    /**
     * Reads the CSV file from assets, maps header columns, and passes a smart [CsvRowReader]
     * to easily extract rich data regardless of column header naming conventions.
     */
    inline fun parseCsvAsset(
        context: Context,
        fileName: String = "vehicles.csv",
        crossinline onRowParsed: (row: CsvRowReader) -> Unit
    ) {
        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    // Read and map header line columns to their index positions
                    val headerLine = reader.readLine() ?: return
                    val headerTokens = parseCsvLine(headerLine)
                    val headerMap = headerTokens.mapIndexed { index, name -> 
                        name.trim().lowercase() to index 
                    }.toMap()
                    
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line.isNullOrBlank()) continue
                        val tokens = parseCsvLine(line!!)
                        
                        // Clean/map tokens to replace "MODEL MISSING" with a user-friendly label
                        val cleanedTokens = tokens.map { token ->
                            if (token.equals("MODEL MISSING", ignoreCase = true) || token.isBlank()) {
                                "Unspecified"
                            } else {
                                token
                            }
                        }
                        
                        val rowReader = CsvRowReader(headerMap, cleanedTokens)
                        onRowParsed(rowReader)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
