# Predstavitveni opis mobilne aplikacije

Mobilna aplikacija omogoča **registracijo in prijavo uporabnikov**, s čimer zagotovimo personaliziran dostop do vsebin.

## Interaktivni zemljevid novic
Po uspešni prijavi se uporabniku prikaže **interaktivni zemljevid Slovenije**, na katerem so prikazane vse novice skupaj z jasno označenimi regijami.

- Regije so na zemljevidu vizualno poudarjene.
- Ob kliku na posamezno regijo se ta označi s spremembo barve.
- Hkrati se prikažejo novice, ki pripadajo izbranemu geografskemu območju.

Na spodnjem delu zaslona se nahaja **seznam novic v obliki drsnega seznama (RecyclerView)**, ki ga lahko uporabnik povleče navzgor in si ogleda vse razpoložljive novice.

## Pregled in filtriranje novic
Uporabnik lahko klikne na katerokoli novico v seznamu in odpre njen **podrobni prikaz**, kjer so prikazane vse informacije o dogodku.

Aplikacija omogoča tudi:
- filtriranje novic po kategorijah,
- dinamično posodabljanje seznama novic glede na izbrano kategorijo.

## Simulirano delovanje senzorja komentarjev
V aplikaciji je implementirano **simulirano delovanje senzorja komentarjev**. Uporabnik lahko nastavi:
- časovni interval,
- število komentarjev,
- lokacijo oziroma regijo.

Simulirani komentarji se obravnavajo kot **dogodki v digitalnem dvojčku**.

## Zaznavanje viralnih novic
Če število komentarjev pri posamezni novici preseže vnaprej določen prag, aplikacija:
- zazna viralno novico,
- uporabniku pošlje obvestilo.

Na ta način je uporabnik pravočasno obveščen o novicah, ki postajajo viralne, in jih lahko takoj prebere.

## Objavljanje novih novic in preverjanje slik
Aplikacija omogoča tudi **objavo nove novice**. Uporabnik lahko:
- doda sliko,
- doda opis novice.

Slika se preveri z uporabo **umetne inteligence**, ki ugotovi, ali je slika:
- resnična,
- ali generirana z umetno inteligenco.

Če je slika zaznana kot AI-generirana, uporabnik prejme opozorilo in se lahko odloči, ali bo novico zavrnil ali vseeno objavil.

Po objavi se novica prikaže:
- v seznamu novic,
- na interaktivnem zemljevidu.
