package org.example.nlp

object LocationRules {
    val locationKeywords = mapOf(
        "Ljubljana" to listOf(
            "ljubljana", "center", "trnovo", "vič", "bežigrad", "šentvid", "rudnik", "stožice", "rožna dolina",
            "barje", "fužine", "zalog", "tivoli", "kongresni trg", "mestna občina ljubljana"
        ),
        "Maribor" to listOf(
            "maribor", "lent", "tabor", "tezno", "pobrežje", "melje", "studenci", "betnava", "mestna občina maribor"
        ),
        "Koper" to listOf(
            "koper", "capodistria", "izola", "piran", "portorož", "lucija", "ankaran", "istrska obala", "obalno-kraška"
        ),
        "Celje" to listOf(
            "celje", "lava", "začret", "mestna občina celje", "zagaj", "šmartno v rožni dolini", "žalec", "slovenske konjice"
        ),
        "Nova Gorica" to listOf(
            "nova gorica", "šempeter pri gorici", "vrtojba", "rožna dolina", "soča", "goriška", "solkan", "mestna občina nova gorica"
        ),
        "Gorenjska" to listOf(
            "kranj", "jesenice", "tržič", "radovljica", "bled", "bohinj", "bohinjska bistrica", "gorenjska", "triglav", "kropa", "železniki"
        ),
        "Primorska" to listOf(
            "postojna", "ajdovščina", "vipava", "sežana", "komenda", "črni kal", "kozina", "primorska", "primorski rob"
        ),
        "Murska Sobota" to listOf(
            "murska sobota", "prekmurje", "radenci", "lendava", "gornja radgona", "puconci", "turnišče", "beltinci"
        ),
        "Novo Mesto" to listOf(
            "novo mesto", "dolenjska", "oštrc", "mestna občina novo mesto", "šentjernej", "šentrupert", "stopiče"
        ),
        "Ptuj" to listOf(
            "ptuj", "slovenske gorice", "ormož", "gorišnica", "spodnje podravje", "drava", "haloze"
        ),
        "Velenje" to listOf(
            "velenje", "šaleška dolina", "šalek", "mestna občina velenje", "šmartno ob paki"
        ),
        "Trbovlje" to listOf(
            "trbovlje", "zasavje", "zagorje ob savi", "hrastnik", "zasavska regija"
        ),
        "Bovec" to listOf(
            "bovec", "trenta", "soča", "kanin", "kobariški muzej", "log pod mangartom", "tolmin", "zdiar"
        ),
        "Krško" to listOf(
            "krško", "brežice", "posavje", "jedrska elektrarna", "nuklearna elektrarna", "mestna občina krško"
        ),
        "splošno" to listOf(
            "slovenija", "država", "regija", "občina", "mesto", "naselje", "slovenski"
        )
    )
}
