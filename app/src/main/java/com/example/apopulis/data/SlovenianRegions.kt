package com.example.apopulis.data

import com.example.apopulis.model.Region
import com.google.android.gms.maps.model.LatLng

object SlovenianRegions {

    fun getAllRegions(): List<Region> = listOf(
        pomurska,
        podravska,
        koroska,
        savinjska,
        zasavska,
        posavska,
        jugovzhodna,
        osrednjeslovenska,
        gorenjska,
        primorskoNotranjska,
        goriska,
        obalnoKraska
    )

    val pomurska = Region(
        "pomurska", "Pomurska",
        listOf(
            LatLng(46.85, 16.00),
            LatLng(46.75, 16.40),
            LatLng(46.55, 16.40),
            LatLng(46.55, 15.95),
            LatLng(46.75, 15.90)
        )
    )

    val podravska = Region(
        "podravska", "Podravska",
        listOf(
            LatLng(46.65, 15.40),
            LatLng(46.55, 16.00),
            LatLng(46.35, 16.00),
            LatLng(46.30, 15.40),
            LatLng(46.50, 15.20)
        )
    )

    val koroska = Region(
        "koroska", "Koroška",
        listOf(
            LatLng(46.65, 14.50),
            LatLng(46.65, 15.00),
            LatLng(46.45, 15.00),
            LatLng(46.45, 14.40)
        )
    )

    val savinjska = Region(
        "savinjska", "Savinjska",
        listOf(
            LatLng(46.45, 14.90),
            LatLng(46.45, 15.50),
            LatLng(46.15, 15.50),
            LatLng(46.10, 14.90)
        )
    )

    val zasavska = Region(
        "zasavska", "Zasavska",
        listOf(
            LatLng(46.15, 14.90),
            LatLng(46.15, 15.20),
            LatLng(45.95, 15.20),
            LatLng(45.95, 14.90)
        )
    )

    val posavska = Region(
        "posavska", "Posavska",
        listOf(
            LatLng(46.05, 15.20),
            LatLng(46.05, 15.60),
            LatLng(45.80, 15.60),
            LatLng(45.80, 15.20)
        )
    )

    val jugovzhodna = Region(
        "jugovzhodna", "Jugovzhodna Slovenija",
        listOf(
            LatLng(45.95, 14.80),
            LatLng(45.95, 15.40),
            LatLng(45.60, 15.40),
            LatLng(45.60, 14.80)
        )
    )

    val osrednjeslovenska = Region(
        "osrednjeslovenska", "Osrednjeslovenska",
        listOf(
            LatLng(46.25, 14.30),
            LatLng(46.25, 14.80),
            LatLng(45.95, 14.80),
            LatLng(45.95, 14.30)
        )
    )

    val gorenjska = Region(
        "gorenjska", "Gorenjska",
        listOf(
            LatLng(46.55, 13.80),
            LatLng(46.55, 14.40),
            LatLng(46.25, 14.40),
            LatLng(46.25, 13.80)
        )
    )

    val primorskoNotranjska = Region(
        "primorsko_notranjska", "Primorsko-notranjska",
        listOf(
            LatLng(45.85, 13.90),
            LatLng(45.85, 14.40),
            LatLng(45.55, 14.40),
            LatLng(45.55, 13.90)
        )
    )

    val goriska = Region(
        "goriska", "Goriška",
        listOf(
            LatLng(46.35, 13.30),
            LatLng(46.35, 13.80),
            LatLng(46.05, 13.80),
            LatLng(46.05, 13.30)
        )
    )

    val obalnoKraska = Region(
        "obalno_kraska", "Obalno-kraška",
        listOf(
            LatLng(45.85, 13.50),
            LatLng(45.85, 13.90),
            LatLng(45.55, 13.90),
            LatLng(45.45, 13.60),
            LatLng(45.55, 13.30)
        )
    )
}

