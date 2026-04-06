package info.meuse24.m24bikestats.data.local.mapper

object ActivityCenterCalculator {

    /**
     * Gibt den geometrischen Mittelpunkt der Bounding Box zurück,
     * oder null wenn keine GPS-Punkte vorhanden sind.
     */
    fun calculate(
        points: List<Pair<Double, Double>> // (lat, lng)
    ): Pair<Double, Double>? {
        val valid = points.filter { (lat, lng) ->
            lat != 0.0 && lng != 0.0
        }
        if (valid.isEmpty()) return null
        val minLat = valid.minOf { it.first }
        val maxLat = valid.maxOf { it.first }
        val minLng = valid.minOf { it.second }
        val maxLng = valid.maxOf { it.second }
        return Pair((minLat + maxLat) / 2.0, (minLng + maxLng) / 2.0)
    }
}
