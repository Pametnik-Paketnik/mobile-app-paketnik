import Utility.RandomUtils;
import algorithms.GA;
import problems.TSP;

import java.util.Locale;

public class TSPParameterTuning {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        RandomUtils.setSeedFromTime();

        String[] realProblems = {
                "direct4me_distance.tsp",
                "direct4me_time.tsp"
        };

        int[] populations = {100, 300};
        double[] crossovers = {0.8, 0.9};
        double[] mutations = {0.05, 0.1, 0.2};

        System.out.println("Začenjam optimizacijo parametrov...");

        for (String fileName : realProblems) {
                System.out.println("==================================================");
                System.out.println("OBDELUJEM DATOTEKO: " + fileName);
                System.out.println("==================================================");

                TSP infoTsp = new TSP(fileName, 0);
                int d = infoTsp.getNumberOfCities();
                int maxFes = 10000 * d;

                double globalBestScore = Double.MAX_VALUE;
                String globalBestParams = "";
                TSP.Tour globalBestTour = null;

                for (int popSize : populations) {
                    for (double cr : crossovers) {
                        for (double pm : mutations) {

                            double sumScore = 0;
                            double bestInBatch = Double.MAX_VALUE;
                            int runs = 10;

                            System.out.printf("Testiram: Pop=%d, Cr=%.2f, Pm=%.2f ... ", popSize, cr, pm);

                            for (int i = 0; i < runs; i++) {
                                TSP tsp = new TSP(fileName, maxFes);
                                GA ga = new GA(popSize, cr, pm);
                                TSP.Tour result = ga.execute(tsp);

                                double score = result.getDistance();
                                sumScore += score;

                                if (score < bestInBatch) {
                                    bestInBatch = score;
                                }

                                if (score < globalBestScore) {
                                    globalBestScore = score;
                                    globalBestTour = result;
                                    globalBestParams = String.format("Pop=%d, Cr=%.2f, Pm=%.2f", popSize, cr, pm);
                                }
                            }

                            double avgScore = sumScore / runs;
                            System.out.printf("-> Best: %.2f | Avg: %.2f%n", bestInBatch, avgScore);
                        }
                    }
                }

                System.out.println("\n--------------------------------------------------");
                System.out.println("ZMAGOVALEC ZA " + fileName + ":");
                System.out.println("  Parametri: " + globalBestParams);

                String enota = fileName.contains("time") ? "sekund" : "metrov";
                System.out.printf("  Rezultat:  %.4f %s%n", globalBestScore, enota);

                System.out.println("\nNAJBOLJŠA POT (Real ID-ji):");
                if (globalBestTour != null) {
                    StringBuilder sb = new StringBuilder();
                    for (TSP.City c : globalBestTour.getPath()) {
                        sb.append(c.realId).append(" ");
                    }
                    sb.append(globalBestTour.getPath()[0].realId);

                    System.out.println(sb.toString());
                }
                System.out.println("--------------------------------------------------\n");
        }
    }
}