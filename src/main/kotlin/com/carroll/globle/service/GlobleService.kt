package com.carroll.globle.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import net.sf.geographiclib.Geodesic
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.geojson.GeoJsonWriter
import org.locationtech.jts.operation.distance.DistanceOp
import org.springframework.stereotype.Service

/** Meter to miles conversion factor */
private const val M_TO_MILES = 0.0006213712

/**
 * This services contains the
 * core functionality for the game.
 * This includes:
 * 1. Generating the target country
 * 2. Computing distances between countries
 * 3. Checking guesses against the target country
 * 4. Returning data on each country in GeoJson format.
 */
@Service
class GlobleGameService(
    private val datasetService: CountryDatasetService
) {

    /** List of country names in the dataset, used for picking the target country. */
    private lateinit var countryNames: MutableList<String>

    /** Current target country, initialized as "None" but is set to a country after dependencies are injected*/
    private var currentTarget: String = "None"

    /** Distance each country is from the target country */
    private var targetDistances: Map<String, Int> = emptyMap()

    /**
     * Initializes the service after dependency injection.
     *
     * Initializes the country names list and generates the first target country.
     */
    @PostConstruct
    fun init() {
        countryNames = datasetService.countriesByKey
            .keys
            .toList().shuffled().toMutableList()

        generateTargetCountry()
    }

    /**
     * Generates a new target country. The new target country
     * is the last element in the country names array. If its empty,
     * its reinitialized and shuffled. After a new country is chosen,
     * the `targetDistances` array is updated to contain the distances
     * to the new target country.
     */
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

    /**
     * Checks if the given guess is a country in the dataset and matches the
     * current target country. Returns data on the guess country in geojson form.
     *
     * Format for the returned JSON:
     *```
     * {
     *   "type": "Feature",
     *   "id": country name,
     *   "geometry": Geometry of country,
     *   "properties": {
     *      "correct": If country is the target,
     *      "distance": Distance to target country,
     *      "center": Rough center of countries polygon,
     *      "country": Name of country
     *    }
     * }
     * ```
     * @param guess The guess to check
     *
     * @return GeoJSON data formatted like above.
     */
    fun checkGuess(guess: String): Map<String, Any> {
        val cleanGuess = guess.trim().lowercase()

        // Get country data or return error message
        val countryData = datasetService.countriesByKey[cleanGuess] ?:
                return mapOf("error" to "'$guess' is not a country in our database")

        // Center of shapes geometry, used to move map to the country
        // The centroid is like the area-weight center
        val center = countryData.second.centroid

        val geojson = mapOf(
            "type" to "Feature",
            "id" to guess,
            "geometry" to geometryToGeoJsonMap(countryData.second),
            "properties" to mapOf(
                "correct" to (guess == currentTarget),
                "distance" to targetDistances[cleanGuess],
                "center" to geometryToGeoJsonMap(center),
                "country" to countryData.first,
            )
        )

        return geojson
    }

    /**
     * Calculates the distance between two geometries. Returns their
     * distance in miles, assuming their original CRS is in WGS84 / Lat and Long values.
     *
     * @param a: Geometry A
     * @param b: Geometry B
     *
     * @return The distance between the two geometries in miles (truncated).
     */
    private fun distanceMiles(a: Geometry, b: Geometry): Int {
        // Nearest points on each geometry to each other
        val pts = DistanceOp.nearestPoints(a, b)
        val p1 = pts[0]
        val p2 = pts[1]

        // Calculates the distance
        val inv = Geodesic.WGS84.Inverse(p1.y, p1.x, p2.y, p2.x)
        val meters = inv.s12 // Distance in meters

        return (meters * M_TO_MILES).toInt()
    }

    /**
     * Helper function to convert a Geometry object to a GeoJsonMap.
     * This is needed because Geometry objects are not automatically serialized to JSON.
     *
     * @param geometry The Geometry to convert
     *
     * @return The geometry converted to a GeoJSON format
     */
    private fun geometryToGeoJsonMap(geometry: Geometry): Map<String, Any?> {
        val objectMapper = jacksonObjectMapper()
        val geoJsonString = GeoJsonWriter().write(geometry) // valid for GeometryCollection too
        @Suppress("UNCHECKED_CAST")
        return objectMapper.readValue(geoJsonString, Map::class.java) as Map<String, Any?>
    }
}