package nlp

object CategoryRules {
    val categoryKeywords = mapOf(

        "politika" to listOf(
            "vlada", "zakon", "ministrstvo", "predsednik", "poslanec", "poslanka", "parlament",
            "glasovanje", "koalicija", "opozicija", "državni zbor", "ustava", "reforma", "referendum",
            "minister", "ministrica", "kandidat", "volitve", "kampanja", "političen", "sodnik", "tožilec",
            "država", "oblast", "funkcija", "kabinet", "pristojnost", "komisija", "sekretar", "politična stranka",
            "državni svet", "zunanja politika", "notranje zadeve", "mandat", "ustavni sodnik", "pravna država",
            "zakonodaja", "izvolitev", "glas", "koalicijski partner", "državni uradnik"
        ),

        "gospodarstvo" to listOf(
            "gospodarstvo", "trg", "podjetje", "podjetništvo", "firma", "dohodek", "prihodek", "denar",
            "financa", "davki", "evro", "banka", "inflacija", "kredit", "posojilo", "naložba",
            "zaposlitev", "zaposlovanje", "plača", "izvoz", "uvoz", "gospodarska rast", "borza", "delnica",
            "zavarovanje", "bruto", "neto", "gospodarski kazalnik", "obrestna mera", "finančna stabilnost",
            "kapital", "tržno gospodarstvo", "konjunktura", "recesija", "potrošnja", "proizvodnja",
            "subvencija", "proračun", "koncern", "monetarna politika", "gospodarski minister"
        ),

        "šport" to listOf(
            "šport", "tekma", "tekmovanje", "gol", "liga", "klub", "ekipa", "reprezentanca", "športnik", "športnica",
            "igralec", "igralka", "trening", "stadion", "rezultat", "poraz", "zmaga", "točke", "turnir", "medalja",
            "nogomet", "košarka", "tenis", "plavanje", "plavalec", "smučanje", "smučar", "gimnastika", "kolesarstvo",
            "atletika", "hokej", "borilne veščine", "borba", "boks", "judo", "rolanje", "vaterpolo", "skoki", "tek",
            "maraton", "olimpijada", "rekord", "drsanje", "curling", "golman", "napadalec", "obrambni", "trener"
        ),

        "kultura" to listOf(
            "kultura", "film", "kino", "gledališče", "predstava", "igra", "igralec", "režiser", "avtor", "umetnost",
            "umetnik", "glasba", "glasbenik", "koncert", "festival", "razstava", "knjiga", "literatura", "pesem", "pesnik",
            "ples", "kulturni", "muzej", "scena", "dogodek", "publika", "založba", "klasična glasba", "sodobna umetnost",
            "likovna umetnost", "fotografija", "kultura naroda", "zgodovina umetnosti", "drama", "performans",
            "poezija", "roman", "zgodovinski roman", "strip", "kulturna dediščina", "arhitektura"
        ),

        "tehnologija" to listOf(
            "tehnologija", "računalnik", "telefon", "mobilni", "aplikacija", "internet", "splet", "program", "programiranje",
            "koda", "kodiranje", "AI", "umetna inteligenca", "algoritem", "pametni telefon", "računalništvo", "digitalno",
            "družbena omrežja", "elektronika", "blockchain", "kriptovaluta", "robot", "IT", "software", "hardware",
            "tehnološki napredek", "inovacija", "varnost", "hekanje", "geslo", "računalniški sistem",
            "oblak", "računalniški virus", "spletna stran", "domena", "server", "računalniški program", "operacijski sistem",
            "spletna aplikacija", "analitika", "računalniška oprema"
        ),

        "vreme" to listOf(
            "vreme", "vremenska napoved", "nevihta", "dež", "padavine", "plohe", "sneg", "led", "toča", "sonce",
            "sončno", "hladno", "mraz", "vročina", "temperatura", "ohladitev", "nebo", "oblačno", "jasno", "zmrzal",
            "zofka", "poletje", "zima", "pomlad", "jesen", "suša", "poplava", "klima", "veter", "vlažnost", "UV indeks",
            "nevihtno", "sprememba vremena", "poslabšanje vremena", "sončni vzhod", "sončni zahod",
            "atmosferski tlak", "meteorolog", "napovedovalec vremena", "občutek mraza"
        ),

        "biznis" to listOf(
            "posel", "poslovanje", "podjetje", "podjetništvo", "startup", "start-up", "naložba", "investicija",
            "investitor", "kapital", "financiranje", "donacija", "računovodstvo", "bilanca", "izkaz", "prihodek",
            "stroški", "dobiček", "izguba", "zaposlovanje", "kariera", "vodja", "direktor", "manager", "vodstvo",
            "konkurenca", "analiza trga", "trg dela", "prodaja", "marketing", "trženje", "pitch", "investicijska priložnost",
            "donos", "izvoz", "uvoz", "dobavitelj", "naročnik", "projekt", "konferenca", "poslovni model", "delavnica",
            "mentor", "poslovni svetovalec", "komercialist", "zagon podjetja", "ustvarjanje vrednosti", "sodelovanje",
            "pogajanja", "poslovni načrt", "tržni delež", "ekonomski kazalci", "poslovni rezultat", "prevoz",
            "uber", "platforma", "aplikacija", "voznik", "taksi"
        ),

        "lifestyle" to listOf(
            "zdravje", "prehrana", "dieta", "vadba", "fitnes", "joga", "spanje", "počitek", "mentalno zdravje",
            "sprostitev", "rekreacija", "rutina", "motivacija", "samopomoč", "osebna rast", "psihohigiena",
            "moda", "stil", "oblačila", "kozmetika", "make-up", "nega kože", "lepota", "trend", "frizura",
            "potovanja", "dopust", "turizem", "počitnice", "avantura", "planiranje potovanja", "izlet", "letalo",
            "družina", "partnerstvo", "odnosi", "zmenki", "poroka", "otroci", "starševstvo", "nasveti", "zabava",
            "hobiji", "kultura bivanja", "notranja oprema", "domačnost", "wellness", "spa", "ravnovesje", "lifestyle sprememba"
        ),

        "splošno" to listOf(
            "dogodek", "dan", "ljudje", "družba", "novica",
            "država", "življenje", "občani", "splošno", "dnevno dogajanje",
            "vlomi", "tatvina", "kriminal", "pohodniki", "izlet", "hribolazci", "avtomobil", "izletniška točka", "parkirišče"
        )

    )
}
