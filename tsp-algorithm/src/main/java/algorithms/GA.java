package algorithms;

import Utility.RandomUtils;
import problems.TSP;

import java.util.ArrayList;

public class GA {

    int popSize;
    double cr; //crossover probability
    double pm; //mutation probability

    ArrayList<TSP.Tour> population;
    ArrayList<TSP.Tour> offspring;

    public GA(int popSize, double cr, double pm) {
        this.popSize = popSize;
        this.cr = cr;
        this.pm = pm;
    }

    public TSP.Tour execute(TSP problem) {
        population = new ArrayList<>();
        offspring = new ArrayList<>();
        TSP.Tour best = null;

        for (int i = 0; i < popSize; i++) {
            TSP.Tour newTour = problem.generateTour();
            problem.evaluate(newTour);
            population.add(newTour);

            if (best == null || newTour.getDistance() < best.getDistance()) {
                best = newTour.clone();
            }
        }

        while (problem.getNumberOfEvaluations() < problem.getMaxEvaluations()) {

            //elitizem - poišči najboljšega in ga dodaj v novo
            // (Clone je nujen, da ne spreminjamo reference!)
            TSP.Tour elite = getBestInPopulation();
            offspring.add(elite.clone());

            // Polnimo populacijo do velikosti popSize
            while (offspring.size() < popSize) {
                TSP.Tour parent1 = tournamentSelection();
                TSP.Tour parent2 = tournamentSelection();
                //TODO preveri, da starša nista enaka (po referenci ali vsebini)
                // Če sta enaka, poskusimo izbrati drugega (samo 1x poskus, da ne ciklamo v nedogled)
                if (parent1 == parent2) {
                    parent2 = tournamentSelection();
                }

                // KRIŽANJE (Crossover)
                if (RandomUtils.nextDouble() < cr) {
                    TSP.Tour[] children = pmx(parent1, parent2);
                    offspring.add(children[0]);
                    if (offspring.size() < popSize)
                        offspring.add(children[1]);
                } else {
                    offspring.add(parent1.clone());
                    if (offspring.size() < popSize)
                        offspring.add(parent2.clone());
                }
            }

            // MUTACIJA
            // Gremo čez vse potomce (razen prvega, ki je elite!)
            // Začnemo z i=1, ker i=0 je elitni posameznik, ki ga ne smemo pokvariti
            for (int i = 1; i < offspring.size(); i++) {
                if (RandomUtils.nextDouble() < pm) {
                    swapMutation(offspring.get(i));
                }
            }

            for (TSP.Tour tour : offspring) {
                problem.evaluate(tour);
                assert best != null;
                if (tour.getDistance() < best.getDistance()) {
                    best = tour.clone();
                }
            }

            population = new ArrayList<>(offspring);
            offspring.clear();
        }
        return best;
    }

    // Pomožna metoda za iskanje najboljšega v trenutni populaciji (za elitizem)
    private TSP.Tour getBestInPopulation() {
        TSP.Tour bestLocal = population.get(0);
        for (TSP.Tour t : population) {
            if (t.getDistance() < bestLocal.getDistance()) {
                bestLocal = t;
            }
        }
        return bestLocal;
    }

    private void swapMutation(TSP.Tour tour) {
        // Izberemo dva naključna indeksa v poti
        int dimension = tour.getPath().length;
        int i = RandomUtils.nextInt(dimension);
        int j = RandomUtils.nextInt(dimension);

        // Zamenjamo mesti
        TSP.City temp = tour.getPath()[i];
        tour.setCity(i, tour.getPath()[j]);
        tour.setCity(j, temp);
    }

    private TSP.Tour[] pmx(TSP.Tour parent1, TSP.Tour parent2) {
        //izvedi pmx križanje, da ustvariš dva potomca
        return null;
    }

    private TSP.Tour tournamentSelection() {
        TSP.Tour best = null;

        int tournamentSize = 3;
        for (int i = 0; i < tournamentSize; i++) {
            int randomIdx = RandomUtils.nextInt(population.size());
            TSP.Tour candidate = population.get(randomIdx);

            if (best == null || candidate.getDistance() < best.getDistance()) {
                best = candidate;
            }
        }
        return best;
    }
}