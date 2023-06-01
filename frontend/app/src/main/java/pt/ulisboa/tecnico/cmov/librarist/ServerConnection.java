package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.userId;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.currentLocation;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.MAX_DIST_KM_CACHE;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public class ServerConnection {
    // Server and port
    public static final String endpoint = "http://192.92.147.54:5000";
    //public static final String wsEndpoint = "ws://cmov2-docentes-tp-1.vps.tecnico.ulisboa.pt:5000/ws";
    //WebSocketClient webSocketClient = null;

    // Method called when creating a new Library -- WORKING!
    public void createLibrary(String name, LatLng latLng, String address, byte[] photo) throws IOException {
        String url = endpoint + "/libraries/create";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", name);
        jsonObject.addProperty("latitude", latLng.latitude);
        jsonObject.addProperty("longitude", latLng.longitude);
        jsonObject.addProperty("address", address);
        jsonObject.addProperty("photo", Base64.getEncoder().encodeToString(photo));

        String jsonString = jsonObject.toString();

        DataOutputStream outputStream = new DataOutputStream((OutputStream) connection.getOutputStream());
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        if (connection.getResponseCode() == 200) {
            // TODO CREATE VERIFICATIONS IN BACKEND AND SEND THEM (FOR EXAMPLE DUPLICATE LIBRARIES)
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Received values
            int libraryId = responseJson.get("libId").getAsInt();

            Library newLibrary = new Library(libraryId, name, latLng, address, photo, new ArrayList<>(), false);
            libraryCache.addLibrary(newLibrary);
            Log.d("LIBRARY", "ADDED LIBRARY TO FRONTEND");

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public List<Library> getLibrariesWithBook(Book book) throws IOException {
        String url = endpoint + "/books/" + book.getId() + "/libraries?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonArray responseJsonArray = getJsonArrayFromResponse(connection.getInputStream());

            List<Library> libraries = new ArrayList<>();
            for (JsonElement element : responseJsonArray){
                // Get book object
                JsonObject libraryObject = element.getAsJsonObject();
                // Get book properties
                int libId = libraryObject.get("libId").getAsInt();
                String name = libraryObject.get("name").getAsString();
                double latitude = libraryObject.get("latitude").getAsDouble();
                double longitude = libraryObject.get("longitude").getAsDouble();
                String address = libraryObject.get("address").getAsString();
                byte[] photo = Base64.getDecoder().decode(libraryObject.get("photo").getAsString());
                boolean favorite = libraryObject.get("isFavorite").getAsBoolean();

                // Process bookIds
                JsonArray bookIdsArray = libraryObject.getAsJsonArray("bookIds");
                List<Integer> bookIds = new ArrayList<>();
                for (JsonElement bookIdElement : bookIdsArray) {
                    bookIds.add(bookIdElement.getAsInt());
                }

                libraries.add(new Library(libId, name, new LatLng(latitude, longitude),
                        address, photo, bookIds, favorite));
            }

            Log.d("LIST ALL", libraries.toString());
            return libraries;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public List<Library> loadLibraries(int maxDistance) throws IOException {
        String url = endpoint + "/libraries?distance=" + maxDistance + "&userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Parse libraries
            JsonArray librariesJsonArray = responseJson.getAsJsonArray("libraries");
            List<Library> libraries = new ArrayList<>();
            List<Integer> booksToCache = new ArrayList<>();

            for (JsonElement element : librariesJsonArray){
                // Get book object
                JsonObject libraryObject = element.getAsJsonObject();
                // Get book properties
                int libId = libraryObject.get("libId").getAsInt();
                String name = libraryObject.get("name").getAsString();
                double latitude = libraryObject.get("latitude").getAsDouble();
                double longitude = libraryObject.get("longitude").getAsDouble();
                String address = libraryObject.get("address").getAsString();
                byte[] photo = Base64.getDecoder().decode(libraryObject.get("photo").getAsString());
                boolean favorite = libraryObject.get("isFavorite").getAsBoolean();

                // Process bookIds
                JsonArray bookIdsArray = libraryObject.getAsJsonArray("bookIds");
                List<Integer> bookIds = new ArrayList<>();
                for (JsonElement bookIdElement : bookIdsArray) {
                    bookIds.add(bookIdElement.getAsInt());
                }

                Library library = new Library(libId, name, new LatLng(latitude, longitude),
                        address, photo, bookIds, favorite);
                libraries.add(library);

                // If in a 10km radius, add to the cache
                float[] result = new float[10];
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                        latitude, longitude, result);
                // Convert result from meters to km
                float lib_distance = result[0] / 1000;

                //
                if (lib_distance <= MAX_DIST_KM_CACHE){
                    libraryCache.addLibrary(library);
                    booksToCache.addAll(bookIds);
                }
            }

            // Parse books in the libraries
            JsonArray booksJsonArray = responseJson.getAsJsonArray("books");

            for (JsonElement element : booksJsonArray){
                // Get book object
                JsonObject bookObject = element.getAsJsonObject();
                // Get book properties
                int bookId = bookObject.get("bookId").getAsInt();
                // If book is from a library that is not in cache
                if (booksToCache.contains(bookId)){
                    String title = bookObject.get("title").getAsString();
                    byte[] cover = Base64.getDecoder().decode(bookObject.get("cover").getAsString());
                    String barcode = bookObject.get("barcode").getAsString();
                    boolean activNotif = bookObject.get("activNotif").getAsBoolean();

                    booksCache.addBook(new Book(bookId, title,cover, barcode, activNotif));
                }
            }

            return libraries;
        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public List<Book> getAllBooks() throws IOException {
        String url = endpoint + "/books?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonArray responseJsonArray = getJsonArrayFromResponse(connection.getInputStream());

            List<Book> allBooks = new ArrayList<>();
            for (JsonElement element : responseJsonArray){
                // Get book object
                JsonObject bookObject = element.getAsJsonObject();
                // Get book properties
                int bookId = bookObject.get("bookId").getAsInt();
                String title = bookObject.get("title").getAsString();
                byte[] cover = Base64.getDecoder().decode(bookObject.get("cover").getAsString());
                String barcode = bookObject.get("barcode").getAsString();
                boolean activNotif = bookObject.get("activNotif").getAsBoolean();

                Book book = new Book(bookId, title,cover, barcode, activNotif);
                allBooks.add(book);
            }

            Log.d("LIST ALL", allBooks.toString());

            return allBooks;
        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public int findBook(String barcode) throws IOException {
        String url = endpoint + "/books/find?barcode=" + barcode;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonParser jsonParser = new JsonParser();
            JsonObject responseJson = jsonParser.parse(new InputStreamReader(connection.getInputStream()))
                    .getAsJsonObject();

            return responseJson.get("bookId").getAsInt();
        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public Book getBook(int bookId) throws IOException {
        String url = endpoint + "/books/get?bookId=" + bookId + "&userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Get book properties
            assert responseJson != null;
            int _bookId = responseJson.get("bookId").getAsInt();
            if(_bookId != bookId){
                Log.d("GET BOOK", "DIFFERENT BOOK IDS");
            }
            String title = responseJson.get("title").getAsString();
            byte[] cover = Base64.getDecoder().decode(responseJson.get("cover").getAsString());
            String barcode = responseJson.get("barcode").getAsString();
            boolean activNotif = responseJson.get("activNotif").getAsBoolean();

            Book bookResponse = new Book(bookId, title,cover, barcode, activNotif);
            booksCache.addBook(bookResponse);

            return bookResponse;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    // Method called when checking in a new book -- WORKING!
    public void checkInBook(String barcode, int libraryID) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + "/libraries/" + libraryID + "/books/checkin").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("barcode", barcode);
        jsonObject.addProperty("libId", libraryID);

        String jsonString = jsonObject.toString();

        Log.d("CHECKIN", "CRIEI JSON");

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        Log.d("CHECKIN", "ENVIEI");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Received values
            assert responseJson != null;
            // just to confirm that the barcodes are the same
            String _barcode = responseJson.get("barcode").getAsString();
            if (!_barcode.equals(barcode)){
                Log.d("CHECK IN", "DIFFERENT BARCODES");
            }
            int bookId = responseJson.get("bookId").getAsInt();
            String title = responseJson.get("title").getAsString();
            byte[] cover = Base64.getDecoder().decode(responseJson.get("cover").getAsString());
            boolean activNotif = responseJson.get("activNotif").getAsBoolean();

            Log.d("CHECKIN", "RECEBI");
            Log.d("CHECKIN", String.valueOf(bookId));

            Book newBook = new Book(bookId, title, cover, barcode, activNotif);
            libraryCache.getLibrary(libraryID).addBook(bookId);
            booksCache.addBook(newBook);
            Log.d("CHECKIN", "ADDED BOOK TO CACHE");

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }



    // Method called when checking in a new book -- WORKING!
    public void checkInNewBook(String title, byte[] cover, String barcode, int libraryID) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + "/libraries/" + libraryID + "/books/checkin/newbook").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("title", title);
        if (cover != null){
            jsonObject.addProperty("cover", Base64.getEncoder().encodeToString(cover));
        } else {
            jsonObject.addProperty("cover", "");
        }
        jsonObject.addProperty("barcode", barcode);
        jsonObject.addProperty("libId", libraryID);

        String jsonString = jsonObject.toString();

        Log.d("CHECKIN", "CRIEI JSON");

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        Log.d("CHECKIN", "ENVIEI");

        if (connection.getResponseCode() == 200) {
            JsonParser jsonParser = new JsonParser();
            JsonObject responseJson =  jsonParser.parse(new InputStreamReader(connection.getInputStream()))
                    .getAsJsonObject();;

            // Received values
            assert responseJson != null;
            int bookId = responseJson.get("bookId").getAsInt();

            Log.d("CHECKIN", "RECEBI");
            Log.d("CHECKIN", String.valueOf(bookId));

            Book newBook = new Book(bookId, title, cover, barcode, false);
            libraryCache.getLibrary(libraryID).addBook(bookId);
            booksCache.addBook(newBook);
            Log.d("BOOK", "ADDED BOOK TO CACHE");

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public int findBookInLibrary(String barcode, int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/books/find?barcode=" + barcode;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonParser jsonParser = new JsonParser();
            JsonObject responseJson = jsonParser.parse(new InputStreamReader(connection.getInputStream()))
                    .getAsJsonObject();

            return responseJson.get("bookId").getAsInt();

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

    }

    // Method called when checking out a book -- WORKING!
    public void checkOutBook(String barcode, int libraryID) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + "/libraries/" + libraryID + "/books/checkout").openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("barcode", barcode);
        jsonObject.addProperty("libId", libraryID);

        String jsonString = jsonObject.toString();

        Log.d("CHECKOUT", "CRIEI JSON");

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        Log.d("CHECKOUT", "ENVIEI");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Received values
            assert responseJson != null;
            int bookId = responseJson.get("bookId").getAsInt();

            Log.d("CHECKIN", "RECEBI");
            Log.d("CHECKIN", String.valueOf(bookId));
            Log.d("CHECKOUT", "RECEBI");

            booksCache.removeBook(bookId);
            libraryCache.getLibrary(libraryID).removeBook(bookId);
            Log.d("BOOK", "REMOVED BOOK FROM CACHE");

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public void addLibraryToFavorites(int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/add_fav?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

         if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

    }

    public void removeLibraryFromFavorites(int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/remove_fav?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

    }

    
    public List<Book> listBooksFromLibrary(int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/books?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        // Create a JSON object
        JsonObject query = new JsonObject();
        query.addProperty("id", libraryId);

        String jsonString = query.toString();

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        if (connection.getResponseCode() == 200) {
            JsonArray jsonArray = getJsonArrayFromResponse(connection.getInputStream());

            List<Book> books = new ArrayList<>();

            for (JsonElement jsonElement : jsonArray) {
                JsonObject item = jsonElement.getAsJsonObject();
                int id = item.get("id").getAsInt();
                String title = item.get("title").getAsString();
                String base64Cover = item.get("cover").getAsString();
                byte[] cover = Base64.getDecoder().decode(base64Cover);
                String barcode = item.get("barcode").getAsString();
                boolean activeNotif = item.get("activeNotif").getAsBoolean();

                // Create a Book object and add it to the list
                books.add(new Book(id, title, cover, barcode, activeNotif));
            }

            booksCache.addBooks(books);
            return books;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }


//    public Book getBookByTitle(String bookTitle, String path) throws IOException {
//
//        // Create a JSON payload with the book title
//        String payload = "{\"title\": \"" + bookTitle + "\"}";
//
//        // Create a connection to the backend API
//        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + path).openConnection();
//        connection.setRequestMethod("POST");
//        connection.setRequestProperty("Content-Type", "application/json");
//        connection.setDoOutput(true);
//
//        // Send the JSON payload to the backend
//        OutputStream outputStream = connection.getOutputStream();
//        outputStream.write(payload.getBytes());
//        outputStream.flush();
//        outputStream.close();
//
//
//        if (connection.getResponseCode() == 200) {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            StringBuilder response = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                response.append(line);
//            }
//            reader.close();
//
//            // Parse the response JSON to a Book object
//
//            Gson gson = new Gson();
//            JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class);
//
//            int id = jsonObject.get("id").getAsInt();
//            String title = jsonObject.get("title").getAsString();
//            String base64Cover = jsonObject.get("cover").getAsString();
//            byte[] cover = Base64.getDecoder().decode(base64Cover);
//            boolean activeNotif = jsonObject.get("activeNotif").getAsBoolean();
//            String barcode = item.get("barcode").getAsString();
//
//            // Return a Book object
//            return new Book(id, title, cover, activeNotif);
//        }
//        throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
//    }

    /*
    public void startWebSocket() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }

        try {
            webSocketClient = new WSClient(new URI(wsEndpoint), new HashMap<>());
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public class WSClient extends WebSocketClient {
        public WSClient(URI serverUri, Map<String, String> httpHeaders) {
            super(serverUri, httpHeaders);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.i("WebSocket", "Opened");
        }

        @Override
        public void onMessage(String message) {
            Log.i("WebSocket", "Message: " + message);

            if (message != null) {
                try {
                    JsonReader jsonReader = new JsonReader(new StringReader(message));
                    jsonReader.setLenient(true);
                    boolean state = jsonReader.nextBoolean();
                    runOnUiThread(() -> {
                        ((Switch) findViewById(R.id.state_switch)).setChecked(state);
                    });
                } catch (IOException e) {
                    throw new RuntimeException("Malformed message on websocket", e);
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (this != webSocketClient) return;
            Log.i("WebSocket", (remote ? "Remotely " : "Locally ") + "Closed " + reason);
        }

        @Override
        public void onError(Exception ex) {
            if (this != webSocketClient) return;
            Log.i("WebSocket", "Error " + ex.getMessage());
        }
    }
   */


    /** -----------------------------------------------------------------------------
     *                                 AUXILIARY FUNCTIONS
     -------------------------------------------------------------------------------- */

    private JsonObject getJsonObjectFromResponse(InputStream inputStream) {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();

            Gson gson = new Gson();
            return gson.fromJson(jsonString.toString(), JsonObject.class);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private JsonArray getJsonArrayFromResponse(InputStream inputStream) {

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();

            Gson gson = new Gson();
            return gson.fromJson(jsonString.toString(), JsonArray.class);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}