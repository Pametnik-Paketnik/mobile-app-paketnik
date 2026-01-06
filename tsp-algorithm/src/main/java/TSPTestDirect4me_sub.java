import Utility.RandomUtils;
import algorithms.GA;
import problems.TSP;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TSPTestDirect4me_sub {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        RandomUtils.setSeedFromTime();

        try {
            TSP fullProblem = new TSP("direct4me_distance.tsp", 0);
            
            List<Integer> userSelection = Arrays.asList(1, 3, 9, 33, 43, 16, 21, 2, 19, 77);
            System.out.println("Izbrani ID-ji: " + userSelection);

            TSP subProblem = fullProblem.generateSubproblem(userSelection);
            GA ga = new GA(100, 0.8, 0.1); 
            TSP.Tour bestPath = ga.execute(subProblem);

            System.out.println("--------------------------------");
            System.out.println("OPTIMALNA POT (Realni ID-ji za Google Maps):");
            
            for (TSP.City city : bestPath.getPath()) {
                System.out.print(city.realId + " -> ");
            }
            System.out.println(bestPath.getPath()[0].realId);
            
            System.out.println("--------------------------------");
            System.out.println("Dolžina: " + bestPath.getDistance() + " m");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}