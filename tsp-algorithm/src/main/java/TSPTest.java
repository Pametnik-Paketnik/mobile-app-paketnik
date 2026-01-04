import Utility.RandomUtils;
import algorithms.GA;
import problems.TSP;

public class TSPTest {

    public static void main(String[] args) {

        try {
            RandomUtils.setSeedFromTime(); // nastavi novo seme ob vsakem zagonu main metode (vsak zagon bo drugačen)
            System.currentTimeMillis();
            // primer zagona za problem eil101.tsp
            for (int i = 0; i < 30; i++) {
                TSP eilTsp = new TSP("eil101.tsp", 10000);
                GA ga = new GA(100, 0.8, 0.1);
                TSP.Tour bestPath = ga.execute(eilTsp);
            }
            //shrani v datoteko
            System.out.println(RandomUtils.getSeed()); // izpiše seme s katerim lahko ponovimo zagon
        } catch (Exception e) {
            System.err.println("Prislo je do napake med izvajanjem!");
            e.printStackTrace();
        }
    }
}
