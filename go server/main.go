package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"math"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/gorilla/mux"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

//VERY IMPORTANT, SPECIAL CHARACTER """" ` """" FOR STRUCT DEFINITION

// Step struct
type Step struct {
	Lon       string `json:"lon"`
	Lat       string `json:"lat"`
	Timestamp string `json:"timestamp"`
}

// init array of routes
var steps []Step

// Cluster .... structure of
type Cluster [][]struct {
	Lon       string `json:"lon"`
	Lat       string `json:"lat"`
	Timestamp string `json:"timestamp"`
}

// ClusterRecord struct
type ClusterRecord struct {
	Steps []Step `json:"steps"`
}

// Center struct
type Center struct {
	Lon string `json:"lon"`
	Lat string `json:"lat"`
}

// CenterRecord struct
type CenterRecord struct {
	Center Center `json:"center"`
}

// Timestamps struct
type Timestamps [][]int

var timestamps Timestamps

// TimestampsRecord struct
type TimestampsRecord struct {
	Timestamps []int `json:"timestamps"`
}

// init array of centers
var centers []Center

// declare mongodb Client
var client *mongo.Client
var collection *mongo.Collection
var ctx context.Context

/////////////// hello folks
func homeLink(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintln(w, "Benvenuti nel Server di ClusterPass")
}

/////////////// Get All Steps ----- only for devs ------
func getAllSteps(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	result := readAllFromMongo()
	json.NewEncoder(w).Encode(result)

}

// Get Routes by Position ---only for devs---
func getRoutes(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	position := mux.Vars(r) // Get position

	lon := position["lon"]
	lat := position["lat"]

	response := getClusters(lon, lat)
	//justString := strings.Join(response, "")
	//fmt.Println(response)

	//var clusters []Cluster

	res, err := json.Marshal(response)

	if err != nil {
		fmt.Println("Errore nel marshal: ", err)
	}

	fmt.Fprintf(w, string(res))
}

// get che cluster's Centers by the user's position
func apiGetCenters(w http.ResponseWriter, r *http.Request) {

	w.Header().Set("Content-Type", "application/json")
	position := mux.Vars(r) // Get position

	lon := position["lon"]
	lat := position["lat"]

	response := getCenters()

	//fmt.Println(response)

	i := 0

	var listaCenters []Center

	for i < len(response) {
		c := response[i].Center

		if checkNearCoordinates(lon, lat, c.Lon, c.Lat) {
			listaCenters = append(listaCenters, c)
		}
		i++
	}

	res, err := json.Marshal(listaCenters)

	if err != nil {
		fmt.Println("Errore nel marshal: ", err)
	}

	fmt.Println(string(res))
	fmt.Fprintf(w, string(res))

}

// get the Cluster from the Center position send from user
func getClusterByPosition(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	position := mux.Vars(r) // Get position

	lon := position["lon"]
	lat := position["lat"]

	response := getOneCluster(lon, lat)
	//justString := strings.Join(response, "")
	//fmt.Println(response)

	//var clusters []Cluster

	res, err := json.Marshal(response)

	if err != nil {
		fmt.Println("Errore nel marshal: ", err)
	}

	fmt.Fprintf(w, string(res))

}

// get che Timestamps of the selected cluster from user
func getTimestampsByCluster(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	position := mux.Vars(r) // Get position

	lon := position["lon"]
	lat := position["lat"]

	fmt.Println(lon, lat)

	response := getTimestamps(lon, lat)

	res, err := json.Marshal(response)

	if err != nil {
		fmt.Println("Errore nel marshal: ", err)
	}

	fmt.Fprintf(w, string(res))

}

// Add new Steps
func createSteps(w http.ResponseWriter, r *http.Request) {

	fmt.Println("Received data from mobile client")

	w.Header().Set("Content-Type", "application/json")
	//var steps []Step
	_ = json.NewDecoder(r.Body).Decode(&steps)

	json.NewEncoder(w).Encode(steps)

	fmt.Println("sto per inviare il metodo newStepOnMongo")
	fmt.Println("stampo il primo step ricevuto")
	fmt.Println(steps[0])

	//metodo per creare gli step sul database mongodb
	createStepsOnMongo(steps)

}

