import java.util.*;

/**
 * Beginnings of a library for graph analysis code
 * 
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2017 (with some inspiration from previous terms)
 * 
 */
public class GraphLib {

	/**
	 * Orders vertices in decreasing order by their in-degree
	 *
	 * @param g graph
	 * @return list of vertices sorted by in-degree, decreasing (i.e., largest at index 0)
	 */
	public static <V, E> List<V> verticesByInDegree(Graph<V, E> g) {
		List<V> vs = new ArrayList<>();
		for (V v : g.vertices()) vs.add(v);
		vs.sort((v1, v2) -> g.inDegree(v2) - g.inDegree(v1));
		return vs;
	}

	/**
	 * Takes a random walk from a vertex, to a random one if its out-neighbors, to a random one of its out-neighbors
	 * Keeps going as along as a random number is less than "continueProb"
	 * Stops earlier if no step can be taken (i.e., reach a vertex with no out-edge)
	 *
	 * @param g      graph to walk on
	 * @param start  initial vertex (assumed to be in graph)
	 * @param keepOn probability of continuing each time -- should be between 0 and 1 (non-inclusive)
	 * @return a list of vertices starting with start, each with an edge to the sequentially next in the list
	 * null if start isn't in graph
	 */
	public static <V, E> List<V> randomWalk(Graph<V, E> g, V start, double keepOn) {
		if (!g.hasVertex(start) || keepOn <= 0 || keepOn >= 1) return null;
		List<V> path = new ArrayList<>();
		path.add(start);
		V curr = start;
		while (Math.random() < keepOn) {
			if (g.outDegree(curr) == 0) return path;
			// Pick a neighbor index
			int nbr = (int) (g.outDegree(curr) * Math.random());
			// Iterate through the out-neighbors the given number of times
			Iterator<V> iter = g.outNeighbors(curr).iterator();
			V next = iter.next();
			while (nbr > 0) {
				next = iter.next();
				nbr--;
			}
			// Got to the right neighbor; continue from there
			path.add(next);
			curr = next;
		}

		return path;
	}

	/**
	 * Takes a number of random walks from random vertices, keeping track of how many times it goes to each vertex
	 * Doesn't actually keep the walks themselves
	 *
	 * @param g        graph to walk on
	 * @param keepOn   probability of continuing each time -- should be between 0 and 1 (non-inclusive)
	 * @param numWalks how many times to do that
	 * @return vertex-hitting frequencies
	 */
	public static <V, E> Map<V, Integer> randomWalks(Graph<V, E> g, double keepOn, int numWalks) {
		if (keepOn <= 0 || keepOn >= 1) return null;

		// Initialize all frequencies to 0
		Map<V, Integer> freqs = new HashMap<>();
		for (V v : g.vertices()) freqs.put(v, 0);

		for (int i = 0; i < numWalks; i++) {
			// Pick a start index
			int start = (int) (g.numVertices() * Math.random());
			// Iterate through vertices till get there
			Iterator<V> iter = g.vertices().iterator();
			V curr = iter.next();
			while (start > 0) {
				curr = iter.next();
				start--;
			}
			while (Math.random() < keepOn && g.outDegree(curr) > 0) {
				// Pick a neighbor index
				int nbr = (int) (g.outDegree(curr) * Math.random());
				// Iterate through the out-neighbors the given number of times
				iter = g.outNeighbors(curr).iterator();
				V next = iter.next();
				while (nbr > 0) {
					next = iter.next();
					nbr--;
				}
				// Keep frequency count
				freqs.put(next, 1 + freqs.get(next));
				curr = next;
			}
		}

		return freqs;
	}


	/**
	 * Orders vertices in decreasing order by their frequency in the map
	 *
	 * @param g graph
	 * @return list of vertices sorted by frequency, decreasing (i.e., largest at index 0)
	 */
	public static <V, E> List<V> verticesByFrequency(Graph<V, E> g, Map<V, Integer> freqs) {
		List<V> vs = new ArrayList<>();
		for (V v : g.vertices()) vs.add(v);
		vs.sort((v1, v2) -> freqs.get(v2) - freqs.get(v1));
		return vs;
	}

