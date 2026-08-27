package com.hiddenhistory.engine.advert.extraction

import java.util.Locale

class AdvertVehicleExtractor {

    // ---------------------------------------------------------
    // ENGINE SIZE
    // ---------------------------------------------------------

    private val engineSizePatterns = listOf(

        // 2.0L / 2.0 L / 2 litre / 2 litres
        Regex(
            """\b([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*(?:l|litre|litres)\b""",
            RegexOption.IGNORE_CASE
        ),

        // 2000cc / 2000 cc
        Regex(
            """\b([0-9]{3,5})\s*cc\b""",
            RegexOption.IGNORE_CASE
        ),

        // 2.0 HDi / 1.6 TDI / 2.0 TDCi / 1.6 FSI etc.
        Regex(
            """\b([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*(?:tdci|hdi|cdti|dci|tdi|tfsi|fsi|tsi|vtec|crdi|d4d|jtd|multijet|ecoboost)\b""",
            RegexOption.IGNORE_CASE
        ),

        // Common advert shorthand such as "1.6 diesel",
        // "2.0 petrol" or "1.5 hybrid".
        Regex(
            """\b([0-9]{1,2}(?:\.[0-9]{1,2})?)\s*(?:diesel|petrol|hybrid|phev)\b""",
            RegexOption.IGNORE_CASE
        )
    )


    // ---------------------------------------------------------
    // MANUFACTURERS
    // ---------------------------------------------------------

    private val makePatterns =
        linkedMapOf(

            "Abarth" to Regex(
                """\babarth\b""",
                RegexOption.IGNORE_CASE
            ),

            "Alfa Romeo" to Regex(
                """\balfa\s+romeo\b""",
                RegexOption.IGNORE_CASE
            ),

            "Alpine" to Regex(
                """\balpine\b""",
                RegexOption.IGNORE_CASE
            ),

            "Aston Martin" to Regex(
                """\baston\s+martin\b""",
                RegexOption.IGNORE_CASE
            ),

            "Audi" to Regex(
                """\baudi\b""",
                RegexOption.IGNORE_CASE
            ),

            "Bentley" to Regex(
                """\bbentley\b""",
                RegexOption.IGNORE_CASE
            ),

            "BMW" to Regex(
                """\bbmw\b""",
                RegexOption.IGNORE_CASE
            ),

            "Citroen" to Regex(
                """\bcitroen\b|\bcitroën\b""",
                RegexOption.IGNORE_CASE
            ),

            "Cupra" to Regex(
                """\bcupra\b""",
                RegexOption.IGNORE_CASE
            ),

            "Dacia" to Regex(
                """\bdacia\b""",
                RegexOption.IGNORE_CASE
            ),

            "DS" to Regex(
                """\bds\b""",
                RegexOption.IGNORE_CASE
            ),

            "Fiat" to Regex(
                """\bfiat\b""",
                RegexOption.IGNORE_CASE
            ),

            "Ford" to Regex(
                """\bford\b""",
                RegexOption.IGNORE_CASE
            ),

            "Honda" to Regex(
                """\bhonda\b""",
                RegexOption.IGNORE_CASE
            ),

            "Hyundai" to Regex(
                """\bhyundai\b""",
                RegexOption.IGNORE_CASE
            ),

            "Jaguar" to Regex(
                """\bjaguar\b""",
                RegexOption.IGNORE_CASE
            ),

            "Jeep" to Regex(
                """\bjeep\b""",
                RegexOption.IGNORE_CASE
            ),

            "Kia" to Regex(
                """\bkia\b""",
                RegexOption.IGNORE_CASE
            ),

            "Land Rover" to Regex(
                """\bland\s+rover\b""",
                RegexOption.IGNORE_CASE
            ),

            "Lexus" to Regex(
                """\blexus\b""",
                RegexOption.IGNORE_CASE
            ),

            "Lotus" to Regex(
                """\blotus\b""",
                RegexOption.IGNORE_CASE
            ),

            "Mazda" to Regex(
                """\bmazda\b""",
                RegexOption.IGNORE_CASE
            ),

            "McLaren" to Regex(
                """\bmclaren\b""",
                RegexOption.IGNORE_CASE
            ),

            "Mercedes-Benz" to Regex(
                """\b(?:mercedes[-\s]?benz|mercedes)\b""",
                RegexOption.IGNORE_CASE
            ),

            "MG" to Regex(
                """\bmg\b""",
                RegexOption.IGNORE_CASE
            ),

            "Mini" to Regex(
                """\bmini\b""",
                RegexOption.IGNORE_CASE
            ),

            "Mitsubishi" to Regex(
                """\bmitsubishi\b""",
                RegexOption.IGNORE_CASE
            ),

            "Nissan" to Regex(
                """\bnissan\b""",
                RegexOption.IGNORE_CASE
            ),

            "Peugeot" to Regex(
                """\bpeugeot\b""",
                RegexOption.IGNORE_CASE
            ),

            "Polestar" to Regex(
                """\bpolestar\b""",
                RegexOption.IGNORE_CASE
            ),

            "Porsche" to Regex(
                """\bporsche\b""",
                RegexOption.IGNORE_CASE
            ),

            "Renault" to Regex(
                """\brenault\b""",
                RegexOption.IGNORE_CASE
            ),

            "Rolls-Royce" to Regex(
                """\brolls[-\s]?royce\b""",
                RegexOption.IGNORE_CASE
            ),

            "SEAT" to Regex(
                """\bseat\b""",
                RegexOption.IGNORE_CASE
            ),

            "Skoda" to Regex(
                """\bskoda\b|\bškoda\b""",
                RegexOption.IGNORE_CASE
            ),

            "Smart" to Regex(
                """\bsmart\b""",
                RegexOption.IGNORE_CASE
            ),

            "Subaru" to Regex(
                """\bsubaru\b""",
                RegexOption.IGNORE_CASE
            ),

            "Suzuki" to Regex(
                """\bsuzuki\b""",
                RegexOption.IGNORE_CASE
            ),

            "Tesla" to Regex(
                """\btesla\b""",
                RegexOption.IGNORE_CASE
            ),

            "Toyota" to Regex(
                """\btoyota\b""",
                RegexOption.IGNORE_CASE
            ),

            "Vauxhall" to Regex(
                """\bvauxhall\b""",
                RegexOption.IGNORE_CASE
            ),

            "Volkswagen" to Regex(
                """\b(?:volkswagen|vw)\b""",
                RegexOption.IGNORE_CASE
            ),

            "Volvo" to Regex(
                """\bvolvo\b""",
                RegexOption.IGNORE_CASE
            )
        )