// Add new Clusters
func createClusters(w http.ResponseWriter, r *http.Request) {

	w.Header().Set("Content-Type", "text/plain")

	bodyBytes, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("Cluster raw received from R")

	// dati grezzi
	rawBody := string(bodyBytes)

	fmt.Println(rawBody)

	// ritorna il body sistemato in json
	body := parseBody(rawBody)
	fmt.Println(body)

	// crea il cluster su mongodb
	createClustersOnMongo(body)

}

// Add new clusters's Centers
func createCenters(w http.ResponseWriter, r *http.Request) {

	w.Header().Set("Content-Type", "text/plain")

	bodyBytes, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("Centers raw received from R")
	rawBody := string(bodyBytes)

	fmt.Println(rawBody)

	body := parseBody(rawBody)
	fmt.Println(body)

	createCentersOnMongo(body)

}

// Add new clusters's Centers
func createTimestamps(w http.ResponseWriter, r *http.Request) {

	w.Header().Set("Content-Type", "text/plain")

	bodyBytes, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("Timestamps raw received from R")
	rawBody := string(bodyBytes)

	fmt.Println(rawBody)

	body := parseBody(rawBody)
	fmt.Println(body)

	createTimestampsOnMongo(body)

}

func parseBody(rawBody string) string {

	rawBody = strings.Replace(rawBody, "\"(", "{\"", -1)
	rawBody = strings.Replace(rawBody, "'", "\"", -1)
	rawBody = strings.Replace(rawBody, " ", "", -1)
	body := strings.Replace(rawBody, ")\"", "\"}", -1)

	//fmt.Println(body)
	return body
}

// a func that call a function to POST on service R every day
func doEvery(d time.Duration, f func(time.Time)) {
	for x := range time.Tick(d) {
		f(x)
	}
}

func main() {

	// init router
	router := mux.NewRouter().StrictSlash(true)

	// say hello
	router.HandleFunc("/", homeLink)

	// list of endpoints
	router.HandleFunc("/api/getCenters/{lon}/{lat}", apiGetCenters).Methods("GET")
	router.HandleFunc("/api/createSteps/", createSteps).Methods("POST")
	router.HandleFunc("/api/createClusters/", createClusters).Methods("POST")
	router.HandleFunc("/api/createCenters/", createCenters).Methods("POST")
	router.HandleFunc("/api/createTimestamps/", createTimestamps).Methods("POST")
	router.HandleFunc("/api/getClusterByPosition/{lon}/{lat}", getClusterByPosition).Methods("GET")
	router.HandleFunc("/api/getTimestampsByCluster/{lon}/{lat}", getTimestampsByCluster).Methods("GET")

	// debug, only for devs
	router.HandleFunc("/api/getAllSteps", getAllSteps).Methods("GET")
	router.HandleFunc("/api/getRoutes/{lon}/{lat}", getRoutes).Methods("GET")

	// uri of mongodb
	mongoURI := "mongodb+srv://ClusterPass:ClusterPass@cluster0-dzkyn.mongodb.net/test?retryWrites=true&w=majority"

	// connect to mongo db
	connectToMongoDB(mongoURI)

	// POST on R service
	// uses a goroutine
	go doEvery(30*time.Second, postOnService) // --------- SHOULD BE EVERY 24HOURS

	log.Fatal(http.ListenAndServe(":8000", router))
}

// connect to mongodb func
func connectToMongoDB(mongoURI string) {
	fmt.Println("Connecting to mongoDB...")

	fmt.Println(mongoURI)

	ctx = context.TODO()
	var err error
	clientOptions := options.Client().ApplyURI(mongoURI)
	client, err = mongo.Connect(ctx, clientOptions)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)
	}

	err = client.Ping(ctx, nil)
	if err != nil {
		fmt.Println(err)
		os.Exit(1)

	}

	fmt.Println("Connected to MongoDB!")

}

// add RawSteps on mongodb
func createStepsOnMongo(steps []Step) {
	collection = client.Database("Routes").Collection("RawSteps")
	fmt.Println("connesso alla collection ")
	fmt.Println(collection)

	fmt.Println(len(steps))

	i := 0

	for i < len(steps) {
		result, err := collection.InsertOne(ctx, steps[i])
		if err != nil {
			fmt.Println(err)
			fmt.Println("errorissimo")
			return
		}
		fmt.Println("inserito step")
		// ID of the inserted document.
		objectID := result.InsertedID.(primitive.ObjectID)
		fmt.Println(objectID)
		i++
	}
}

