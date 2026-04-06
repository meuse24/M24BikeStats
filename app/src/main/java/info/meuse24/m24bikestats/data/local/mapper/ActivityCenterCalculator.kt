package info.meuse24.m24bikestats.data.local.mapper

import kotlin.math.cos
import kotlin.math.PI

object ActivityCenterCalculator {

    /**
     * Gibt den GPS-Punkt zurück, der vom Startpunkt am weitesten entfernt liegt.
     * Bei Rundtouren entspricht dies typischerweise dem Streckenziel.
     * Gibt null zurück, wenn keine validen GPS-Punkte vorhanden sind.
     *
     * Distanzberechnung: euklidisch mit Cosinus-Korrektur für Längengrade
     * (ausreichend genau für Vergleichszwecke, kein Haversine nötig).
     */
    fun calculate(
        points: List<Pair<Double, Double>> // (lat, lng)
    ): Pair<Double, Double>? {
        val valid = points.filter { (lat, lng) -> lat != 0.0 && lng != 0.0 }
        if (valid.isEmpty()) return null
        val start = valid.first()
        val cosLat = cos(start.first * PI / 180.0)
        return valid.maxByOrNull { (lat, lng) ->
            val dLat = lat - start.first
            val dLng = (lng - start.second) * cosLat
            dLat * dLat + dLng * dLng  // sqrt entfällt, da nur Vergleich
        }
    }
}
