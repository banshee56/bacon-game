import java.io.*;
import java.util.*;

/**
 * The Bacon Game shows the connections between actors and Kevin Bacon, the default center of the acting universe
 * @author Bansharee Ireen
 */
public class BaconGame {
    Graph<String, String> pathTree;
    static boolean gameOn = true;

    BaconGame() {
        pathTree = new AdjacencyMapGraph<>();
    }

    /**
     * Creates a map from actor ID to actor name and movie ID to movie name
     * @param fileName          name of the file we create the map from
     * @return                  returns the map
     * @throws IOException      exception resulting from FileReader
     */
    private static Map<String, String> fileToMap(String fileName) throws IOException {
        Map<String, String> map = new HashMap<>();
        BufferedReader in = new BufferedReader(new FileReader(fileName));   // reads the file

        String line;
        while ((line = in.readLine()) != null) {          // reading each line
            String[] toSplit = line.split("\\|");   // separating by |
            String id = toSplit[0];     // the first part always contains an ID
            String name = toSplit[1];   // the second part always contains a movie/actor name
            map.put(id, name);
        }

        in.close();
        return map;
    }

    /**
     * Creates map of movie ID to IDs of actors who played in it
     * @param moviesActorsFile      the file with movie IDs and corresponding actor IDs
     * @return                      returns the map
     * @throws IOException          exception resulting from FileReader
     */
    private static Map<String, Set<String>> idMap(String moviesActorsFile) throws IOException {
        Map<String, Set<String>> map = new HashMap<>();
        BufferedReader in = new BufferedReader(new FileReader(moviesActorsFile));   // reads the file

        String line;
        while ((line = in.readLine()) != null) {
            String[] toSplit = line.split("\\|");
            String movieID = toSplit[0];    // the first part is always the movie ID
            String actorID = toSplit[1];    // the second part is an actor ID

            // if we've added this movie ID before for a previous actor
            if (map.containsKey(movieID)) map.get(movieID).add(actorID); // add new actor to the set of actors IDs

            // if this is the first time adding the movie ID
            else {
                Set<String> actorsIDSet = new HashSet<>();  // create a new set of actor IDs
                actorsIDSet.add(actorID);
                map.put(movieID, actorsIDSet);  // add the actor
            }
        }

        in.close();
        return map;
    }

    /**
     * Creates the final map from movie name to actor names
     * @param actorsFile     the file with actor IDs and names
     * @param moviesFile     the file with movie IDs and names
     * @param idFile         the file with movie IDs and actor IDs
     * @return               returns the final map needed for our graph
     * @throws IOException   exception resulting from FileReader in fileToMap() and idMap()
     */
    public static Map<String, Set<String>> movieToActors(String actorsFile, String moviesFile,
                                                         String idFile) throws IOException {
        // getting 3 maps using our files
        Map<String, String> id2Actor = fileToMap(actorsFile);
        Map<String, String> id2Movie = fileToMap(moviesFile);

        Map<String, Set<String>> mID2AID = idMap(idFile);   // this maps movie to the actors in it

        // to create map that maps movies to the actors in them
        Map<String, Set<String>> m2A = new HashMap<>();

        for (String movieID: id2Movie.keySet()) {        // going through each movie ID
            String movie = id2Movie.get(movieID);        // grab the corresponding movie name
            Set<String> actorIDs = mID2AID.get(movieID); // grab the actor IDs for the movie

            Set<String> actorNames = new HashSet<>();

            if (actorIDs!=null) {              // some movies do not have actors listed so set may be null
                for (String actorID : actorIDs) {
                    actorNames.add(id2Actor.get(actorID));
                }
            }
            m2A.put(movie, actorNames); // all movie IDs appear only once in mID2AID, so no repeats of movie names
        }
        return m2A;
    }

