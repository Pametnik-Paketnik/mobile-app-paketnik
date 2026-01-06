import Utility.RandomUtils;
import algorithms.GA;
import problems.TSP;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TSPTestDirect4me {

    private static final String TEAM_NAME = "AirBox";

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        String[] realProblems = {
                "direct4me_distance.tsp",
                "direct4me_time.tsp"
        };

        String resultsPath = "tsp-algorithm/results";
        new File(resultsPath).mkdirs();

        try {
            RandomUtils.setSeedFromTime();

            for (String fileName : realProblems) {
                System.out.println("--------------------------------------------------");
                System.out.println("Rešujem: " + fileName);

                TSP problemInfo = new TSP(fileName, 0);
                int d = problemInfo.getNumberOfCities();
                
                int maxFes = 10000 * d; 
                
                List<Double> allScores = new ArrayList<>();
                
                TSP.Tour bestGlobalTour = null;
                double minDistance = Double.MAX_VALUE;

                for (int i = 0; i < 10; i++) {
                    TSP tsp = new TSP(fileName, maxFes);
                    GA ga = new GA(100, 0.8, 0.1); 
                    
                    TSP.Tour best = ga.execute(tsp);
                    double score = best.getDistance();
                    allScores.add(score);

                    if (score < minDistance) {
                        minDistance = score;
                        bestGlobalTour = best;
                    }
                    
                    String enota = fileName.contains("time") ? "s" : "m";
                    System.out.printf("    Run %2d: %.2f %s%n", (i+1), score, enota);
                }

                String cleanName = fileName.replace(".tsp", "");
                String statsFile = resultsPath + "/" + TEAM_NAME + "_" + cleanName + "_STATS.txt";
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(statsFile))) {
                    for (Double val : allScores) {
                        writer.write(String.format(Locale.US, "%.20f", val));
                        writer.newLine();
                    }
                }
                System.out.println("  -> Statistika: " + statsFile);

                if (bestGlobalTour != null) {
                    String solFile = resultsPath + "/" + TEAM_NAME + "_" + cleanName + "_SOLUTION.txt";
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(solFile))) {
                        writer.write("NAME: " + cleanName);
                        writer.newLine();
                        writer.write("SCORE: " + minDistance);
                        writer.newLine();
                        writer.write("TOUR_SECTION");
                        writer.newLine();
                        
                        StringBuilder sb = new StringBuilder();
                        for (TSP.City city : bestGlobalTour.getPath()) {
                            sb.append(city.index).append("\n"); 
                        }
                        writer.write(sb.toString());
                        writer.write("-1");
                        writer.newLine();
                        writer.write("EOF");
                    }
                    System.out.println("  -> Rešitev (Pot): " + solFile);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}