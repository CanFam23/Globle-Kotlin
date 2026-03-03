/** Stores current number of guesses. */
let numGuesses = 0;

let bestGuesses = Number(sessionStorage.getItem("bestScore")) || 0;
let bestCountry = sessionStorage.getItem("bestCountry") || "";

document.getElementById("bestScore").innerText = `Best Score: ${bestGuesses}`;
document.getElementById("bestCountry").innerText = `Country: ${bestCountry}`;

/** Stores past guesses */
let pastGuesses = [];

/** Empty GeoJson used to reset the map */
let emptyCollection = {
  type: "FeatureCollection",
  features: [],
};

/**
 * Creates a new Mapbox map and sets it up basically
 * @returns Map instance created
 */
async function initMap() {
  // This is a public key, its will be exposed on the frontend
  mapboxgl.accessToken = 'your key here';

  const map = new mapboxgl.Map({
    container: "map",
    style: "mapbox://styles/mapbox/standard",
    projection: "globe",
    zoom: 1,
    center: [30, 15],
    config: {
      basemap: {
        // Disable anything that could help the user guess the country
        showPedestrianRoads: false,
        showPointOfInterestLabels: false,
        showPlaceLabels: false,
        showTransitLabels: false,
        font: "Inter",
        showLandmarkIconLabels: true,
      },
    },
  });

  map.addControl(new mapboxgl.NavigationControl());

  map.on("style.load", () => {
    map.setFog({}); // Set the default atmosphere style
  });

  map.on("load", () => {
    // Empty GeoJSON source
    map.addSource("countries", {
      type: "geojson",
      dynamic: true,
      data: {
        type: "FeatureCollection",
        features: [],
      },
    });

    // One fill layer, color driven by distance
    map.addLayer({
      id: "countries-fill",
      type: "fill",
      source: "countries",
      paint: {
        "fill-opacity": 0.8,
        "fill-color": [
          "interpolate",
          ["linear"],
          ["get", "distance"],
          0,
          "#8c0000",
          750,
          "#ff0000",
          1500,
          "#ffb300",
          3000,
          "#fffd91",
        ],
      },
    });

    map.addSource("answer", {
      type: "geojson",
      dynamic: true,
      data: {
        type: "FeatureCollection",
        features: [],
      },
    });

    map.addLayer({
      id: "answer-fill",
      type: "fill",
      source: "answer",
      paint: {
        "fill-opacity": 1,
        "fill-color": "#0ace00",
      },
    });

    // outline
    map.addLayer({
      id: "countries-outline",
      type: "line",
      source: "countries",
    });

    map.addLayer({
      id: "answer-outline",
      type: "line",
      source: "answer",
    });
  });

  return map;
}

// The map
let map;

// Wait for map initialization
initMap().then((m) => {
  map = m;
});

// Add functionality to button that prompts user for new game
document.addEventListener("DOMContentLoaded", function () {
  document
    .getElementById("newGameBtn")
    .addEventListener("click", function (event) {
      event.preventDefault();

      // Generate a new target country
      fetch("/newGame", {
        method: "POST",
      })
        .then((response) => response.json())
        .then((data) => {
          if (!data) {
            setMessage(
              "Something went wrong, maybe try turning it off and back on again?",
            );
            return;
          }
          resetGame();
        });
    });
});

let countryNames;

document.addEventListener("DOMContentLoaded", function () {
  // Fetch the list of country names
  fetch("/getCountries", {
    method: "GET",
  })
    .then((response) => response.json()) // Convert response to JSON
    .then((data) => {
      countryNames = data.countries; // Store countries globally
    })
    .catch((error) => {
      // Handle fetch errors (Very robust)
      console.error("Error fetching country list:", error);
      setMessage(
        "Something went wrong, maybe try turning it off and back on again?",
      );
    });
});

// Event listener for search suggestions
document.addEventListener("DOMContentLoaded", () => {
  const searchBox = document.getElementById("searchBox");
  const dataList = document.getElementById("suggestions");

  searchBox.addEventListener("keyup", () => {
    if (!Array.isArray(countryNames)) return;

    const query = searchBox.value.trim().toLowerCase();
    dataList.innerHTML = "";

    if (query.length === 0) return;

    // Filter for countries starting with current search term
    const filteredCountries = countryNames
      .filter((b) => b.toLowerCase().startsWith(query))
      .slice(0, 15);

    // Add matching countries to data list
    filteredCountries.forEach((title) => {
      const op = document.createElement("option");
      op.value = title;
      dataList.appendChild(op);
    });
  });
});

