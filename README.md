# Globle

This project recreates the [Globle](https://globle-game.com/game) game, where you try to guess the mystery country in as few guesses as possible.

This project is the exact same as my [Python Globle](https://github.com/CanFam23/Globle-Python/tree/main)
project, but the backend is written in Kotlin with the help of Spring Boot.

The user can guess a country, and it will be colored on the map (Thanks Mapbox!) based on how far away it is from the target country.

**Key Features:**

* Interactive world map powered by Mapbox GL JS
* Dynamic distance-based coloring to guide player guesses
* Tracks player progress, high scores, and past guesses
* Python backend handles game logic and country selection
* Real time country name completion
  * This is a feature I wish the real Globle had especially for those who don't have their countries memorized... but I get why it doesn't have it.

**Technologies Used:**

* Kotlin & Spring Boot (game logic, distance calculations)
* Mapbox GL JS (map rendering and visualization)
* GeoJSON (country geometries and map data)
* JavaScript & HTML/CSS (frontend interactivity, some game logic)

**Gameplay Summary:**

1. The game randomly selects a target country.
2. Players enter country guesses through the web UI.
3. Each guess is plotted on the map and colored based on distance to the target.
4. Feedback helps players refine guesses until the target is found (or not).

# Prerequisites

* **Java 21+** (recommended for Spring Boot 4)
  * Not tested with other versions
* **Gradle** (or use the included Gradle Wrapper `./gradlew`)
* **Kotlin** (typically handled by Gradle; no separate install needed beyond a JDK)
* **Mapbox public token (`pk.*`)** from [Mapbox](https://www.mapbox.com/)

## Mapbox token configuration

Put your Mapbox token on line 25 the `scrpt.js` file in the **src/main/resources/static** directory of the project:

```txt
mapboxgl.accessToken = 'your key here';
```

> Note: For browser apps, the token will be visible in the client at runtime. You should restrict the token in Mapbox (allowed URLs/referrers) and keep scopes minimal.

# Setup

## Install dependencies

From the project root, run:

```bash
./gradlew build
```

(Windows: `gradlew.bat build`)

# Running the app

## Run with Gradle

```bash
./gradlew bootRun
```

(Windows: `gradlew.bat bootRun`)

# Accessing the app

By default, Spring Boot runs on port **8080**. Once running, open:

* `http://localhost:8080/`

If you changed the port (via `server.port`), use that instead.

## Typical console output

You should see log lines indicating Spring Boot started successfully, including something like:

* `Tomcat started on port(s): 8080`
* `Started <ApplicationName> in ... seconds`

# Known Issues / Limitations

* Tested with **Java 21 / Spring Boot 4** (other versions may require changes).
* Mapbox integration requires:

  * A valid **`pk.*` token**
  * Internet access
  * Proper token restrictions (allowed URLs/referrers) to reduce abuse risk.
* The distance between countries isn't super accurate. I used a similar method 
to one I'd made for another project, but it's hard to find a mutually agreed-upon algorithm for computing the distances with Python without writing a bunch of my own code. (Not in the scope of this project, sorry :( ).


# Citations
[https://geojson-maps.kyd.au/](https://geojson-maps.kyd.au/)  
GeoJSON Data, I did 50 meter resolution with all regions except for 'Other'.

[https://docs.mapbox.com/#maps](https://docs.mapbox.com/#maps)  
Pretty awesome documentation that makes it easy to create my own map.

[https://github.com/CanFam23/BookRecommender](https://github.com/CanFam23/BookRecommender)  
I reused some of my old code for creating the datalist that shows countries the user can choose from. I also used essentially the same CSS styling.
