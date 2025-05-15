fun main() {
    val testInputs = listOf(
        """
        city "MyCity" {
            road "MainStreet" {
                line((0.0), (100.0))
            }
            building "TownHall" {
                box((50.50), (150.150))
            }
        }
        """,
        """
        city "BigCity" {
            road "Highway1" {
                line((0.0), (300.0));
                bend((300.0), (300.100));
            }
            building "Mall" {
                box((100.100), (200.200));
            }
            building "Library" {
                circ((250.250), 20);
            }
        }
        """,
        """
        city "NatureCity" {
            park "CentralPark" {
                box((10.10), (100.100));
            }
            lake "BlueLake" {
                circ((150.150), 50);
            }
        }
        """,
        """
        city "TrafficCity" {
            junction "MainJunction" {
                marker("StopSign");
            }
        }
        """,
        """
        let center = (100.100);
        foreach road in "CityRoads" {
            translate(road, (10.0));
        }
        """,
        """
        unknown "Mystery" {
            line((0.0), (10.10));
        }
        """,
        """
    REGION, CITY, ROAD, BUILDING, NEWS, PARK, LAKE,
    JUNCTION, MARKER, PROCEDURE,
    UNKNOWN, LET, FOREACH, IN, IF, ELSE, FOR, TO ,
    LINE, BEND, BOX, CIRC, TRANSLATE, CHECK, VALIDATE,
    OPERATOR, FST, SND, NIL, SET,
        """
    )

    testInputs.forEachIndexed { index, input ->
        println("\n=== Test ${index + 1} ===")
        val inputStream = input.trimIndent().byteInputStream()
        val scanner = Scanner(inputStream)

        while (!scanner.eof()) {
            val token = scanner.nextToken()
            println(token)
        }
    }
}