    // ---------------------------------------------------------
    // COMMON UK MODELS
    // ---------------------------------------------------------

    private val modelPatterns =
        linkedMapOf(

            // Ford
            "Focus" to Regex(
                """\bfocus\b""",
                RegexOption.IGNORE_CASE
            ),

            "Fiesta" to Regex(
                """\bfiesta\b""",
                RegexOption.IGNORE_CASE
            ),

            "Mondeo" to Regex(
                """\bmondeo\b""",
                RegexOption.IGNORE_CASE
            ),

            "Puma" to Regex(
                """\bpuma\b""",
                RegexOption.IGNORE_CASE
            ),

            "Kuga" to Regex(
                """\bkuga\b""",
                RegexOption.IGNORE_CASE
            ),

            "Ka" to Regex(
                """\bka\b""",
                RegexOption.IGNORE_CASE
            ),

            "Mustang" to Regex(
                """\bmustang\b""",
                RegexOption.IGNORE_CASE
            ),

            "Ecosport" to Regex(
                """\becosport\b""",
                RegexOption.IGNORE_CASE
            ),

            // Peugeot
            //
            // Numeric Peugeot models are deliberately kept AFTER
            // the normal textual model names so that a vehicle year
            // such as "2008" cannot accidentally become the model
            // when another genuine model is present.
            "108" to Regex(
                """\b108\b"""
            ),

            "208" to Regex(
                """\b208\b"""
            ),

            "2008" to Regex(
                """\b2008\b"""
            ),

            "308" to Regex(
                """\b308\b"""
            ),

            "3008" to Regex(
                """\b3008\b"""
            ),

            "408" to Regex(
                """\b408\b"""
            ),

            "508" to Regex(
                """\b508\b"""
            ),

            "5008" to Regex(
                """\b5008\b"""
            ),

            // Volkswagen
            "Up" to Regex(
                """\bup!\b|\bup\b""",
                RegexOption.IGNORE_CASE
            ),

            "Polo" to Regex(
                """\bpolo\b""",
                RegexOption.IGNORE_CASE
            ),

            "Golf" to Regex(
                """\bgolf\b""",
                RegexOption.IGNORE_CASE
            ),

            "Passat" to Regex(
                """\bpassat\b""",
                RegexOption.IGNORE_CASE
            ),

            "Jetta" to Regex(
                """\bjetta\b""",
                RegexOption.IGNORE_CASE
            ),

            "Tiguan" to Regex(
                """\btiguan\b""",
                RegexOption.IGNORE_CASE
            ),

            "Touareg" to Regex(
                """\btouareg\b""",
                RegexOption.IGNORE_CASE
            ),

            "Touran" to Regex(
                """\btouran\b""",
                RegexOption.IGNORE_CASE
            ),

            "Arteon" to Regex(
                """\barteon\b""",
                RegexOption.IGNORE_CASE
            ),

            // Audi
            "A1" to Regex(
                """\ba1\b""",
                RegexOption.IGNORE_CASE
            ),

            "A3" to Regex(
                """\ba3\b""",
                RegexOption.IGNORE_CASE
            ),

            "A4" to Regex(
                """\ba4\b""",
                RegexOption.IGNORE_CASE
            ),

            "A5" to Regex(
                """\ba5\b""",
                RegexOption.IGNORE_CASE
            ),

            "A6" to Regex(
                """\ba6\b""",
                RegexOption.IGNORE_CASE
            ),

            "A7" to Regex(
                """\ba7\b""",
                RegexOption.IGNORE_CASE
            ),

            "A8" to Regex(
                """\ba8\b""",
                RegexOption.IGNORE_CASE
            ),

            "Q2" to Regex(
                """\bq2\b""",
                RegexOption.IGNORE_CASE
            ),

            "Q3" to Regex(
                """\bq3\b""",
                RegexOption.IGNORE_CASE
            ),

            "Q5" to Regex(
                """\bq5\b""",
                RegexOption.IGNORE_CASE
            ),

            "Q7" to Regex(
                """\bq7\b""",
                RegexOption.IGNORE_CASE
            ),

            // BMW
            "1 Series" to Regex(
                """\b1\s*series\b""",
                RegexOption.IGNORE_CASE
            ),

            "2 Series" to Regex(
                """\b2\s*series\b""",
                RegexOption.IGNORE_CASE
            ),

            "3 Series" to Regex(
                """\b3\s*series\b""",
                RegexOption.IGNORE_CASE
            ),

            "4 Series" to Regex(
                """\b4\s*series\b""",
                RegexOption.IGNORE_CASE
            ),

            "5 Series" to Regex(
                """\b5\s*series\b""",
                RegexOption.IGNORE_CASE
            ),

            "6 Series" to Regex(
                """\b6\s*series\b""",
                RegexOption.IGNORE_CASE
            ),

            "7 Series" to Regex(
                """\b7\s*series\b""",
                RegexOption.IGNORE_CASE
            ),

            "X1" to Regex(
                """\bx1\b""",
                RegexOption.IGNORE_CASE
            ),

            "X2" to Regex(
                """\bx2\b""",
                RegexOption.IGNORE_CASE
            ),

            "X3" to Regex(
                """\bx3\b""",
                RegexOption.IGNORE_CASE
            ),

            "X4" to Regex(
                """\bx4\b""",
                RegexOption.IGNORE_CASE
            ),

            "X5" to Regex(
                """\bx5\b""",
                RegexOption.IGNORE_CASE
            ),

            "X6" to Regex(
                """\bx6\b""",
                RegexOption.IGNORE_CASE
            ),

            // Mercedes-Benz
            "A-Class" to Regex(
                """\ba[-\s]?class\b""",
                RegexOption.IGNORE_CASE
            ),

            "B-Class" to Regex(
                """\bb[-\s]?class\b""",
                RegexOption.IGNORE_CASE
            ),

            "C-Class" to Regex(
                """\bc[-\s]?class\b""",
                RegexOption.IGNORE_CASE
            ),

            "E-Class" to Regex(
                """\be[-\s]?class\b""",
                RegexOption.IGNORE_CASE
            ),

            "S-Class" to Regex(
                """\bs[-\s]?class\b""",
                RegexOption.IGNORE_CASE
            ),

            "GLA" to Regex(
                """\bgla\b""",
                RegexOption.IGNORE_CASE
            ),

            "GLC" to Regex(
                """\bglc\b""",
                RegexOption.IGNORE_CASE
            ),

            "GLE" to Regex(
                """\bgle\b""",
                RegexOption.IGNORE_CASE
            ),

            "GLS" to Regex(
                """\bgls\b""",
                RegexOption.IGNORE_CASE
            ),

            // Vauxhall
            "Corsa" to Regex(
                """\bcorsa\b""",
                RegexOption.IGNORE_CASE
            ),

            "Astra" to Regex(
                """\bastra\b""",
                RegexOption.IGNORE_CASE
            ),

            "Insignia" to Regex(
                """\binsignia\b""",
                RegexOption.IGNORE_CASE
            ),

            "Mokka" to Regex(
                """\bmokka\b""",
                RegexOption.IGNORE_CASE
            ),

            "Crossland X" to Regex(
                """\bcrossland(?:\s+x)?\b""",
                RegexOption.IGNORE_CASE
            ),

            "Grandland X" to Regex(
                """\bgrandland(?:\s+x)?\b""",
                RegexOption.IGNORE_CASE
            ),

            // Nissan
            "Micra" to Regex(
                """\bmicra\b""",
                RegexOption.IGNORE_CASE
            ),

            "Juke" to Regex(
                """\bjuke\b""",
                RegexOption.IGNORE_CASE
            ),

            "Qashqai" to Regex(
                """\bqashqai\b""",
                RegexOption.IGNORE_CASE
            ),

            "X-Trail" to Regex(
                """\bx[-\s]?trail\b""",
                RegexOption.IGNORE_CASE
            ),

            "Note" to Regex(
                """\bnote\b""",
                RegexOption.IGNORE_CASE
            ),

            // Toyota
            "Aygo" to Regex(
                """\baygo\b""",
                RegexOption.IGNORE_CASE
            ),

            "Yaris" to Regex(
                """\byaris\b""",
                RegexOption.IGNORE_CASE
            ),

            "Auris" to Regex(
                """\bauris\b""",
                RegexOption.IGNORE_CASE
            ),

            "Corolla" to Regex(
                """\bcorolla\b""",
                RegexOption.IGNORE_CASE
            ),

            "C-HR" to Regex(
                """\bc[-\s]?hr\b""",
                RegexOption.IGNORE_CASE
            ),

            "RAV4" to Regex(
                """\brav4\b""",
                RegexOption.IGNORE_CASE
            ),

            "Prius" to Regex(
                """\bprius\b""",
                RegexOption.IGNORE_CASE
            ),

            // Honda
            "Jazz" to Regex(
                """\bjazz\b""",
                RegexOption.IGNORE_CASE
            ),

            "Civic" to Regex(
                """\bcivic\b""",
                RegexOption.IGNORE_CASE
            ),

            "Accord" to Regex(
                """\baccord\b""",
                RegexOption.IGNORE_CASE
            ),

            "CR-V" to Regex(
                """\bcr[-\s]?v\b""",
                RegexOption.IGNORE_CASE
            ),

            // Renault
            "Clio" to Regex(
                """\bclio\b""",
                RegexOption.IGNORE_CASE
            ),

            "Megane" to Regex(
                """\bmegane\b""",
                RegexOption.IGNORE_CASE
            ),

            "Captur" to Regex(
                """\bcaptur\b""",
                RegexOption.IGNORE_CASE
            ),

            "Kadjar" to Regex(
                """\bkadjar\b""",
                RegexOption.IGNORE_CASE
            ),

            "Scenic" to Regex(
                """\bscenic\b""",
                RegexOption.IGNORE_CASE
            ),

            // Hyundai
            "i10" to Regex(
                """\bi10\b""",
                RegexOption.IGNORE_CASE
            ),

            "i20" to Regex(
                """\bi20\b""",
                RegexOption.IGNORE_CASE
            ),

            "i30" to Regex(
                """\bi30\b""",
                RegexOption.IGNORE_CASE
            ),

            "Tucson" to Regex(
                """\btucson\b""",
                RegexOption.IGNORE_CASE
            ),

            "Kona" to Regex(
                """\bkona\b""",
                RegexOption.IGNORE_CASE
            ),

            // Kia
            "Picanto" to Regex(
                """\bpicanto\b""",
                RegexOption.IGNORE_CASE
            ),

            "Rio" to Regex(
                """\brio\b""",
                RegexOption.IGNORE_CASE
            ),

            "Ceed" to Regex(
                """\bceed\b""",
                RegexOption.IGNORE_CASE
            ),

            "Sportage" to Regex(
                """\bsportage\b""",
                RegexOption.IGNORE_CASE
            ),

            "Sorento" to Regex(
                """\bsorento\b""",
                RegexOption.IGNORE_CASE
            ),

            // Skoda
            "Fabia" to Regex(
                """\bfabia\b""",
                RegexOption.IGNORE_CASE
            ),

            "Octavia" to Regex(
                """\boctavia\b""",
                RegexOption.IGNORE_CASE
            ),

            "Superb" to Regex(
                """\bsuperb\b""",
                RegexOption.IGNORE_CASE
            ),

            "Kodiaq" to Regex(
                """\bkodiaq\b""",
                RegexOption.IGNORE_CASE
            ),

            "Karoq" to Regex(
                """\bkaroq\b""",
                RegexOption.IGNORE_CASE
            ),

            // SEAT
            "Ibiza" to Regex(
                """\bibiza\b""",
                RegexOption.IGNORE_CASE
            ),

            "Leon" to Regex(
                """\bleon\b""",
                RegexOption.IGNORE_CASE
            ),

            "Ateca" to Regex(
                """\bateca\b""",
                RegexOption.IGNORE_CASE
            ),

            "Arona" to Regex(
                """\barona\b""",
                RegexOption.IGNORE_CASE
            ),

            // Mazda
            "Mazda2" to Regex(
                """\bmazda\s*2\b""",
                RegexOption.IGNORE_CASE
            ),

            "Mazda3" to Regex(
                """\bmazda\s*3\b""",
                RegexOption.IGNORE_CASE
            ),

            "Mazda6" to Regex(
                """\bmazda\s*6\b""",
                RegexOption.IGNORE_CASE
            ),

            "CX-3" to Regex(
                """\bcx[-\s]?3\b""",
                RegexOption.IGNORE_CASE
            ),

            "CX-5" to Regex(
                """\bcx[-\s]?5\b""",
                RegexOption.IGNORE_CASE
            ),

            // Volvo
            "C30" to Regex(
                """\bc30\b""",
                RegexOption.IGNORE_CASE
            ),

            "S40" to Regex(
                """\bs40\b""",
                RegexOption.IGNORE_CASE
            ),

            "S60" to Regex(
                """\bs60\b""",
                RegexOption.IGNORE_CASE
            ),

            "S90" to Regex(
                """\bs90\b""",
                RegexOption.IGNORE_CASE
            ),

            "V40" to Regex(
                """\bv40\b""",
                RegexOption.IGNORE_CASE
            ),

            "V60" to Regex(
                """\bv60\b""",
                RegexOption.IGNORE_CASE
            ),

            "V90" to Regex(
                """\bv90\b""",
                RegexOption.IGNORE_CASE
            ),

            "XC40" to Regex(
                """\bxc40\b""",
                RegexOption.IGNORE_CASE
            ),

            "XC60" to Regex(
                """\bxc60\b""",
                RegexOption.IGNORE_CASE
            ),

            "XC90" to Regex(
                """\bxc90\b""",
                RegexOption.IGNORE_CASE
            )
        )