	/**
	 * Uses BFS to create a path tree containing the shortest paths between vertices
	 * @param g      	the original undirected graph
	 * @param source 	the new center of the universe
	 * @return 			returns the directed path tree
	 */
	public static <V, E> Graph<V, E> bfs(Graph<V, E> g, V source) {
		Graph<V, E> pathTree = new AdjacencyMapGraph<>();
		// placed in case someone forcefully uses a source not in the original graph
		if (!g.hasVertex(source)) {
			System.out.println(source+" not found.");
			return pathTree;
		}

		//load start vertex with null parent
		pathTree.insertVertex(source);

		Set<V> visited = new HashSet<>(); 		   //Set to track which vertices have already been visited
		Queue<V> queue = new LinkedList<>(); 	   //queue to implement BFS

		queue.add(source); 			 		//enqueue start vertex
		visited.add(source);		 	 	//add start to visited Set
		while (!queue.isEmpty()) {	        //loop until no more vertices
			V u = queue.remove(); 			//dequeue
			for (V v : g.outNeighbors(u)) { //loop over out neighbors
				if (!visited.contains(v)) { //if neighbor not visited, then neighbor is discovered from this vertex
					visited.add(v); 		//add neighbor to visited Set
					queue.add(v); 			//enqueue neighbor

					//save that this vertex was discovered from prior vertex
					pathTree.insertVertex(v);
					pathTree.insertDirected(v, u, g.getLabel(v, u));
				}
			}
		}
		return pathTree;
	}

	/**
	 * Get the shortest path from the given vertex v to source
	 * @param tree		the directed BFS path tree
	 * @param v			the vertex at which the path ends
	 * @return			the path from source to end vertex v
	 */
	public static <V,E> List<V> getPath(Graph<V,E> tree, V v) {
		//make sure end vertex in pathTree
		if (!tree.hasVertex(v)) {
			System.out.println("The separation between center and "+v+" is infinite.");
			return new ArrayList<>();
		}

		//start from end vertex and work backward to source
		ArrayList<V> path = new ArrayList<>(); //this will hold the path from start to end vertex
		V current = v; 						   //start at end vertex

		//loop from end vertex back to source
		while (tree.outDegree(current)!=0) {	// runs until the current vertex has no out neighbor (start vertex)
			path.add(current); //add this vertex to front of arraylist path
			for (V parent: tree.outNeighbors(current)) {
				current = parent; //get vertex that discovered this vertex
			}
		}
		path.add(current);	// adding the root to the path
		return path;
	}

	/**
	 *
	 * @param graph			the original undirected graph
	 * @param subgraph		the bfs graph/path tree
	 * @return				the set of vertices that cannot be reached by BFS from source
	 */
	public static <V,E> Set<V> missingVertices(Graph<V,E> graph, Graph<V,E> subgraph) {
		Set<V> nobodies = new HashSet<>();	// set of vertices not in subgraph

		for (V v: graph.vertices()) {	// grab a vertex v from graph
			// if vertex v not found in subgraph, add them to nobodies set
			if (!subgraph.hasVertex(v)) nobodies.add(v);
		}
		return nobodies;
	}

	/**
	 * Calculates the average
	 * @param tree		the bfs path tree
	 * @param root		the root/center of the universe
	 * @return			returns the average distance from root
	 */
	public static <V,E> double averageSeparation(Graph<V,E> tree, V root) {
		int sum = sumOfPaths(tree, root, 0);			// getting the total sum
		return ((double) sum)/(double)(tree.numVertices()-1);	// avg = sum/num of connected actors
	}

	/**
	 * Helper functions for averageSeparation() which allows us to recurse through the path tree
	 * Gets the total sum of edges in all the possible paths from root
	 * @param tree			the bfs path tree
	 * @param vertex		the current vertex, initially the root of the path tree
	 * @param pathSoFar		to keep track of the number of edges for each path
	 * @return				the sum of all edges in each path from root
	 */
	public static <V, E> int sumOfPaths(Graph<V,E> tree, V vertex, int pathSoFar) {
		int sum = 0;
		// for every child in path tree, add their path from root to the sum
		for (V neighbor: tree.inNeighbors(vertex)) sum += sumOfPaths(tree, neighbor, pathSoFar+1);
		sum+=pathSoFar;
		return sum;
	}
}