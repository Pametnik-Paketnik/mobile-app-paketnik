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

            while (offspring.size() < popSize) {
                TSP.Tour parent1 = tournamentSelection();
                TSP.Tour parent2 = tournamentSelection();
                //TODO preveri, da starša nista enaka (po referenci ali vsebini)
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
        int dimension = tour.getPath().length;
        int i = RandomUtils.nextInt(dimension);
        int j = RandomUtils.nextInt(dimension);

        TSP.City temp = tour.getPath()[i];
        tour.setCity(i, tour.getPath()[j]);
        tour.setCity(j, temp);
    }

    private TSP.Tour[] pmx(TSP.Tour parent1, TSP.Tour parent2) {
        int size = parent1.getPath().length;
        TSP.Tour child1 = new TSP.Tour(size);
        TSP.Tour child2 = new TSP.Tour(size);

        int cut1 = RandomUtils.nextInt(size);
        int cut2 = RandomUtils.nextInt(size);

        if (cut1 > cut2) {
            int temp = cut1;
            cut1 = cut2;
            cut2 = temp;
        }

        for (int i = cut1; i <= cut2; i++) {
            child1.setCity(i, parent1.getPath()[i]);
            child2.setCity(i, parent2.getPath()[i]);
        }

        fillRest(child1, parent2, cut1, cut2, parent1);
        fillRest(child2, parent1, cut1, cut2, parent2);

        return new TSP.Tour[]{child1, child2};
    }

    private void fillRest(TSP.Tour child, TSP.Tour sourceParent, int cut1, int cut2, TSP.Tour mappingParent) {
        int size = child.getPath().length;

        for (int i = 0; i < size; i++) {
            if (i >= cut1 && i <= cut2) continue;

            TSP.City candidate = sourceParent.getPath()[i];

            while (contains(child, candidate, cut1, cut2)) {
                int indexInMappingParent = findIndex(mappingParent, candidate);
                candidate = sourceParent.getPath()[indexInMappingParent];
            }

            child.setCity(i, candidate);
        }
    }

    private boolean contains(TSP.Tour tour, TSP.City city, int start, int end) {
        for (int i = start; i <= end; i++) {
            if (tour.getPath()[i].index == city.index) {
                return true;
            }
        }
        return false;
    }

    private int findIndex(TSP.Tour tour, TSP.City city) {
        for (int i = 0; i < tour.getPath().length; i++) {
            if (tour.getPath()[i].index == city.index) {
                return i;
            }
        }
        return -1;
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