    // ---------------------------------------------------------
    // MAKE
    // ---------------------------------------------------------

    fun extractMake(
        text: String
    ): String? {

        return makePatterns
            .entries
            .firstOrNull { (_, pattern) ->
                pattern.containsMatchIn(text)
            }
            ?.key
    }

    // ---------------------------------------------------------
    // MODEL
    // ---------------------------------------------------------

    fun extractModel(
        text: String
    ): String? {

        if (text.isBlank()) {
            return null
        }

        /*
         * Prefer models containing letters. This prevents a vehicle
         * year such as "2008" from becoming a Peugeot 2008 when the
         * advert already contains a genuine textual model such as
         * "Golf".
         */
        val textualModel =
            modelPatterns
                .entries
                .filter { (model, _) ->
                    model.any { it.isLetter() }
                }
                .firstOrNull { (model, pattern) ->
                    modelMatches(
                        model = model,
                        pattern = pattern,
                        text = text
                    )
                }
                ?.key

        if (textualModel != null) {
            return textualModel
        }

        /*
         * Numeric models in this dictionary are Peugeot models.
         * Only allow the numeric fallback when Peugeot is actually
         * present in the advert.
         *
         * Without this guard, an advert such as:
         *
         *     2008 Volkswagen Golf
         *
         * could incorrectly produce:
         *
         *     Peugeot 2008
         *
         * even though the advert is clearly for a Golf.
         */
        val make = extractMake(text)

        if (!make.equals("Peugeot", ignoreCase = true)) {
            return null
        }

        return modelPatterns
            .entries
            .filter { (model, _) ->
                model.all { it.isDigit() }
            }
            .firstOrNull { (_, pattern) ->
                pattern.containsMatchIn(text)
            }
            ?.key
    }

