import Utility.RandomUtils;
import algorithms.GA;
import problems.TSP;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class TSPTest {

    private static final String TEAM_NAME = "AirBox";

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);

        String[] problemFiles = {
                "bays29.tsp",
                "eil101.tsp",
                "a280.tsp",
                "pr1002.tsp",
                "dca1389.tsp"
        };

        String resultsPath = "tsp-algorithm/results";

        File resultsDir = new File(resultsPath);
        if (!resultsDir.exists()) {
            boolean created = resultsDir.mkdirs();
            if (created) {
                System.out.println("Ustvaril mapo: " + resultsPath);
            } else {
                resultsPath = "results";
                resultsDir = new File(resultsPath);
                if (!resultsDir.exists()) {
                    resultsDir.mkdir();
                }
                System.out.println("Uporabljam mapo: " + resultsPath);
            }
        }

        try {
            RandomUtils.setSeedFromTime();
            System.out.println("Seed: " + RandomUtils.getSeed());

            for (String fileName : problemFiles) {
                System.out.println("--------------------------------------------------");
                System.out.println("Processing: " + fileName);

                TSP infoTsp = new TSP(fileName, 0);
                int d = infoTsp.getNumberOfCities();
                int maxFes = 1000 * d;

                System.out.println("  Cities (d): " + d);
                System.out.println("  MaxFes: " + maxFes);

                String cleanName = fileName.replace(".tsp", "");

                String outputFileName = resultsPath + "/" + TEAM_NAME + "_" + cleanName + ".txt";

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {

                    for (int i = 0; i < 30; i++) {
                        TSP tsp = new TSP(fileName, maxFes);
                        GA ga = new GA(100, 0.8, 0.1);

                        TSP.Tour best = ga.execute(tsp);

                        double score = best.getDistance();

                        writer.write(String.format(Locale.US, "%.20f", score));
                        writer.newLine();

                        System.out.printf("    Run %2d: %.4f%n", (i + 1), score);
                    }

                    System.out.println("  -> Saved to: " + outputFileName);

                } catch (IOException e) {
                    System.err.println("Error writing file: " + outputFileName);
                    e.printStackTrace();
                }
            }
            System.out.println("DONE!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}