package problems;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import Utility.RandomUtils;

public class TSP {

    enum DistanceType {EUCLIDEAN, WEIGHTED}

    public class City {
        public int index;
        public int realId;
        public double x, y;
    }

    public static class Tour {

        double distance;
        int dimension;
        City[] path;

        public Tour(Tour tour) {
            distance = tour.distance;
            dimension = tour.dimension;
            path = tour.path.clone();
        }

        public Tour(int dimension) {
            this.dimension = dimension;
            path = new City[dimension];
            distance = Double.MAX_VALUE;
        }

        public Tour clone() {
            return new Tour(this);
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public City[] getPath() {
            return path;
        }

        public void setPath(City[] path) {
            this.path = path.clone();
        }

        public void setCity(int index, City city) {
            path[index] = city;
            distance = Double.MAX_VALUE;
        }
    }

    String name;
    City start;
    List<City> cities = new ArrayList<>();
    int numberOfCities;
    double[][] weights;
    DistanceType distanceType = DistanceType.EUCLIDEAN;
    int numberOfEvaluations, maxEvaluations;

    public TSP(String path, int maxEvaluations) {
        loadData(path);
        numberOfEvaluations = 0;
        this.maxEvaluations = maxEvaluations;
    }

    private TSP() {
    }

    public void evaluate(Tour tour) {
        double distance = 0;
        distance += calculateDistance(start, tour.getPath()[0]);
        for (int index = 0; index < numberOfCities; index++) {
            if (index + 1 < numberOfCities)
                distance += calculateDistance(tour.getPath()[index], tour.getPath()[index + 1]);
            else
                distance += calculateDistance(tour.getPath()[index], start);
        }
        tour.setDistance(distance);
        numberOfEvaluations++;
    }

    private double calculateDistance(City from, City to) {
        switch (distanceType) {
            case EUCLIDEAN:
                double dx = from.x - to.x;
                double dy = from.y - to.y;
                return Math.sqrt(dx * dx + dy * dy);

            case WEIGHTED:
                return weights[from.index - 1][to.index - 1];

            default:
                return Double.MAX_VALUE;
        }
    }

    public Tour generateTour() {
        Tour tour = new Tour(numberOfCities);
        for (int i = 0; i < numberOfCities; i++) {
            tour.setCity(i, cities.get(i));
        }
        for (int i = numberOfCities - 1; i > 0; i--) {
            int index = RandomUtils.nextInt(i + 1);
            City a = tour.getPath()[index];
            tour.getPath()[index] = tour.getPath()[i];
            tour.getPath()[i] = a;
        }
        return tour;
    }

    public TSP generateSubproblem(List<Integer> selectedIds) {
        TSP subProblem = new TSP();

        int newSize = selectedIds.size();
        subProblem.name = this.name + "_sub";
        subProblem.numberOfCities = newSize;
        subProblem.maxEvaluations = newSize * 10000;
        subProblem.distanceType = DistanceType.WEIGHTED;

        subProblem.cities = new ArrayList<>();
        subProblem.weights = new double[newSize][newSize];
        subProblem.numberOfEvaluations = 0;

        int[] originalListIndices = new int[newSize];

        for (int i = 0; i < newSize; i++) {
            int targetRealId = selectedIds.get(i);
            boolean found = false;

            for (int k = 0; k < this.cities.size(); k++) {
                City originalCity = this.cities.get(k);
                if (originalCity.realId == targetRealId) {
                    City newCity = new City();
                    newCity.index = i + 1;
                    newCity.realId = originalCity.realId;
                    newCity.x = originalCity.x;
                    newCity.y = originalCity.y;

                    subProblem.cities.add(newCity);
                    originalListIndices[i] = k;
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.err.println("OPOZORILO: Nisem našel mesta z ID: " + targetRealId);
            }
        }

        if (subProblem.cities.isEmpty()) {
            throw new RuntimeException("Napaka pri generiranju sub-problema: Nobenega mesta nisem našel! Preveri realId v loadData.");
        }

        for (int i = 0; i < newSize; i++) {
            for (int j = 0; j < newSize; j++) {
                int oldRow = originalListIndices[i];
                int oldCol = originalListIndices[j];
                subProblem.weights[i][j] = this.weights[oldRow][oldCol];
            }
        }

        if (!subProblem.cities.isEmpty()) {
            subProblem.start = subProblem.cities.get(0);
        }

        return subProblem;
    }

    private void loadData(String path) {
        InputStream inputStream = TSP.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            System.err.println("File " + path + " not found!");
            return;
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = br.readLine();
            while (line != null) {
                lines.add(line);
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        City[] tempCities = null;
        ArrayList<Double> matrixValues = new ArrayList<>();

        boolean readingCoords = false;
        boolean readingMatrix = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.equals("EOF")) continue;

            if (line.startsWith("DIMENSION")) {
                String[] parts = line.split(":");
                numberOfCities = Integer.parseInt(parts[1].trim());
                weights = new double[numberOfCities][numberOfCities];
                tempCities = new City[numberOfCities];
                cities = new ArrayList<>();
            }
            else if (line.startsWith("EDGE_WEIGHT_TYPE")) {
                String[] parts = line.split(":");
                String type = parts[1].trim();
                if (type.equals("EUC_2D")) distanceType = DistanceType.EUCLIDEAN;
                else if (type.equals("EXPLICIT")) distanceType = DistanceType.WEIGHTED;
            }
            else if (line.startsWith("NODE_COORD_SECTION")) {
                readingCoords = true;
                readingMatrix = false;
                continue;
            }
            else if (line.startsWith("DISPLAY_DATA_SECTION")) {
                readingCoords = true;
                readingMatrix = false;
                continue;
            }
            else if (line.startsWith("EDGE_WEIGHT_SECTION")) {
                readingMatrix = true;
                readingCoords = false;
                continue;
            }

            if (readingCoords) {
                String[] parts = line.split("\\s+");
                int offset = parts[0].isEmpty() ? 1 : 0;

                if (parts.length >= 3 + offset) {
                    try {
                        int id = Integer.parseInt(parts[offset]);
                        double x = Double.parseDouble(parts[offset + 1]);
                        double y = Double.parseDouble(parts[offset + 2]);
                        if (id <= numberOfCities) {
                            City c = new City();
                            c.index = id;
                            c.realId = id;
                            c.x = x;
                            c.y = y;
                            tempCities[id - 1] = c;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
            else if (readingMatrix) {
                String[] parts = line.split("\\s+");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        try {
                            matrixValues.add(Double.parseDouble(part));
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
        }

        if (!matrixValues.isEmpty()) {
            int counter = 0;
            for (int i = 0; i < numberOfCities; i++) {
                for (int j = 0; j < numberOfCities; j++) {
                    if (counter < matrixValues.size()) {
                        weights[i][j] = matrixValues.get(counter++);
                    }
                }
            }
        }

        for (int i = 0; i < numberOfCities; i++) {
            if (tempCities != null && tempCities[i] != null) {
                cities.add(tempCities[i]);
            } else {
                City dummy = new City();
                dummy.index = i + 1;
                dummy.realId = i + 1;
                dummy.x = 0;
                dummy.y = 0;
                cities.add(dummy);
            }
        }

        if (!cities.isEmpty()) {
            start = cities.get(0);
        }
    }

    public int getMaxEvaluations() {
        return maxEvaluations;
    }

    public int getNumberOfEvaluations() {
        return numberOfEvaluations;
    }

    public int getNumberOfCities() {
        return numberOfCities;
    }
}