package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;

import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

import android.app.Activity;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Switch;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ServerConnection {
    public static final String endpoint = "http://192.92.147.54:5000";
    //public static final String wsEndpoint = "ws://cmov2-docentes-tp-1.vps.tecnico.ulisboa.pt:5000/ws";
    //WebSocketClient webSocketClient = null;

    // Method called when creating a new Library -- WORKING!
    public void createLibrary(String name, LatLng latLng, String address, byte[] photo) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + "/libraries").openConnection();
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
            assert responseJson != null;
            int libraryId = responseJson.get("libId").getAsInt();

            Library newLibrary = new Library(libraryId, name, latLng, address, new ArrayList<>(), photo);
            libraryCache.addLibrary(libraryId, newLibrary);
            Log.d("LIBRARY", "ADDED LIBRARY TO FRONTEND");

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }


    // Method called when adding a book to a library
    public void addBook(String path, Book book, Library library) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + path).openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", book.getId());
        jsonObject.addProperty("title", book.getTitle());
        jsonObject.addProperty("cover", Base64.getEncoder().encodeToString(book.getCover()));
        jsonObject.addProperty("activeNotif", book.isActiveNotif());

        // Add the id of the library where the book is
        jsonObject.addProperty("libId", library.getId());

        // Convert the JSON object to a string
        String jsonString = jsonObject.toString();

        DataOutputStream outputStream = (DataOutputStream) connection.getOutputStream();
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.close();

        if (connection.getResponseCode() == 200) {

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }


    public List<Library> getAllLibraries(String path) throws IOException {
        List<Library> libraries = new ArrayList<>();

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + path).openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == 200) {
            JsonReader jsonReader = new JsonReader(new InputStreamReader(connection.getInputStream()));
            jsonReader.setLenient(true);
            JsonArray jsonArray = new JsonParser().parse(String.valueOf(jsonReader)).getAsJsonArray();

            for (JsonElement jsonElement : jsonArray) {

                JsonObject jsonObject = jsonElement.getAsJsonObject();

                int id = jsonObject.get("id").getAsInt();
                String name = jsonObject.get("name").getAsString();
                JsonObject locationObject = jsonObject.get("location").getAsJsonObject();
                double latitude = locationObject.get("latitude").getAsDouble();
                double longitude = locationObject.get("longitude").getAsDouble();
                LatLng latLng = new LatLng(latitude, longitude);
                String address = jsonObject.get("address").getAsString();
                List<Integer> bookIds = new ArrayList<>();

                String base64Photo = jsonObject.get("photo").getAsString();
                byte[] photo = Base64.getDecoder().decode(base64Photo);

                JsonArray bookIdsArray = jsonObject.get("bookIds").getAsJsonArray();
                for (JsonElement bookIdElement : bookIdsArray) {
                    int bookId = bookIdElement.getAsInt();
                    bookIds.add(bookId);
                }

                // Create a Library object and add it to the list
                Library library = new Library(id, name, latLng, address, bookIds, photo);
                libraries.add(library);
            }
        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

        return libraries;
    }

    public int getBook(String barcode) throws IOException {
        String url = endpoint + "/books/get?barcode=" + barcode;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonParser jsonParser = new JsonParser();
            JsonObject responseJson = jsonParser.parse(new InputStreamReader(connection.getInputStream()))
                    .getAsJsonObject();

            int bookId = responseJson.get("bookId").getAsInt();
            return bookId;
        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

    }

    // Method called when checking in a new book -- WORKING!
    public void checkInBook(String barcode, int libraryID) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + "/" + libraryID + "/books/checkin").openConnection();
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
            int bookId = responseJson.get("bookId").getAsInt();
            String title = responseJson.get("title").getAsString();
            String _barcode = responseJson.get("barcode").getAsString();
            // just to confirm that the barcodes are the same
            if (!_barcode.equals(barcode)){
                Log.d("CHECK IN", "DIFFERENT BARCODES");
            }
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
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + "/" + libraryID + "/books/checkin/newbook").openConnection();
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
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

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

    // Method called when checking out a book -- WORKING!
    public void checkOutBook(String barcode, int libraryID) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + "/" + libraryID + "/books/checkout").openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("barcode", barcode);
        jsonObject.addProperty("libId", libraryID);

        String jsonString = jsonObject.toString();

        Log.d("CHECKOUT", "CRIEI JSON");

        DataOutputStream outputStream = new DataOutputStream((OutputStream) connection.getOutputStream());
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


    public List<Book> listBooksFromLibrary(int libraryID) throws IOException {
        List<Book> books = new ArrayList<>();

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint + "/libraries/" + libraryID + "/books").openConnection();
        connection.setRequestMethod("GET");

        // Create a JSON object
        JsonObject query = new JsonObject();
        query.addProperty("id", libraryID);

        String jsonString = query.toString();

        DataOutputStream outputStream = new DataOutputStream((OutputStream) connection.getOutputStream());
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        if (connection.getResponseCode() == 200) {
            JsonReader jsonReader = new JsonReader(new InputStreamReader(connection.getInputStream()));
            jsonReader.setLenient(true);
            JsonArray jsonArray = new JsonParser().parse(String.valueOf(jsonReader)).getAsJsonArray();

            for (JsonElement jsonElement : jsonArray) {
                JsonObject item = jsonElement.getAsJsonObject();
                int id = item.get("id").getAsInt();
                String title = item.get("title").getAsString();
                String base64Cover = item.get("cover").getAsString();
                byte[] cover = Base64.getDecoder().decode(base64Cover);
                String barcode = item.get("barcode").getAsString();
                boolean activeNotif = item.get("activeNotif").getAsBoolean();

                // Create a Book object and add it to the list
                Book book = new Book(id, title, cover, barcode, activeNotif);
                books.add(book);
            }

            booksCache.addBooks(books);

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

        return books;
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
}