    private fun modelMatches(
        model: String,
        pattern: Regex,
        text: String
    ): Boolean {

        /*
         * A small number of model names are also extremely common
         * ordinary English words. These need vehicle context rather
         * than a bare word match.
         */
        return when (model.lowercase(Locale.ROOT)) {

            "up" -> {
                Regex(
                    """\bup!\b|\b(?:volkswagen|vw)\s+up\b|\bup\s+(?:volkswagen|vw)\b""",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(text)
            }

            "note" -> {
                Regex(
                    """\b(?:nissan\s+note|note\s+(?:nissan|1\.2|1\.4|1\.5))\b""",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(text)
            }

            else -> pattern.containsMatchIn(text)
        }
    }

    // ---------------------------------------------------------
    // ENGINE SIZE
    // ---------------------------------------------------------

    fun extractEngineSize(
        text: String
    ): String? {

        for (pattern in engineSizePatterns) {

            val match =
                pattern.find(text)
                    ?: continue

            val value =
                match.groupValues
                    .getOrNull(1)
                    ?: continue

            val numeric =
                value
                    .replace(
                        ",",
                        ""
                    )
                    .toDoubleOrNull()
                    ?: continue

            return if (numeric >= 100) {
                "${numeric.toInt()} cc"
            } else {
                "${value} L"
            }
        }

        return null
    }

    // ---------------------------------------------------------
    // FUEL TYPE
    // ---------------------------------------------------------

    fun extractFuelType(
        text: String
    ): String? {

        val lowerText =
            text.lowercase(Locale.ROOT)

        /*
         * Check hybrid/electric terminology before ordinary petrol or
         * diesel terminology. A hybrid advert can legitimately mention
         * petrol or diesel as part of the powertrain description, but
         * the vehicle fuel classification should remain Hybrid/PHEV.
         */
        val plugInHybrid =
            Regex(
                """\b(?:plug[-\s]?in\s+hybrid|phev)\b""",
                RegexOption.IGNORE_CASE
            )

        if (plugInHybrid.containsMatchIn(lowerText)) {
            return "Plug-in Hybrid"
        }

        val hybrid =
            Regex(
                """\bhybrid\b""",
                RegexOption.IGNORE_CASE
            )

        if (hybrid.containsMatchIn(lowerText)) {
            return "Hybrid"
        }

        /*
         * Electric detection deliberately requires vehicle/powertrain
         * context. This avoids treating phrases such as:
         *
         *     electric windows
         *     electric mirrors
         *     electric seats
         *
         * as evidence that the car itself is electric.
         */
        val electricVehiclePatterns =
            listOf(
                Regex(
                    """\b(?:full\s+electric|fully\s+electric|all[-\s]?electric|battery[-\s]?electric)\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\belectric\s+(?:vehicle|car|van|motor|powertrain)\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\b(?:vehicle|car|van)\s+is\s+electric\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\bbev\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\bev\b""",
                    RegexOption.IGNORE_CASE
                )
            )

        if (electricVehiclePatterns.any { it.containsMatchIn(lowerText) }) {
            return "Electric"
        }

        val dieselPatterns =
            listOf(
                Regex("""\bdiesel\b""", RegexOption.IGNORE_CASE),
                Regex("""\btdci\b""", RegexOption.IGNORE_CASE),
                Regex("""\btdi\b""", RegexOption.IGNORE_CASE),
                Regex("""\bhdi\b""", RegexOption.IGNORE_CASE),
                Regex("""\bcrdi\b""", RegexOption.IGNORE_CASE),
                Regex("""\bcdti\b""", RegexOption.IGNORE_CASE),
                Regex("""\bdci\b""", RegexOption.IGNORE_CASE),
                Regex("""\bd4d\b""", RegexOption.IGNORE_CASE),
                Regex("""\bjtd\b""", RegexOption.IGNORE_CASE),
                Regex("""\bmultijet\b""", RegexOption.IGNORE_CASE)
            )

        if (dieselPatterns.any { it.containsMatchIn(lowerText) }) {
            return "Diesel"
        }

        val petrolPatterns =
            listOf(
                Regex("""\bpetrol\b""", RegexOption.IGNORE_CASE),
                Regex("""\bunleaded\b""", RegexOption.IGNORE_CASE),
                Regex("""\btfsi\b""", RegexOption.IGNORE_CASE),
                Regex("""\bfsi\b""", RegexOption.IGNORE_CASE),
                Regex("""\btsi\b""", RegexOption.IGNORE_CASE),
                Regex("""\bvvt[-\s]?i\b""", RegexOption.IGNORE_CASE),
                Regex("""\bvtec\b""", RegexOption.IGNORE_CASE)
            )

        if (petrolPatterns.any { it.containsMatchIn(lowerText) }) {
            return "Petrol"
        }

        return null
    }

    // ---------------------------------------------------------
    // TRANSMISSION
    // ---------------------------------------------------------

    fun extractTransmission(
        text: String
    ): String? {

        val lowerText =
            text.lowercase(Locale.ROOT)

        /*
         * Automatic transmission requires actual gearbox context.
         * In particular, a bare "auto" must NOT classify a vehicle as
         * automatic because adverts commonly contain:
         *
         *     auto lights
         *     auto headlights
         *     auto dimming mirror
         *
         * A bare speed count is also not treated as manual because an
         * automatic gearbox can have 5, 6, 7, 8 or 9 speeds.
         */
        val automaticTransmissionPatterns =
            listOf(
                Regex(
                    """\bautomatic(?!\s+(?:headlights?|lights?|mirror|mirrors|dimming))(?:\s+(?:gearbox|transmission|gear\s*box))?\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\bauto\s+(?:gearbox|transmission|gear\s*box)\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\b(?:dsg|cvt|tiptronic|geartronic|steptronic|s[-\s]?tronic|stronic)\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\bpaddle[-\s]?shift(?:ing)?\b""",
                    RegexOption.IGNORE_CASE
                )
            )

        if (automaticTransmissionPatterns.any { it.containsMatchIn(lowerText) }) {
            return "Automatic"
        }

        val manualTransmissionPatterns =
            listOf(
                Regex(
                    """\bmanual(?:\s+(?:gearbox|transmission|gear\s*box))?\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\bstick\s*shift\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\b(?:3|4|5|6|7|8|9|10)[-\s]?speed\s+manual\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\bmanual\s+(?:3|4|5|6|7|8|9|10)[-\s]?speed\b""",
                    RegexOption.IGNORE_CASE
                ),
                Regex(
                    """\bthree[-\s]?pedal\b""",
                    RegexOption.IGNORE_CASE
                )
            )

        if (manualTransmissionPatterns.any { it.containsMatchIn(lowerText) }) {
            return "Manual"
        }

        return null
    }

    // ---------------------------------------------------------
    // SAFE PHRASE MATCHING
    // ---------------------------------------------------------

    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean {

        val escaped =
            Regex.escape(
                phrase.lowercase(Locale.ROOT)
            )

        return Regex(
            """(?<![a-z0-9])$escaped(?![a-z0-9])""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)
    }
}