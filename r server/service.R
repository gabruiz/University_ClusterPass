library(tidyverse)  # data manipulation
library(cluster)    # clustering algorithms
library(factoextra) # clustering algorithms & visualization
library (plumber)
library(jsonlite)
library(fpc)
library(NbClust)
library(rlist)
library(httr)
library(mclust)
library(rsconnect)
library(lubridate)

clusterize<-function(rawData){
  #print(rawData)
  data <- fromJSON(rawData)
  
  print(data)
  
  
  # --------- SHOULD BE 500---------  #
  MINSTEPPERSESSION <- 5
  

  output <- matrix(as.numeric(unlist(data$lon)), ncol = 1, byrow = TRUE)
  output <- cbind(output, as.numeric(unlist(data$lat)))
  colnames(output) <- c("lon","lat")
  
  print(output)
 
  #uniqueOutput <- unique(output)
  
  
  output_timestamp <- matrix(as.numeric(unlist(data$lon)), ncol = 1, byrow = TRUE)
  output_timestamp <- cbind(output, as.numeric(unlist(data$lat)))
  output_timestamp <- cbind(output, as.numeric(unlist(data$timestamp)))

  
  #print(output)
  maxnc <- nrow(output)
  #maxunique <- unique(output)
  #print("Massimo numero di clusters")
  #print(maxnc)

  maxClusters <- as.integer(maxnc/MINSTEPPERSESSION)+1
  print("maxClusters")
  print(maxClusters)
  
  #Ward : Ward method minimizes the total within-cluster variance.
  res <- NbClust(data = output, diss = NULL, distance = "euclidean",
                 min.nc = 2, max.nc = maxClusters , method = "ward.D2") 


  bc <- length(unique(res$Best.partition))
  
  print("Optimal number of clusters: ")
  print(bc)
  
  
  
  #k<-kmeans(output, centers = bc)
  #plot(fviz_cluster(k, data = output))
  
  # Ward Hierarchical Clustering
  d <- dist(output, method = "euclidean") # distance matrix
  k <- hclust(d, method="ward")
  
  #plot(k) # display dendogram
  
  groups <- cutree(k, k=bc) # cut tree into n clusters
  
  # draw dendogram with red borders around the n clusters
  
  rect.hclust(k, k=bc, border="red")
  



  i <- 1
  j <- 1
  
  clusterList<-array(list(), bc)
  
  listaOre <- matrix(0, nrow = bc, ncol=24)

  clusterListValueslon<-array(list(), bc)
  clusterListValuesLat<-array(list(), bc)
  
  for (i in 1:maxnc){
    index <- groups[i]
  
    
    
    clusterListValueslon[[index]]<-c(clusterListValueslon[[index]], output[i,1])
    clusterListValuesLat[[index]]<-c(clusterListValuesLat[[index]], output[i,2])
    
    passo <- paste ("(lon':'", output[i,1], "', 'lat' : '", output[i,2], "', 'timestamp' : '", output_timestamp[i,3], ")")
  
    dt <- as_datetime(output_timestamp[i,3]/1000)
    ora <- hour(dt)+1
    if (ora == 24){
      ora <- 0
    }
    
    listaOre[index,ora] <- listaOre[index,ora]+1
    
    
    clusterList[[index]]<-c(clusterList[[index]], passo)
  }

  print("Lunghezza di clusterList")
  print(length(clusterList))

  z<-1
  
  centersList<-array(bc)
  
  
  
  for (z in 1:length(clusterList)){
    
    center <- paste ("(lon':'", mean(clusterListValueslon[[z]]), "', 'lat' : '", mean(clusterListValuesLat[[z]]), ")")
    
    centersList[z]<-center
  }
  

  

  
  
  
  
  


  
  
  
  
  
  
  
  
  
  
  
  jsonCenters <- toJSON (centersList)

  jsonClusters <- toJSON(clusterList)
  
  jsonTimestamp <- toJSON(listaOre)

  
  postClusters(jsonClusters)
  postCenters(jsonCenters)
  postTimestamp(jsonTimestamp)

}

#* Return the clusterized data
#* @post /steps
function(req){
  
  print("Connected")
  clusterize(req$postBody)
  

}

postClusters<-function(js){
  
  POST(url = "http://localhost:8000/api/createClusters/", body = js, encode = "json")
  
}


postCenters<-function(js){
  
  POST(url = "http://localhost:8000/api/createCenters/", body = js, encode = "json")
  
}

postTimestamp<-function(js){
  
  POST(url = "http://localhost:8000/api/createTimestamps/", body = js, encode = "json")
  
}