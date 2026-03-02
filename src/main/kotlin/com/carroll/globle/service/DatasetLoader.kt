package com.carroll.globle.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.annotation.PostConstruct
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.geojson.GeoJsonReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.BufferedReader

/**
 * This service handles loading the GeoJson file containing the data for each country.
 * The data is loaded on start-up of the app after dependency injection has finished.
 * The data is stored in a public read-only attribute so it's easily accessible.
 */
@Service
class CountryDatasetService(
    @Value("\${globle.dataset}") private val datasetResource: Resource
) {
    /** In-Memory storage of country data. Key is cleaned name, value is a
     * pair of the long name of the country and its geometry.
     */
    lateinit var countriesByKey: Map<String, Pair<String, Geometry>>

    /** Object mapper for reading JSON */
    private val objectMapper = jacksonObjectMapper()

    /**
     * Loads country data and stores it in memory. `PostConstruct` annotation causes
     * this to be initialized after dependency injection so the data is ready to be used.
     */
    @PostConstruct
    fun init() {
        val json = datasetResource.inputStream.bufferedReader().use(BufferedReader::readText)

        val root = objectMapper.readTree(json)

        // Ensure the data is a feature collection
        if (root["type"]?.asText() != "FeatureCollection") {
            throw IllegalArgumentException("Invalid GeoJSON: Expected a FeatureCollection")
        }

        val reader = GeoJsonReader()

        // Temp storage for country data
        val map = mutableMapOf<String, Pair<String, Geometry>>()

        // Load each countries name and geometry data
        root["features"].forEach { feature ->
            val name = feature["properties"]["name"].asText()
            val cleaned = name.trim().lowercase()

            val nameLong = feature["properties"]["name_long"].asText()

            val geometryJson = feature["geometry"].toString()
            val geometry = reader.read(geometryJson)

            map[cleaned] = nameLong to geometry
        }

        // Convert to read-only map
        countriesByKey = map.toMap()

        println("Loaded ${countriesByKey.size} countries")
    }

}