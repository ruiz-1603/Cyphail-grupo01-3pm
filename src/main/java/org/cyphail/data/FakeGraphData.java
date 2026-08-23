package org.cyphail.data;

import java.util.*;

public class FakeGraphData {
    private static final Map<String, GraphInfo> GRAPHS = new LinkedHashMap<>();

    static {
        GRAPHS.put("amigos", new GraphInfo(
            "Social Network",
            new String[][] {
                {"nombre", "edad"},
                {"Ana", "28"},
                {"Luis", "31"},
                {"Carlos", "25"},
                {"Beatriz", "34"},
                {"David", "29"},
                {"Elena", "22"}
            },
            new String[][] {
                {"desde", "relacion", "hacia"},
                {"Ana", "AMIGO_DE", "Luis"},
                {"Ana", "AMIGO_DE", "Beatriz"},
                {"Luis", "AMIGO_DE", "Carlos"},
                {"Luis", "AMIGO_DE", "David"},
                {"Carlos", "AMIGO_DE", "Elena"},
                {"Beatriz", "AMIGO_DE", "Elena"}
            }
        ));

        GRAPHS.put("tasks", new GraphInfo(
            "Tasks and resources",
            new String[][] {
                {"nombre", "estado"},
                {"Task1", "pending"},
                {"Task2", "completed"},
                {"Task3", "in_progress"}
            },
            new String[][] {
                {"tarea", "tipo", "recurso"},
                {"Task1", "REQUIRES", "Resource1"},
                {"Task2", "REQUIRES", "Resource2"}
            }
        ));

        GRAPHS.put("teams", new GraphInfo(
            "Soccer Teams",
            new String[][] {
                {"nombre", "posicion"},
                {"Saprissa", "Cartago"},
                {"Alajuelense", "Alajuela"},
                {"Deportivo", "San Jose"}
            },
            new String[][] {
                {"equipo1", "juegan", "equipo2"},
                {"Saprissa", "RIVAL", "Alajuelense"},
                {"Alajuelense", "RIVAL", "Deportivo"}
            }
        ));

        GRAPHS.put("planets", new GraphInfo(
            "Planets in Solar System",
            new String[][] {
                {"nombre", "tipo"},
                {"Mercury", "Rocky"},
                {"Venus", "Rocky"},
                {"Earth", "Rocky"},
                {"Mars", "Rocky"},
                {"Jupiter", "Gas Giant"}
            },
            new String[][] {
                {"planeta1", "orbita", "planeta2"},
                {"Mercury", "ORBITA", "Sun"},
                {"Venus", "ORBITA", "Sun"}
            }
        ));
    }

    public static Map<String, GraphInfo> getGraphs() {
        return new LinkedHashMap<>(GRAPHS);
    }

    public static GraphInfo getGraph(String name) {
        return GRAPHS.get(name);
    }

    public static boolean graphExists(String name) {
        return GRAPHS.containsKey(name);
    }

    public static class GraphInfo {
        public String description;
        public String[][] nodes;
        public String[][] edges;

        public GraphInfo(String description, String[][] nodes, String[][] edges) {
            this.description = description;
            this.nodes = nodes;
            this.edges = edges;
        }
    }
}