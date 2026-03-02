package com.carroll.globle.service

import jakarta.annotation.PostConstruct
import net.sf.geographiclib.Geodesic
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.operation.distance.DistanceOp
import org.springframework.stereotype.Service

private const val M_TO_MILES = 0.0006213712

@Service
class GlobleGameService(
    private val datasetService: CountryDatasetService
) {

    private lateinit var countryNames: MutableList<String>

    private var currentTarget: String = "None"
    private var targetDistances: Map<String, Int> = emptyMap()

    @PostConstruct
    fun init() {
        countryNames = datasetService.countriesByKey
            .keys
            .toList().shuffled().toMutableList()

        generateTargetCountry()

        println("Current target is $currentTarget")
    }

    fun generateTargetCountry() {
        if (countryNames.isEmpty()) {
            countryNames = datasetService.countriesByKey
                .keys
                .toMutableList().shuffled().toMutableList()
        }

        val newTargetName = countryNames.removeLast()

        val newTarget = datasetService.countriesByKey[newTargetName]

        val targetGeom = newTarget?.second

        // Keeps original key but transforms each value
        targetDistances = datasetService.countriesByKey
            .mapValues { (_, value) ->
                distanceMiles(targetGeom!!, value.second)
            }

        currentTarget = newTargetName
    }

    fun distanceMiles(a: Geometry, b: Geometry): Int {
        if (a.intersects(b)) return 0

        val pts = DistanceOp.nearestPoints(a, b)
        val p1 = pts[0]
        val p2 = pts[1]

        val inv = Geodesic.WGS84.Inverse(p1.x, p1.y, p2.x, p2.y)
        val meters = inv.s12

        return (meters * M_TO_MILES).toInt()
    }

    fun checkGuess(guess: String): Map<String, Any> {
        val cleanGuess = guess.trim().lowercase()

        val countryData = datasetService.countriesByKey[cleanGuess] ?: return mapOf("error" to "Country '$guess' not recognized")

        // Center of shapes geometry, used to move map to the country
        // The centroid is like the area-weight center
        val center = countryData.second.centroid

        val geojson = mapOf(
            "type" to "Feature",
            "id" to guess,
            "geometry" to countryData.second,
            "properties" to mapOf(
                "correct" to (guess == currentTarget),
                "distance" to targetDistances[guess],
                "center" to center,
                "country" to countryData.first,
            )
        )

        return geojson
    }
}