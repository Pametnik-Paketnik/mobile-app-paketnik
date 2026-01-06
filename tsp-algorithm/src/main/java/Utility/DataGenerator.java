package Utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataGenerator {

    private static final String API_KEY = loadApiKeyFromEnv();

    private static final String[] RAW_ADDRESSES = {
            "Goriška cesta 24, 5270 Ajdovščina", "Čolnikov trg 9, 2234 Benedikt", "Ljubljanska cesta 10, 4260 Bled",
            "Trg svobode 2 C, 4264 Bohinjska Bistrica", "Molkov trg 12, 1353 Borovnica", "Obrobna ulica 1, 2354 Bresternica",
            "Podpeška cesta 20, 1351 Brezovica pri Ljubljani", "Ulica stare pravde 34, 8250 Brežice", "Krekov trg 9, 3101 Celje",
            "Ulica Prekmurske čete 14, 9232 Črenšovci", "Center 16, 2393 Črna na Koroškem", "Kolodvorska cesta 30 A, 8340 Črnomelj",
            "Krtina 144, 1233 Dob", "Ulica bratov Gerjovičev 52, 8257 Dobova", "Trg 4. julija 1, 2370 Dravograd",
            "Gorišnica 79, 2272 Gorišnica", "Cesta na Stadion 1, 9250 Gornja Radgona", "Podsmrečje 20, 3342 Gornji Grad",
            "Attemsov trg 8, 3342 Gornji Grad", "Partizanska cesta 7, 1290 Grosuplje", "Vodnikova ulica 1, 5280 Idrija",
            "Ploščad Osvobodilne fronte 4, 1295 Ivančna Gorica", "Ivanjkovci 9 B, 2259 Ivanjkovci", "Cankarjev drevored 1, 6310 Izola",
            "Cesta Cirila Tavčarja 8, 4271 Jesenice", "Jesenice 9, 8261 Jesenice na Dolenjskem", "Cesta v Rošpoh 18, 2351 Kamnica",
            "Ljubljanska cesta 14 A, 1241 Kamnik", "Kopališka ulica 2, 2325 Kidričevo", "Kobilje 30, 9227 Kobilje",
            "Ljubljanska cesta 23, 1330 Kočevje", "Glavarjeva cesta 39, 1218 Komenda", "Kolodvorska cesta 9, 6104 Koper",
            "Dražgoška ulica 8, 4101 Kranj", "Škofjeloška cesta 17, 4103 Kranj", "Jezerska cesta 41, 4104 Kranj",
            "Ulica Lojzeta Hrovata 2, 4105 Kranj", "Borovška cesta 92, 4280 Kranjska Gora", "Trg Matije Gubca 1, 8271 Krško",
            "Partizanska cesta 3, 2230 Lenart v Slovenskih goricah", "Trg ljudske pravice 7, 9220 Lendava", "Alpska cesta 37 B, 4248 Lesce",
            "Poljanski nasip 30, 1104 Ljubljana", "Pražakova ulica 3, 1106 Ljubljana", "Zaloška cesta 57, 1110 Ljubljana",
            "Dunajska cesta 141, 1113 Ljubljana", "Riharjeva ulica 38, 1115 Ljubljana", "Vodnikova cesta 235, 1117 Ljubljana",
            "Resljeva cesta 14, 1124 Ljubljana", "Letališka cesta 12, 1122 Ljubljana", "Tacenska cesta 94, 1133 Ljubljana",
            "Hrvaška ulica 8, 1122 Ljubljana", "Ob železnici 22, 1110 Ljubljana", "Hranilniška ulica 1, 1116 Ljubljana",
            "Dunajska cesta 145, 1113 Ljubljana", "Litijska cesta 140, 1119 Ljubljana", "Tehnološki park 22a, 1125 Ljubljana",
            "Tržaška cesta 89, 1111 Ljubljana", "Cesta dveh cesarjev 73, 1123 Ljubljana", "Cesta dveh cesarjev 71, 1123 Ljubljana",
            "Dunajska cesta 361, 1231 Ljubljana - Črnuče", "Zadobrovška cesta 14, 1260 Ljubljana - Polje", "Prušnikova ulica 2, 1210 Ljubljana - Šentvid",
            "Makole 37, 2321 Makole", "Malečnik 56, 2229 Malečnik", "Dominkuševa ulica 4, 2116 Maribor",
            "Istrska ulica 49, 2104 Maribor", "Gosposvetska cesta 83, 2101 Maribor", "Gosposvetska cesta 84, 2101 Maribor",
            "Pohorska ulica 21a, 2115 Maribor", "Prvomajska ulica 35, 2105 Maribor", "Radvanjska cesta 63, 2103 Maribor",
            "Razlagova ulica 3, 2116 Maribor", "Šarhova ulica 53, 2106 Maribor", "Šarhova ulica 59 A, 2106 Maribor",
            "Tyrševa ulica 23, 2101 Maribor", "Ulica heroja Šlandra 15, 2116 Maribor", "Trdinov trg 8 A, 1234 Mengeš",
            "Naselje Borisa Kidriča 2, 8330 Metlika", "Cesta na Fužine 3, 8233 Mirna", "Vegova ulica 1, 1251 Moravče",
            "Savinjska cesta 3, 3330 Mozirje", "Trg Zmage 6, 9101 Murska Sobota", "Ulica Štefana Kovača 43, 9101 Murska Sobota",
            "Glavni trg 31, 2366 Muta", "Glavna cesta 24, 4202 Naklo", "Kidričeva ulica 19, 5101 Nova Gorica",
            "Industrijska cesta 9, 5102 Nova Gorica", "Ulica Slavka Gruma 7, 8102 Novo mesto", "Novi trg 7, 8105 Novo mesto",
            "Poštna ulica 2, 2270 Ormož", "Gornji Petrovci 40 E, 9203 Petrovci", "Malteška cesta 38, 3313 Polzela",
            "Ulica 1. maja 2 A, 6230 Postojna", "Šiška 1, 4205 Preddvor", "Trg 32 A, 2391 Prevalje",
            "Mariborska Cesta 19, 2251 Ptuj", "Ljubljanska cesta 14, 2327 Rače", "Panonska cesta 5, 9252 Radenci",
            "Trg svobode 19, 2390 Ravne na Koroškem", "Kolodvorska ulica 2, 1310 Ribnica", "Slovenski trg 4, 2352 Selnica ob Dravi",
            "Trg svobode 9, 8290 Sevnica", "Partizanska cesta 48 A, 6210 Sežana", "Kidričeva ulica 3a, 2380 Slovenj Gradec",
            "Cesta k Dravi 5, 2241 Spodnji Duplek", "Vrtojbenska cesta 19 C, 5290 Šempeter pri Gorici", "Gasilska cesta 2 A, 4208 Šenčur",
            "Prvomajska cesta 3, 8310 Šentjernej", "Mestni trg 5a, 3230 Šentjur", "Kapucinski trg 14, 4221 Škofja Loka",
            "Aškerčev trg 26, 3240 Šmarje pri Jelšah", "Trg maršala Tita 10, 5220 Tolmin", "Trg revolucije 27, 1420 Trbovlje",
            "Trg Franca Fakina 4, 1422 Trbovlje", "Goliev trg 11, 8210 Trebnje", "Predilniška cesta 10, 4290 Tržič",
            "Kidričeva cesta 2a, 3320 Velenje", "Poštna ulica 2, 1360 Vrhnika", "Tržaška cesta 32, 1360 Vrhnika",
            "Cesta zmage 28, 1410 Zagorje ob Savi", "Mariborska ulica 26, 2314 Zgornja Polskava", "Ulica heroja Staneta 1, 3310 Žalec",
            "Na Kresu 1, 4228 Železniki", "Trg svobode 1, 4226 Žiri", "Žirovnica 4, 4274 Žirovnica"
    };

    static class Location {
        int id;
        String address;
        double lat, lon;
        public Location(int id, String address) { this.id = id; this.address = address; }
    }

    private static final HttpClient client = HttpClient.newHttpClient();

    private static String loadApiKeyFromEnv() {
        String[] possiblePaths = {
            ".env",
            "../.env",
            "../../.env",
            "tsp-algorithm/.env",
            System.getProperty("user.dir") + "/.env",
            System.getProperty("user.dir") + "/../.env"
        };

        for (String path : possiblePaths) {
            File envFile = new File(path);
            if (envFile.exists() && envFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        if (line.startsWith("GOOGLE_API_KEY=")) {
                            String key = line.substring("GOOGLE_API_KEY=".length()).trim();
                            if (!key.isEmpty()) {
                                System.out.println("Loaded GOOGLE_API_KEY from: " + envFile.getAbsolutePath());
                                return key;
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading .env file from " + path + ": " + e.getMessage());
                }
            }
        }

        System.err.println("GOOGLE_API_KEY not found in .env file. Please create a .env file with GOOGLE_API_KEY=your_key");
        return "TU_PRILEPI_SVOJ_GOOGLE_API_KEY";
    }

    public static void main(String[] args) {
        System.out.println("ZACENJAM GENERIRANJE PODATKOV Z GOOGLE API...");

        if (API_KEY.contains("TU_PRILEPI") || API_KEY.length() < 20) {
            System.err.println("NAPAKA: API kljuc izgleda neveljaven ali prazen! Preveri .env datoteko.");
            System.err.println("Prebran kljuc: '" + API_KEY + "'");
            return;
        }

        try {
            List<Location> locations = new ArrayList<>();
            int idCounter = 1;

            System.out.println("\n--- 1. Geocoding (Google Maps) ---");
            for (String addr : RAW_ADDRESSES) {
                String fullAddr = addr + ", Slovenia";
                String encodedAddr = URLEncoder.encode(fullAddr, StandardCharsets.UTF_8);
                String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + encodedAddr + "&key=" + API_KEY;

                String json = sendRequest(url);
                
                Double lat = extractValue(json, "\"lat\"\\s*:\\s*([0-9.]+)");
                Double lng = extractValue(json, "\"lng\"\\s*:\\s*([0-9.]+)");

                if (lat != null && lng != null) {
                    Location loc = new Location(idCounter++, addr);
                    loc.lat = lat;
                    loc.lon = lng;
                    locations.add(loc);
                    System.out.println(addr + " -> " + lat + ", " + lng);
                } else {
                    System.err.println("ERROR za: " + addr);
                    System.err.println("ODGOVOR GOOGLA: " + json);
                    
                    if (json.contains("REQUEST_DENIED") || json.contains("OVER_QUERY_LIMIT")) {
                        System.err.println("USTAVLJAM PROGRAM ZARADI API NAPAKE.");
                        return;
                    }
                }
                Thread.sleep(100); 
            }
            
            if (locations.isEmpty()) {
                System.err.println("Nismo dobili nobenih lokacij. Prekinitve.");
                return;
            }

            System.out.println("\n--- 2. Distance Matrix (Vsi do vseh) ---");
            int size = locations.size();
            long[][] distanceMatrix = new long[size][size]; 
            long[][] timeMatrix = new long[size][size];     

            for (int i = 0; i < size; i++) {
                Location origin = locations.get(i);
                System.out.printf("Procesiram izvor %d/%d: %s...%n", (i+1), size, origin.address);

                for (int j = 0; j < size; j += 25) {
                    StringBuilder destCoords = new StringBuilder();
                    int end = Math.min(j + 25, size);
                    
                    for (int k = j; k < end; k++) {
                        if (k > j) destCoords.append("%7C");
                        destCoords.append(locations.get(k).lat).append(",").append(locations.get(k).lon);
                    }

                    String url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                            "?origins=" + origin.lat + "," + origin.lon +
                            "&destinations=" + destCoords.toString() +
                            "&key=" + API_KEY;

                    String json = sendRequest(url);
                    
                    if (json.contains("error_message")) {
                         System.err.println("MATRIX ERROR: " + json);
                    }

                    parseGoogleMatrixResponse(json, distanceMatrix, timeMatrix, i, j, end);
                    Thread.sleep(100); 
                }
            }

            System.out.println("\n--- 3. Shranjevanje ---");
            saveFullMatrixFile("tsp-algorithm/src/main/resources/direct4me_distance.tsp", "direct4me_distance", locations, distanceMatrix, false);
            saveFullMatrixFile("tsp-algorithm/src/main/resources/direct4me_time.tsp", "direct4me_time", locations, timeMatrix, true);

            System.out.println("KONCANO! Datoteke so pripravljene.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String sendRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static Double extractValue(String json, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return null;
    }

    private static void parseGoogleMatrixResponse(String json, long[][] distMat, long[][] timeMat, int rowIdx, int colStart, int colEnd) {
        Pattern distPattern = Pattern.compile("\"distance\"\\s*:\\s*\\{[^}]*\"value\"\\s*:\\s*(\\d+)");
        Pattern durPattern = Pattern.compile("\"duration\"\\s*:\\s*\\{[^}]*\"value\"\\s*:\\s*(\\d+)");
        
        Matcher mDist = distPattern.matcher(json);
        Matcher mDur = durPattern.matcher(json);
        
        int targetCol = colStart;
        
        while (mDist.find() && mDur.find() && targetCol < colEnd) {
            long distVal = Long.parseLong(mDist.group(1));
            long durVal = Long.parseLong(mDur.group(1));
            
            distMat[rowIdx][targetCol] = distVal;
            timeMat[rowIdx][targetCol] = durVal;
            
            targetCol++;
        }
    }

    private static void saveFullMatrixFile(String filename, String name, List<Location> locations, long[][] matrix, boolean isTime) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("NAME: " + name + "\n");
            writer.write("TYPE: TSP\n");
            writer.write("DIMENSION: " + locations.size() + "\n");
            writer.write("EDGE_WEIGHT_TYPE: EXPLICIT\n");
            writer.write("EDGE_WEIGHT_FORMAT: FULL_MATRIX\n");
            writer.write("DISPLAY_DATA_TYPE: TWOD_DISPLAY\n");
            
            writer.write("EDGE_WEIGHT_SECTION\n");
            for (long[] row : matrix) {
                for (long val : row) {
                    writer.write(" " + val);
                }
                writer.write("\n");
            }
            
            writer.write("DISPLAY_DATA_SECTION\n");
            for (Location loc : locations) {
                writer.write(String.format(Locale.US, " %d %.6f %.6f\n", loc.id, loc.lat, loc.lon));
            }
            writer.write("EOF\n");
        }
        System.out.println("Shranjeno: " + filename);
    }
}