    /**
     * Creates the original graph with actor vertices connected by their movies as edges
     * @param map               map of movie id to a set of actor IDs of actors in the movie
     * @param actorsFile        file containing actor IDs and names
     * @return                  returns the final graph
     * @throws IOException      rises due to FileReader in fileToMap()
     */
    public static Graph<String, Set<String>> createGraph(Map<String, Set<String>> map,
                                                         String actorsFile) throws IOException {
        Graph<String, Set<String>> baconGraph = new AdjacencyMapGraph<>();

        Map<String, String> actors = fileToMap(actorsFile); // getting a map of actor IDs to names

        for (String actor: actors.values()) baconGraph.insertVertex(actor); // inserting actor vertices

        for (String movie: map.keySet()) {
            Set<String> actorSet = map.get(movie);  // grabs a movie to turn it into an edge

            // grab 2 actors from the set
            for (String actor1: actorSet) {
                for (String actor2: actorSet) {
                    // if they are different actors
                    if (!actor1.equals(actor2)) {
                        // if the graph does not have an edge between them
                        if (!baconGraph.hasEdge(actor1, actor2)) {
                            Set<String> movieSet = new HashSet<>(); // create a new set of movies
                            movieSet.add(movie);                    // add the common movie
                            baconGraph.insertUndirected(actor1, actor2, movieSet);  // add movie edge between actors
                        }
                        // else add movie to set of movies in the edge
                        else baconGraph.getLabel(actor1, actor2).add(movie);
                    }
                }
            }
        }
        return baconGraph;
    }