// Event listener for form submission
document.addEventListener("DOMContentLoaded", function () {
  document
    .getElementById("countryForm")
    .addEventListener("submit", function (event) {
      event.preventDefault(); // Prevent form reload

      // Check for duplicate guess
      let guessValue = document
        .getElementById("searchBox")
        .value.toLowerCase()
        .trim();

      for (const c of pastGuesses) {
        if (guessValue.toLowerCase() === c.country.toLowerCase()) {
          setMessage(`${guessValue} has already been guessed.`);
          return;
        }
      }

      // Send a POST request to check answer
      fetch("/checkGuess", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ guess: guessValue }),
      })
        .then((response) => response.json()) // Convert response to JSON
        .then((data) => {
          // Very good error handling
          if (!data || data.error) {
            setMessage(
              `${guessValue} is not recognized as a country in our records...`,
            );
            return;
          }

          numGuesses++;
          updateGuesses();
          clearSearchBox();

          if (data.properties.correct) {
            map.getSource("answer").setData(data);
            correct(data);
          } else {
            map.getSource("countries").updateData(data);
            incorrect(data);
          }
        })
        .catch((error) => console.error("Error:", error)); // Handle fetch errors
    });
});

/**
 * Resets the game
 */
function resetGame() {
  numGuesses = 0;
  setMessage("");

  // Clear map data
  map.getSource("answer").setData(emptyCollection);
  map.getSource("countries").setData(emptyCollection);

  map.flyTo({
    center: [30, 15],
    zoom: 1,
    speed: 0.8,
    curve: 1,
  });

  document.getElementById("newGameBtn").style.display = "none";

  document.getElementById("submitBtn").disabled = false;

  document.getElementById("pastGuesses").innerHTML = "";

  pastGuesses = [];

  updateGuesses();
  clearSearchBox();
}

/** Updates number of guesses to numGuesses */
function updateGuesses() {
  document.getElementById("numGuess").innerText =
    `Number of guesses: ${numGuesses}`;
}

/** Clears search box */
function clearSearchBox() {
  document.getElementById("searchBox").value = "";
}

/**
 * Tells user they got the target country and prompts them for a new game.
 * @param {*} data GeoJSON formatted object with properties center, country, distance
 */
function correct(data) {
  map.flyTo({
    center: data.properties.center.coordinates,
    zoom: 2,
    speed: 0.8,
    curve: 1,
  });

  setMessage(`${data.properties.country} is the target country!`);

  document.getElementById("newGameBtn").style.display = "block";

  document.getElementById("submitBtn").disabled = true;

  updateBestScore(numGuesses, data.properties.country);
}

/**
 * Flys to country in given GeoJSON, fills its color based on distance,
 * and tells user how far away guess is to target.
 * @param {json} data GeoJSON formatted object with properties center, country, distance
 */
function incorrect(data) {
  map.flyTo({
    center: data.properties.center.coordinates,
    zoom: 1.5,
    speed: 0.8,
    curve: 1,
  });

  updatePastGuesses({
    country: data.properties.country,
    distance: data.properties.distance,
  });

  setMessage(
    `${data.properties.country} is ${data.properties.distance === 0 ? "adjacent to" : data.properties.distance + " mi from"} the target country`,
  );
}

/**
 * Displays given message on page.
 * @param {string} msg
 */
function setMessage(msg) {
  document.getElementById("msg").innerText = msg;
}

/**
 * Updates list of past guesses and 'rerenders' it.
 * Guesses are sorted by distance to target.
 * @param {json} newGuess New guess to add
 */
function updatePastGuesses(newGuess) {
  pastGuesses.push(newGuess);

  pastGuesses.sort((a, b) => a.distance - b.distance);

  const passGuessesHTML = pastGuesses
    .map(
      (country) => `
  <li>${country.country} - ${country.distance} mi </li>
  `,
    )
    .join("");

  document.getElementById("pastGuesses").innerHTML = passGuessesHTML;
}

/**
 * Updates the best score if the new score is better. If so,
 * also updates session storage to store score across sessions.
 * @param {number} newScore
 * @param {number} country
 */
function updateBestScore(newScore, country) {
  if (bestGuesses === 0 || newScore < bestGuesses) {
    bestGuesses = newScore;
    bestCountry = country;

    document.getElementById("bestScore").innerText =
      `Best Score: ${bestGuesses}`;
    document.getElementById("bestCountry").innerText =
      `Country: ${bestCountry}`;

    sessionStorage.setItem("bestScore", bestGuesses);
    sessionStorage.setItem("bestCountry", bestCountry);
  }
}