// create the clusters on mongodb
func createClustersOnMongo(body string) {

	collection = client.Database("Routes").Collection("Clusters")
	collection.Drop(ctx)

	fmt.Println("connesso alla collection clusters")

	cluster := Cluster{}

	err := json.Unmarshal([]byte(body), &cluster)
	if err != nil {
		fmt.Println("Errore unmarshal: ", err)
	}

	i := 0

	for i < len(cluster) {
		fmt.Println(cluster[i])
		result, err := collection.InsertOne(ctx, bson.M{"_id": i + 1, "steps": cluster[i]})
		if err != nil {
			fmt.Println("Errore inserimento: ", err)
		}
		fmt.Println("Response: ", result)
		i++
	}
}

// create the centers on mongodb
func createCentersOnMongo(body string) {

	//declare db name and collction name
	collection = client.Database("Routes").Collection("Centers")
	collection.Drop(ctx)

	fmt.Println("connesso alla collection Centers")

	err := json.Unmarshal([]byte(body), &centers)
	if err != nil {
		fmt.Println("Errore nell'unmarshalling del body di Centers")
	}

	i := 0

	for i < len(centers) {
		result, err := collection.InsertOne(ctx, bson.M{"_id": i + 1, "center": centers[i]})
		if err != nil {
			fmt.Println(err)
			fmt.Println("errore center")
			return
		}
		fmt.Println("inserito center")
		fmt.Println(result)
		i++
	}
}

// create the timestamps on mongodb
func createTimestampsOnMongo(body string) {

	//declare db name and collction name
	collection = client.Database("Routes").Collection("Timestamps")
	collection.Drop(ctx)

	fmt.Println("connesso alla collection Timestamps")

	err := json.Unmarshal([]byte(body), &timestamps)
	if err != nil {
		fmt.Println("Errore nell'unmarshalling del body di Timestamps")
	}

	i := 0

	for i < len(timestamps) {
		result, err := collection.InsertOne(ctx, bson.M{"_id": i + 1, "timestamps": timestamps[i]})
		if err != nil {
			fmt.Println(err)
			fmt.Println("errore timestamp")
			return
		}
		fmt.Println("inserito timestamp")
		fmt.Println(result)
		i++
	}
}

// read all database
func readAllFromMongo() []Step {

	collection = client.Database("Routes").Collection("RawSteps")
	fmt.Println("Connected to RawSteps")

	var result []Step

	c, err := collection.Find(ctx, bson.D{})
	if err != nil {
		log.Fatal(err)
	}

	for c.Next(ctx) {
		//Create a value into which the single document can be decoded
		var res Step
		err := c.Decode(&res)
		if err != nil {
			log.Fatal(err)
		}

		result = append(result, res)
	}

	fmt.Printf("Found these documents: %+v\n", result)

	return result

}

// get only one cluster by position
func getOneCluster(lon string, lat string) ClusterRecord {

	resultCenters := getCenters()
	//fmt.Println(resultCenters)

	// connect to a collection
	collection = client.Database("Routes").Collection("Clusters")
	//fmt.Println("Connected to Clusters")

	var result []ClusterRecord

	c, err := collection.Find(ctx, bson.M{})
	if err != nil {
		log.Fatal(err)
	}

	for c.Next(ctx) {
		//Create a value into which the single document can be decoded
		var res ClusterRecord

		//fmt.Println(c)
		err := c.Decode(&res)
		if err != nil {
			log.Fatal(err)
		}
		//fmt.Println(res)
		result = append(result, res)

	}

	//fmt.Printf("Found these documents: %+v\n", result)

	var percorso ClusterRecord

	i := 0
	for i < len(resultCenters) {

		if lon == resultCenters[i].Center.Lon && lat == resultCenters[i].Center.Lat {
			percorso = result[i]
		}
		i++
	}

	//fmt.Printf("Found these documents: %+v\n", listaPercorsi)

	return percorso
}