    public static void main(String[] args) throws IOException {
        String actorsInput = "inputs/actors.txt";
        String moviesInput = "inputs/movies.txt";
        String idInput = "inputs/movie-actors.txt";
        String source = "Kevin Bacon";

        System.out.println("Commands:");
        System.out.println("c <#>: list top (positive number) or bottom (negative) <#> centers of the universe, sorted by average separation");
        System.out.println("d <#>: list top (positive number) or bottom (negative) <#> actors sorted by degree");
        System.out.println("i: list actors with infinite separation from the current center");
        System.out.println("p <name>: find path from <name> to current center of the universe");
        System.out.println("s: show the average separation between current center and all actors connected to them");
        System.out.println("u <name>: make <name> the center of the universe");
        System.out.println("q: quit game");

        // creating final map from which we create the original graph, then creating the bfs path tree
        Map<String, Set<String>> finalMap = movieToActors(actorsInput, moviesInput, idInput);
        Graph<String, Set<String>> ogGraph = createGraph(finalMap, actorsInput);
        Graph<String, Set<String>> bfsTree = GraphLib.bfs(ogGraph, source); // bfs path tree

        // connects to 1 less than the num of vertices in BFS tree so that the actor doesn't connect
        // to themselves
        System.out.println(source+" is now the center of the acting universe, connected to "+
                (bfsTree.numVertices()-1)+"/"+ogGraph.numVertices()+" actors.");

        // for the console input
        Scanner in = new Scanner(System.in);
        while (gameOn) {
            System.out.println();
            System.out.println("Kevin Bacon game >");
            String line = in.nextLine();
            String[] terms = line.split(" ");

            String mode = terms[0]; // identifying the mode from the first character of input

            // if invalid key is pressed, will print out a statement and not run the rest
            if (!(mode.equals("p") || mode.equals("i") || mode.equals("u") || mode.equals("q") || mode.equals("s") || mode.equals("c") || mode.equals("d"))) {
                System.out.println("Invalid feature. Please check commands.");
                continue;
            }

            // if mode is p or u, we need a name from the input
            String name = "";
            if (mode.equals("p") || mode.equals("u")) {
                // building the name from the input so that it comes after the character for mode
                for (String n : terms) {
                    if (n.equals(mode)) continue;
                    name = name + n + " ";
                }
                name = name.trim();
            }

            // if mode is c or d, we need 2 values from the input
            int num=0;
            if (mode.equals("c") || mode.equals("d")) {
                if (terms.length!=2 || Integer.parseInt(terms[1]) == 0) {
                    System.out.println("Invalid numeric input. Please check commands.");
                    continue;
                }
                num = Integer.parseInt(terms[1]);
            }

            if (mode.equals("p")) { // find shortest path
                // does not run if invalid name is input
                if (!ogGraph.hasVertex(name)) System.out.println("Name not found. Please try again.");
                else if (name.equals(source)) System.out.println("They're the same person!");
                else {
                    bfsTree = GraphLib.bfs(ogGraph, source);
                    ArrayList<String> path = (ArrayList<String>) GraphLib.getPath(bfsTree, name);   // get path
                    int ind = path.size()-1;
                    if (ind>0) System.out.println(name+"'s number is "+ ind);
                    for (String costar: path) {
                        if (costar.equals(name)) continue;
                        System.out.println(name+" appeared in "+bfsTree.getLabel(name, costar)+" with "+costar);
                        name = costar;
                    }
                }
            }

            if (mode.equals("i")) { // list nobodies
                System.out.println("The actors with infinite separation from "+source+":");
                Set<String> infSeparation = GraphLib.missingVertices(ogGraph, bfsTree); // gets set of nobodies
                for (String isolated : infSeparation) System.out.println(isolated); // prints them all out
            }

            if (mode.equals("u")) { // change center
                source = name;  // updates the name input
                // does not run if invalid name is input
                if (!ogGraph.hasVertex(name)) {
                    System.out.println("Name not found. Please try again.");
                }
                else {
                    bfsTree = GraphLib.bfs(ogGraph, source);    // updates the bfsTree with new center
                    System.out.println(source+" is now the center of the acting universe, connected to "+
                            (bfsTree.numVertices()-1)+"/"+ogGraph.numVertices()+" actors.");
                }
            }

            if (mode.equals("s")) { // show avg separation
                double separation = GraphLib.averageSeparation(bfsTree, source);
                System.out.println("Average separation with center "+source+": "+separation);
            }

            if (mode.equals("c")) { // find good/bad Bacon through avg separation
                ArrayList<Double> avgSepList = new ArrayList<>();   // list of average separation values

                // map from separation to set of actors with the average separation value
                Map<Double, Set<String>> sep2Actor = new HashMap<>();

                for (String actor: bfsTree.vertices()) {    // going through every actor in the path tree
                    bfsTree = GraphLib.bfs(ogGraph, actor); // considering each actor as the center
                    double avgPathLength = GraphLib.averageSeparation(bfsTree, actor);  // finding avg separation
                    avgSepList.add(avgPathLength);  // add the separation value to the list

                    // if first time seeing the separation value, add it to the map
                    if (!sep2Actor.containsKey(avgPathLength)) {
                        Set<String> actors = new HashSet<>();
                        actors.add(actor);  // add actor to a new set of actors
                        sep2Actor.put(avgPathLength, actors);
                    }
                    else sep2Actor.get(avgPathLength).add(actor);   // if value already there, add actor to the set
                }

                avgSepList.sort(Comparator.comparingDouble(s -> s));    // sort separation values list

                int i = 0;  // variable to keep track of whether we've printed enough names
                int j;      // variable to keep track of index number for list

                // if num is positive, we print the best Bacons
                if (num>0) {
                    j = 0;  // start printing from beginning of list
                    System.out.println("Best possible Bacons (with smallest average separation):");
                }
                // else we print the worst Bacons
                else {
                    j = avgSepList.size()-1;    // start printing from end of list
                    System.out.println("Worst possible Bacons (with largest average separation):");
                }

                // while i does not meet num requirement
                while (i < Math.min(avgSepList.size(), Math.abs(num))) {
                    double sepValue = avgSepList.get(j);    // get smallest/largest value from sorted list
                    Set<String> actors = sep2Actor.get(sepValue);   // get set of actors

                    for (String actor: actors) {
                        // in the format: <name>    <avg separation value>
                        System.out.println(actor + "  " + sepValue);
                        i++;    // increment i
                        // in case we print enough while printing from a single set
                        if (i >= Math.min(avgSepList.size(), Math.abs(num))) break;
                    }
                    if (num>0) j=i;
                    else if (num<0) j-=i;
                }
            }

            if (mode.equals("d")) { // find good/bad Bacon through degree
                List<String> degreeList = GraphLib.verticesByInDegree(ogGraph); // getting the list from method
                int size = degreeList.size();

                if (num>0) {
                    System.out.println("Best possible Bacons (with largest degree):");
                    // printing out the best Bacons until we hit the required number of actors or we hit the end of the list
                    for (int ind = 0; ind < Math.min(num, size); ind++) {
                        // in the format: <name>     <degree>
                        System.out.println(degreeList.get(ind) + "    " + ogGraph.inDegree(degreeList.get(ind)));
                    }
                }
                else {
                    // printing out the worst Bacons until we hit the required number of actors or we hit the end of the list
                    System.out.println("Worst possible Bacons (with smallest degree):");
                    for (int ind = size - 1; ind >= Math.max(0, size - Math.abs(num)); ind--) {
                        System.out.println(degreeList.get(ind) + "    " + ogGraph.inDegree(degreeList.get(ind)));
                    }
                }
            }

            if (mode.equals("q")) { // ends the game
                System.out.println("\nGame over. Thank you for playing!");
                gameOn = false;
            }
        }
    }
}
