
/**
 * An undirected, adjacency-list based graph data structure developed
 * specifically for METAL highway mapping graphs.
 * 
 * Starter implementation for the METAL Learning Module
 * Working with METAL Data
 * 
 * @author Jim Teresco ADD LAB PARTNER NAMES HERE
 * @version January 2024
 */

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HighwayGraph
{

    private static final DecimalFormat df = new DecimalFormat("#.###");

    // Small, internal data structure representing a
    // latitude-longitude pair.  It has the added benefit
    // of being able to compute its distance to another
    // LatLng object.
    private class LatLng {
        private double lat, lng;
        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        /**
        compute the distance in miles from this LatLng to another

        @param other another LatLng
        @return the distance in miles from this LatLng to other
         */
        public double distanceTo(LatLng other) {
            /** radius of the Earth in statute miles */
            final double EARTH_RADIUS = 3963.1;

            // did we get the same point?
            if (equals(other)) return 0.0;

            // coordinates in radians
            double rlat1 = Math.toRadians(lat);
            double rlng1 = Math.toRadians(lng);
            double rlat2 = Math.toRadians(other.lat);
            double rlng2 = Math.toRadians(other.lng);

            return Math.acos(Math.cos(rlat1)*Math.cos(rlng1)*Math.cos(rlat2)*Math.cos(rlng2) +
                Math.cos(rlat1)*Math.sin(rlng1)*Math.cos(rlat2)*Math.sin(rlng2) +
                Math.sin(rlat1)*Math.sin(rlat2)) * EARTH_RADIUS;
        }

        /**
        Compare another LatLng with this for equality, subject to the
        specified tolerance.

        @param o the other LatLng
        @pre o instanceof LatLng
        @return whether the two lat/lng pairs should be considered equal
         */
        public boolean equals(Object o) {
            final double TOLERANCE = 0.00001;
            LatLng other = (LatLng)o;

            return ((Math.abs(other.lat-lat) < TOLERANCE) &&
                (Math.abs(other.lng-lng) < TOLERANCE));
        }

        public String toString() {
            return "(" + lat + "," + lng + ")";
        }
    }

    // our private internal structure for a Vertex
    private class Vertex {
        private String label;
        private LatLng point;
        private Edge head;

        public Vertex(String l, double lat, double lng) {
            label = l;
            point = new LatLng(lat,lng);
        }

    }

    // our private internal structure for an Edge
    private class Edge {

        // the edge needs to know its own label, its destination vertex (note that
        // it knows its source as which vertex's list contains this edge), an
        // optional array of points that improve the edge's shape, and its length
        // in miles, which is computed on construction
        private String label;
        private int dest;
        private LatLng[] shapePoints;
        private double length;

        // and Edge is also a linked list
        private Edge next;

        public Edge(String l, int dst, LatLng startPoint, LatLng points[], LatLng endPoint, Edge n) {
            label = l;
            dest = dst;
            shapePoints = points;
            next = n;
            length = 0.0;
            LatLng prevPoint = startPoint;
            if (points != null) {
                for (int pointNum = 0; pointNum < points.length; pointNum++) {
                    length += prevPoint.distanceTo(points[pointNum]);
                    prevPoint = points[pointNum];
                }
            }
            length += prevPoint.distanceTo(endPoint);
        }
    }

    // vertices -- we know how many at the start, so these 
    // are simply in an array
    private Vertex[] vertices;

    // number of edges
    private int numEdges;

    // construct from a TMG format file that comes from the given
    // Scanner (likely over a File or URLConnection, but does not
    // matter here)
    public HighwayGraph(Scanner s) {

        // read header line -- for now assume it's OK, but should
        // check
        s.nextLine();

        // read number of vertices and edges
        int numVertices = s.nextInt();
        numEdges = s.nextInt();

        // construct our array of Vertices
        vertices = new Vertex[numVertices];

        // next numVertices lines are Vertex entries
        for (int vNum = 0; vNum < numVertices; vNum++) {
            vertices[vNum] = new Vertex(s.next(), s.nextDouble(), s.nextDouble());
        }

        // next numEdge lines are Edge entries
        for (int eNum = 0; eNum < numEdges; eNum++) {
            int v1 = s.nextInt();
            int v2 = s.nextInt();
            String label = s.next();
            // shape points take us to the end of the line, and this
            // will be just a new line char if there are none for this edge
            String shapePointText = s.nextLine().trim();
            String[] shapePointStrings = shapePointText.split(" ");
            LatLng v1Tov2[] = null;
            LatLng v2Tov1[] = null;
            if (shapePointStrings.length > 1) {
                // build arrays in both orders
                v1Tov2 = new LatLng[shapePointStrings.length/2];
                v2Tov1 = new LatLng[shapePointStrings.length/2];
                for (int pointNum = 0; pointNum < shapePointStrings.length/2; pointNum++) {
                    LatLng point = new LatLng(Double.parseDouble(shapePointStrings[pointNum*2]),
                            Double.parseDouble(shapePointStrings[pointNum*2+1]));
                    v1Tov2[pointNum] = point;
                    v2Tov1[shapePointStrings.length/2 - pointNum - 1] = point;
                }
            }

            // build our Edge structures and add to each adjacency list
            vertices[v1].head = new Edge(label, v2, vertices[v1].point, v1Tov2, vertices[v2].point, vertices[v1].head);
            vertices[v2].head = new Edge(label, v1, vertices[v2].point, v2Tov1, vertices[v1].point, vertices[v2].head);
        }
    }

    // construct and return a human-readable summary of the graph
    public String toString() {

        StringBuilder s = new StringBuilder();
        s.append("|V|=" + vertices.length + ", |E|=" + numEdges + "\n");
        for (Vertex v : vertices) {
            s.append(v.label + " " + v.point + "\n");
            Edge e = v.head;
            while (e != null) {
                Vertex o = vertices[e.dest];
                s.append("  to " + o.label + " " + o.point + " on " + e.label);
                if (e.shapePoints != null) {
                    s.append(" via");
                    for (int pointNum = 0; pointNum < e.shapePoints.length; pointNum++) {
                        s.append(" " + e.shapePoints[pointNum]);
                    }
                }
                s.append(" length " + df.format(e.length) + "\n");
                e = e.next;
            }
        }

        return s.toString();
    }

    // try it out
    public static void main(String args[]) throws IOException {

        if (args.length != 1) {
            System.err.println("Usage: java HighwayGraph tmgfile");
            System.exit(1);
        }

        // read in the file to construct the graph
        Scanner s = new Scanner(new File(args[0]));
        HighwayGraph g = new HighwayGraph(s);
        s.close();

        // print summary of the graph
        System.out.println(g);

        // ADD CODE HERE TO COMPLETE LAB TASKS


        // vertex search function
        int north = 0;
        int south = 0;
        int east = 0;
        int west = 0;
        int longest = 0;
        int shortest = 0;

        // scan vertices
        for (int i = 1; i < g.vertices.length; i++) {
            Vertex v = g.vertices[i];

            // compare
            if (v.point.lat > g.vertices[north].point.lat) {
                north = i;
            }
            if (v.point.lat < g.vertices[south].point.lat) {
                south = i;
            }
            if (v.point.lng > g.vertices[east].point.lng) {
                east = i;
            }
            if (v.point.lng < g.vertices[west].point.lng) {
                west = i;
            }

            int len = v.label.length();
            if (len > g.vertices[longest].label.length()) {
                longest = i;
            }
            if (len < g.vertices[shortest].label.length()) {
                shortest = i;
            }
        }

        // print

        System.out.println("Vertex Search Function: ");
        System.out.println("Northernmost: " + g.vertices[north].label);
        System.out.println("Southernmost: " + g.vertices[south].label);
        System.out.println("Easternmost: " + g.vertices[east].label);
        System.out.println("Westernmost: " + g.vertices[west].label);
        System.out.println("Longest label: " + g.vertices[longest].label);
        System.out.println("Shortest label: " + g.vertices[shortest].label);



        // basic edge search

        int longestLabel = 0;
        int shortestLabel = 0;
        int longestLength = 0;
        int shortestLength = 0;

        Edge longestLabelEdge   = null;
        Edge shortestLabelEdge  = null;
        Edge longestLengthEdge  = null;
        Edge shortestLengthEdge = null;

        int vertexIndex = 0;

        int edgeCount = 0;

        double totalEdgeLength = 0.0;

        for (Vertex v : g.vertices) {
            for (Edge e = v.head; e != null; e = e.next) {
                if (vertexIndex < e.dest) {
        
                    edgeCount++;

                    totalEdgeLength += e.length;
        
                    // initialize
                    if (longestLabelEdge == null) {
                        longestLabelEdge   = e;
                        shortestLabelEdge  = e;
                        longestLengthEdge  = e;
                        shortestLengthEdge = e;
                    }
        
                    // compare
                    if (e.label.length() > longestLabelEdge.label.length()) {
                        longestLabelEdge = e;
                    }
                    if (e.label.length() < shortestLabelEdge.label.length()) {
                        shortestLabelEdge = e;
                    }
                    if (e.length > longestLengthEdge.length) {
                        longestLengthEdge = e;
                    }
                    if (e.length < shortestLengthEdge.length) {
                        shortestLengthEdge = e;
                    }
                }
            }
        
            vertexIndex++;
        }

        // print
        System.out.println();
        System.out.println();
        System.out.println("Basic Edge Search Function: ");
        System.out.println("Edge with longest label: " + longestLabelEdge.label);
        System.out.println("Edge with shortest label: " + shortestLabelEdge.label);
        System.out.println("Longest edge: " + longestLengthEdge.label);
        System.out.println("Shortest edge: " + shortestLengthEdge.label);

        System.out.println();
        System.out.println("Edges examined: " + edgeCount);
        System.out.println("Total edges: " + g.numEdges);

        System.out.println();
        System.out.println("Total graph length: " + totalEdgeLength + " miles");
        System.out.println();




    }
}
