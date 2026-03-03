package com.carroll.globle.controller

import com.carroll.globle.service.CountryDatasetService
import com.carroll.globle.service.GlobleGameService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** Simple data class used when a guess is posted. */
data class Guess(
    /** Guess string */
    val guess: String
)

/**
 * This controller contains all the endpoints for the Globle game. These are:
 * 1. `/getCountries` **GET** - Returns a list of all country names in the dataset (All lower case).
 * 2. `/newGame` **GET** - Generates a new target country.
 * 3. `/checkGuess` **POST** - Checks the given `guess` string to see if it's the target country.
 *
 */
@RestController
class GlobleController (
    private val globalService: GlobleGameService,
    private val datasetService: CountryDatasetService
){

    /**
     * Gets a list of all countries in the dataset. Each string is trimmed and contains
     * only lowercase letters.
     *
     * Data format:
     * ```
     * {
     *   "countries": ["united states", "mexico", ...]
     * }
     * ```
     */
    @GetMapping("/getCountries")
    fun getCountries(): Map<String, List<String>> {
        return mapOf(
            "countries" to datasetService.countriesByKey.keys.toList()
        )
    }

    /**
     * Generates a new target country. Returns a simple JSON object
     * to signal a new country has been successfully generated.
     *
     * Data format:
     * ```
     * {
     *   "country_generated": true
     * }
     * ```
     */
    @PostMapping("/newGame")
    fun newGame(): Map<String, Boolean> {
        globalService.generateTargetCountry()

        return mapOf("country_generated" to true)
    }


    /**
     * Checks if the given `guess` string is equal to the name of the target country.
     * Returns a GeoJSON object.
     *
     *
     * Body format:
     * ```
     * {
     *   "guess": "united states" <- user's guess
     * }
     * ```
     *
     * Returned data format:
     * ```
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
     */
    @PostMapping("/checkGuess")
    @ResponseStatus(HttpStatus.CREATED)
    fun checkGuess(@RequestBody request: Guess): Map<String, Any> {
        val guess = request.guess.trim().lowercase()

        return globalService.checkGuess(guess)
    }
}