package test.pcf.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.Identity.decode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import javax.imageio.ImageTranscoder
import java.util.UUID;

@Serializable
data class TrainSchedule(
    val trainNumber: String,
    val fromStation: String,
    val toStation: String,
    val departureTime: String,
    val arrivalTime: String
)

@Serializable
data class StationCoordinate(
    val stationName: String,
    val latitude: Double,
    val longitude: Double,
    val directLinks: List<String>
)

@Serializable
data class RouteRequest(
    val graphId: String,
    val startStation: String,
    val endStation: String
)

val graphStorage: MutableMap<String, Pair<List<TrainSchedule>, List<StationCoordinate>>> = mutableMapOf()

fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            getRoutes()
            postRoutes()
        }
    }
}

fun Route.getRoutes() {
    get("/graph") {
        //val graphId = loadGraph(call.receiveText())
    }
}

fun Route.postRoutes() {
    post("/route") {
        //val requestData = call.receive<RouteRequest>()
        //val route = calculateRoute(requestData.graphId)
    }
}

fun generateGraphId(): String {
    return UUID.randomUUID().toString()
}
suspend fun loadGraph(data: String): String = coroutineScope {

    val trainSchedules = async { readTrainSchedules("uk_train_schedule.csv") }
    val stationCoordinates = async { readStationCoordinates("uk_station_coordinates.csv") }

    val graphId = generateGraphId()
    graphStorage[graphId] = trainSchedules.await() to stationCoordinates.await()

    graphId
}

fun readTrainSchedules(file: String): List<TrainSchedule> {

    val schedule = mutableListOf<TrainSchedule>()
    val scheduleFile = File("files/$file")
    if (scheduleFile.exists()){
        val csv = scheduleFile.bufferedReader()
        val rows = csv.readLines().drop(1)
        for (r in rows){
            val (trainNumber, fromStation, toStation, departureTime, arrivalTime) = r.split(",")
            schedule.add(TrainSchedule(trainNumber, fromStation, toStation, departureTime, arrivalTime))
        }
        csv.close()
    }
    return schedule
}

fun readStationCoordinates(file: String): List<StationCoordinate> {
    val coordinates = mutableListOf<StationCoordinate>()
    val coordinatesFile = File("files/$file")
    if (coordinatesFile.exists()) {
        val csv = coordinatesFile.bufferedReader()
        val rows = csv.readLines().drop(1) // Skip header line
        for (r in rows) {
            val (stationName, latitudeStr, longitudeStr, directLinksStr) = r.split(",")
            val latitude = latitudeStr.toDouble()
            val longitude = longitudeStr.toDouble()
            val directLinks = directLinksStr.split(";")
            coordinates.add(StationCoordinate(stationName, latitude, longitude, directLinks))
        }
        csv.close()
    }
    return coordinates
}