// get list of clusters near to position
func getClusters(lon string, lat string) []ClusterRecord {

	resultCenters := getCenters()
	//fmt.Println(resultCenters)

	collection = client.Database("Routes").Collection("Clusters")
	//fmt.Println("Connected to Clusters")

	var result []ClusterRecord

	c, err := collection.Find(ctx, bson.M{})
	if err != nil {
		log.Fatal(err)
	}

	for c.Next(ctx) {
		//Create a value into which the single document can be decoded
		var res ClusterRecord

		//fmt.Println(c)
		err := c.Decode(&res)
		if err != nil {
			log.Fatal(err)
		}
		//fmt.Println(res)
		result = append(result, res)

	}

	//fmt.Printf("Found these documents: %+v\n", result)

	var listaPercorsi []ClusterRecord

	i := 0
	for i < len(resultCenters) {

		if checkNearCoordinates(lon, lat, resultCenters[i].Center.Lon, resultCenters[i].Center.Lat) {
			listaPercorsi = append(listaPercorsi, result[i])
		}
		i++
	}

	//fmt.Printf("Found these documents: %+v\n", listaPercorsi)

	return listaPercorsi
}

// return the list of centers
func getCenters() []CenterRecord {
	collection = client.Database("Routes").Collection("Centers")
	//fmt.Println("Connected to Centers")

	var result []CenterRecord

	c, err := collection.Find(ctx, bson.M{})
	if err != nil {
		log.Fatal(err)
	}

	for c.Next(ctx) {
		//Create a value into which the single document can be decoded
		var res CenterRecord
		err := c.Decode(&res)
		if err != nil {
			log.Fatal(err)
		}
		result = append(result, res)

	}

	return result
}

// return the array of timestamps of a cluster
func getTimestamps(lon string, lat string) TimestampsRecord {
	resultCenters := getCenters()

	collection = client.Database("Routes").Collection("Timestamps")
	//fmt.Println("Connected to Timestamps")

	var result []TimestampsRecord

	c, err := collection.Find(ctx, bson.M{})
	if err != nil {
		fmt.Println("ERrore nella collection timestamps")
		log.Fatal(err)
	}

	for c.Next(ctx) {
		//Create a value into which the single document can be decoded
		var res TimestampsRecord
		//fmt.Println("Stampo c ")
		//fmt.Println(c)
		err := c.Decode(&res)
		if err != nil {
			fmt.Println("ERrore nella decodifica del document")
			log.Fatal(err)
		}
		//fmt.Println(res)

		result = append(result, res)

	}

	//fmt.Printf("Found these documents: %+v\n", result)

	var ts TimestampsRecord

	i := 0
	for i < len(resultCenters) {

		if lon == resultCenters[i].Center.Lon && lat == resultCenters[i].Center.Lat {
			ts = result[i]
		}
		i++
	}

	fmt.Println(ts)

	//fmt.Printf("Found these documents: %+v\n", listaPercorsi)

	return ts

}

// consumes R's POST API and get response
func postOnService(t time.Time) {

	result := readAllFromMongo()

	print(result)

	jsonValue, _ := json.Marshal(result)

	request, _ := http.NewRequest("POST", "http://localhost:6877/steps", bytes.NewBuffer(jsonValue))
	request.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	response, err := client.Do(request)
	if err != nil {
		fmt.Printf("The HTTP request failed with error %s\n", err)
	} else {
		data, _ := ioutil.ReadAll(response.Body)
		fmt.Println("R received data ")
		fmt.Print(string(data))
	}

}

// check the proximity of the user's coordinates with the coordinates of the database centers
func checkNearCoordinates(lon string, lat string, lonCluster string, latCluster string) bool {

	maxDistance := 5 * 0.009

	newlon, err := strconv.ParseFloat(lon, 64)
	newLat, err := strconv.ParseFloat(lat, 64)
	newlonCluster, err := strconv.ParseFloat(lonCluster, 64)
	newLatCluster, err := strconv.ParseFloat(latCluster, 64)

	if err != nil {
		fmt.Println("Error in converting string to float64")
	}

	a := newlon - newlonCluster
	b := newLat - newLatCluster

	c := math.Sqrt(a*a + b*b)

	// 1km = 0.009
	if c < maxDistance {
		return true
	}

	return false
}
