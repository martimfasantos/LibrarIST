package pt.ulisboa.tecnico.cmov.librarist;

import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.markerMap;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.userId;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.libraryCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.booksCache;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.currentLocation;
import static pt.ulisboa.tecnico.cmov.librarist.MainActivity.MAX_DIST_KM_CACHE;

import pt.ulisboa.tecnico.cmov.librarist.caches.LibraryCache;
import pt.ulisboa.tecnico.cmov.librarist.models.Book;
import pt.ulisboa.tecnico.cmov.librarist.models.Library;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import java.util.HashMap;
import java.util.List;


public class ServerConnection {
    // Server and port
    public static final String endpoint = "https://gp-cmov2-cmu-project-1.vps.tecnico.ulisboa.pt:5000";


    /** -----------------------------------------------------------------------------
     *                                  USER ACCOUNTS
     -------------------------------------------------------------------------------- */

    public int createGuestUser() throws IOException {
        String url = endpoint + "/users/guest/create";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Received values
            assert responseJson != null;
            return responseJson.get("userId").getAsInt();

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public boolean validateUser(int userId) throws IOException {
        String url = endpoint + "/users/validate?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Received values
            assert responseJson != null;
            return responseJson.get("validUser").getAsBoolean();

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public int registerUser(int userId, String username, String password) throws IOException {
        String url = endpoint + "/users/register?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", username);
        jsonObject.addProperty("password", password);

        String jsonString = jsonObject.toString();

        DataOutputStream outputStream = new DataOutputStream((OutputStream) connection.getOutputStream());
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Received values
            assert responseJson != null;
            // User id confirmation for safety
            if (responseJson.get(("userId")).getAsInt() != -1 &
                    responseJson.get(("userId")).getAsInt() != userId){
                Log.d("REGISTER USER", "DIFFERENT IDS");
            }
            return responseJson.get("userId").getAsInt();

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public int loginUser(String username, String password) throws IOException {
        String url = endpoint + "/users/login?username=" + username + "&password=" + password;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Received values
            assert responseJson != null;
            return responseJson.get("userId").getAsInt();

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    // Method called when creating a new Library
    public void createLibrary(String name, LatLng latLng, String address, byte[] photo) throws IOException {
        String url = endpoint + "/libraries/create";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
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

            Library newLibrary = new Library(libraryId, name, latLng, address, photo, new ArrayList<>(), false);
            libraryCache.addLibrary(newLibrary);
            Log.d("LIBRARY", "ADDED LIBRARY TO FRONTEND");

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public Library getLibrary(int libraryId) throws IOException {
        String url = endpoint + "/libraries/get?libId=" + libraryId + "&userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Get library properties
            assert responseJson != null;
            int libId = responseJson.get("libId").getAsInt();
            if(libId != libraryId){
                Log.d("GET LIBRARY", "DIFFERENT LIB IDS");
            }
            String name = responseJson.get("name").getAsString();
            double latitude = responseJson.get("latitude").getAsDouble();
            double longitude = responseJson.get("longitude").getAsDouble();
            String address = responseJson.get("address").getAsString();
            byte[] photo = Base64.getDecoder().decode(responseJson.get("photo").getAsString());
            boolean favorite = responseJson.get("isFavorite").getAsBoolean();

            // Process bookIds
            JsonArray bookIdsArray = responseJson.getAsJsonArray("bookIds");
            List<Integer> bookIds = new ArrayList<>();
            for (JsonElement bookIdElement : bookIdsArray) {
                bookIds.add(bookIdElement.getAsInt());
            }

            Library library = new Library(libId, name, new LatLng(latitude, longitude),
                    address, photo, bookIds, favorite);
            // Add library to cache
            libraryCache.addLibrary(library);

            return library;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public Library getLibraryNoPhoto(int libraryId) throws IOException {
        String url = endpoint + "/libraries/get/no_photo?libId=" + libraryId + "&userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Get library properties (without the photo)
            assert responseJson != null;
            int libId = responseJson.get("libId").getAsInt();
            if(libId != libraryId){
                Log.d("GET LIBRARY NO PHOTO", "DIFFERENT LIB IDS");
            }
            String name = responseJson.get("name").getAsString();
            double latitude = responseJson.get("latitude").getAsDouble();
            double longitude = responseJson.get("longitude").getAsDouble();
            String address = responseJson.get("address").getAsString();
            boolean favorite = responseJson.get("isFavorite").getAsBoolean();

            // Process bookIds
            JsonArray bookIdsArray = responseJson.getAsJsonArray("bookIds");
            List<Integer> bookIds = new ArrayList<>();
            for (JsonElement bookIdElement : bookIdsArray) {
                bookIds.add(bookIdElement.getAsInt());
            }

            Library library = new Library(libId, name, new LatLng(latitude, longitude),
                    address, null, bookIds, favorite);
            // Add library to cache
            libraryCache.addLibrary(library);

            return library;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public void getLibraryPhoto(int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/photo";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Get photo for the library
            assert responseJson != null;
            byte[] photo = Base64.getDecoder().decode(responseJson.get("photo").getAsString());

            // Update the item in cache
            libraryCache.addLibraryPhoto(libraryId, photo);

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

    }

    public List<Library> getLibrariesWithBook(int bookId, int radius) throws IOException {
        String url = endpoint + "/books/" + bookId + "/libraries?lat=" + currentLocation.getLatitude()
                + "&lon=" + currentLocation.getLongitude() + "&radius=" + radius
                + "&userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
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
                boolean favorite = libraryObject.get("isFavorite").getAsBoolean();

                // Process bookIds
                JsonArray bookIdsArray = libraryObject.getAsJsonArray("bookIds");
                List<Integer> bookIds = new ArrayList<>();
                for (JsonElement bookIdElement : bookIdsArray) {
                    bookIds.add(bookIdElement.getAsInt());
                }

                Library library = new Library(libId, name, new LatLng(latitude, longitude),
                        address, null, bookIds, favorite);

                // If cache already has the item, use the new information with the stored photo
                if (libraryCache.containsLibrary(libId)){
                    library.setPhoto(libraryCache.getLibrary(libId).getPhoto());
                }
                libraryCache.addLibrary(library);
                libraries.add(library);
            }

            Log.d("GOT LIBRARIES FROM SERVER", libraries.toString());
            return libraries;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public HashMap<Integer, MarkerOptions> getLibrariesMarkers(LatLng coordinates, int radius) throws IOException {
        String url = endpoint + "/libraries/markers?lat=" + coordinates.latitude
                + "&lon=" + coordinates.longitude + "&radius=" + radius + "&userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonArray responseJsonArray = getJsonArrayFromResponse(connection.getInputStream());

            // Parse libraries markers
            HashMap<Integer, MarkerOptions> markers = new HashMap<>();
            for (JsonElement element : responseJsonArray){
                // Get library marker object
                JsonObject libraryMarker = element.getAsJsonObject();
                // Get library marker properties
                int libId = libraryMarker.get("libId").getAsInt();
                String name = libraryMarker.get("name").getAsString();
                double latitude = libraryMarker.get("latitude").getAsDouble();
                double longitude = libraryMarker.get("longitude").getAsDouble();
                String address = libraryMarker.get("address").getAsString();
                boolean favorite = libraryMarker.get("isFavorite").getAsBoolean();

                int imageResource;
                if (favorite){
                    imageResource = R.drawable.marker_library_fav;
                    Log.d("FAV", "FAV");
                } else {
                    imageResource = R.drawable.marker_library;
                    Log.d("FAV", "NOT FAV");
                }

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .title(name)
                        .snippet(address)
                        .icon(BitmapDescriptorFactory.fromResource(imageResource));

                markers.put(libId, markerOptions);
            }

            return markers;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

    }

    public void loadLibrariesToCache() throws IOException {
        String url = endpoint + "/libraries/load?lat=" + currentLocation.getLatitude()
                + "&lon=" + currentLocation.getLongitude() + "&radius=" + MAX_DIST_KM_CACHE
                + "&userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Parse libraries
            JsonArray librariesJsonArray = responseJson.getAsJsonArray("libraries");

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

                libraryCache.addLibrary(new Library(libId, name, new LatLng(latitude, longitude),
                        address, photo, bookIds, favorite));
            }

            // Parse books in the libraries
            JsonArray booksJsonArray = responseJson.getAsJsonArray("books");

            for (JsonElement element : booksJsonArray){
                // Get book object
                JsonObject bookObject = element.getAsJsonObject();
                // Get book properties
                int bookId = bookObject.get("bookId").getAsInt();
                String title = bookObject.get("title").getAsString();
                byte[] cover = Base64.getDecoder().decode(bookObject.get("cover").getAsString());
                String barcode = bookObject.get("barcode").getAsString();
                boolean activNotif = bookObject.get("activNotif").getAsBoolean();

                // Process rates
                JsonArray ratesArray = bookObject.getAsJsonArray("rates");
                List<Integer> rates = new ArrayList<>();
                for (JsonElement bookIdElement : ratesArray) {
                    rates.add(bookIdElement.getAsInt());
                }

                booksCache.addBook(new Book(bookId, title,cover, barcode, activNotif, rates));
            }

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public List<Book> getAllBooks() throws IOException {
        String url = endpoint + "/books?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonArray responseJsonArray = getJsonArrayFromResponse(connection.getInputStream());
            assert responseJsonArray != null;

            List<Book> books = getBookListFromJsonArray(responseJsonArray);
            // Save loaded books in cache
            booksCache.addBooks(books);

            return books;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    
    public List<Book> getBooksFromLibrary(int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/books?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonArray responseJsonArray = getJsonArrayFromResponse(connection.getInputStream());
            assert responseJsonArray != null;

            List<Book> books = getBookListFromJsonArray(responseJsonArray);

            for (Book book: books) {
                // If cache already has the item, use the new information with the stored cover
                if (booksCache.containsBook(book.getId())){
                    book.setCover(booksCache.getBook(book.getId()).getCover());
                }
                booksCache.addBook(book);
            }

            return books;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public List<Book> getBooksByPage(int booksPage) throws IOException {
        String url = endpoint + "/books/pages/" + booksPage + "?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonArray responseJsonArray = getJsonArrayFromResponse(connection.getInputStream());
            assert responseJsonArray != null;

            List<Book> books = getBookListFromJsonArray(responseJsonArray);

            for (Book book: books) {
                // If cache already has the item, use the new information with the stored cover
                if (booksCache.containsBook(book.getId())){
                    book.setCover(booksCache.getBook(book.getId()).getCover());
                }
                booksCache.addBook(book);
            }

            return books;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public int findBook(String barcode) throws IOException {
        String url = endpoint + "/books/find?barcode=" + barcode;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());
            assert responseJson != null;
            return responseJson.get("bookId").getAsInt();
        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public Book getBook(int bookId) throws IOException {
        String url = endpoint + "/books/get?bookId=" + bookId + "&userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
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

            // Process rates
            JsonArray ratesArray = responseJson.get("rates").getAsJsonArray();
            List<Integer> rates = new ArrayList<>();
            for (JsonElement bookIdElement : ratesArray) {
                rates.add(bookIdElement.getAsInt());
            }

            Book bookResponse = new Book(bookId, title, cover, barcode, activNotif, rates);
            booksCache.addBook(bookResponse);

            return bookResponse;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public Book getBookNoCover(int bookId) throws IOException {
        String url = endpoint + "/books/get/no_cover?bookId=" + bookId + "&userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
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
            String barcode = responseJson.get("barcode").getAsString();
            boolean activNotif = responseJson.get("activNotif").getAsBoolean();

            // Process rates
            JsonArray ratesArray = responseJson.get("rates").getAsJsonArray();
            List<Integer> rates = new ArrayList<>();
            for (JsonElement bookIdElement : ratesArray) {
                rates.add(bookIdElement.getAsInt());
            }

            Book bookResponse = new Book(bookId, title, null, barcode, activNotif, rates);
            booksCache.addBook(bookResponse);

            return bookResponse;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public void getBookCover(int bookId) throws IOException {
        String url = endpoint + "/book/" + bookId + "/photo";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            // Get photo for the library
            assert responseJson != null;
            byte[] cover = Base64.getDecoder().decode(responseJson.get("cover").getAsString());

            // Update the item in cache
            booksCache.addBookCover(bookId, cover);

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

    }

    public void reportBook(int bookId) throws IOException {
        String url = endpoint + "/books/" + bookId + "/report?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }

        // Remove book from the books cache
        booksCache.removeBook(bookId);
        Log.d("BOOKS CACHE", booksCache.getBooks().toString());
    }

    // Method called when checking in a new book -- WORKING!
    public void checkInBook(String barcode, int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/books/checkin?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("barcode", barcode);
        jsonObject.addProperty("libId", libraryId);

        String jsonString = jsonObject.toString();

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

            // Process rates
            JsonArray ratesArray = responseJson.get("rates").getAsJsonArray();
            List<Integer> rates = new ArrayList<>();
            for (JsonElement bookIdElement : ratesArray) {
                rates.add(bookIdElement.getAsInt());
            }

            Book newBook = new Book(bookId, title, cover, barcode, activNotif, rates);
            libraryCache.getLibrary(libraryId).addBook(bookId);
            booksCache.addBook(newBook);
            Log.d("CHECKIN", "ADDED BOOK TO CACHE");

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }



    // Method called when checking in a new book -- WORKING!
    public void checkInNewBook(String title, byte[] cover, String barcode, int libraryID) throws IOException {
        String url = endpoint + "/libraries/" + libraryID + "/books/checkin/newbook?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
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

            Book newBook = new Book(bookId, title, cover, barcode, false, new ArrayList<>());
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
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
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

    public void checkOutBook(String barcode, int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/books/checkout";

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("barcode", barcode);

        String jsonString = jsonObject.toString();

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
            libraryCache.getLibrary(libraryId).removeBook(bookId);
            Log.d("BOOK", "REMOVED BOOK FROM CACHE");

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public void rateBook(int bookId, int rating) throws IOException {
        String url = endpoint + "/books/" + bookId + "/rate?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create a JSON object
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("rating", rating);

        String jsonString = jsonObject.toString();

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        if (connection.getResponseCode() == 200) {
            JsonObject responseJson = getJsonObjectFromResponse(connection.getInputStream());

            assert responseJson != null;
            JsonArray ratesArray = responseJson.getAsJsonArray("rates");
            List<Integer> rates = new ArrayList<>();
            for (JsonElement bookIdElement : ratesArray) {
                rates.add(bookIdElement.getAsInt());
            }

            booksCache.getBook(bookId).setRates(rates);

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }


    public void addLibraryToFavorites(int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/add_fav?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

         if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }

    public void removeLibraryFromFavorites(int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/remove_fav?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }


    public void reportLibrary(int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/report?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }


    public List<Book> listBooksFromLibrary(int libraryId) throws IOException {
        String url = endpoint + "/libraries/" + libraryId + "/books?userId=" + userId;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000); // Set a timeout of 5 seconds
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
            assert jsonArray != null;
            List<Book> books = getBookListFromJsonArray(jsonArray);
            booksCache.addBooks(books);
            return books;

        } else {
            throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
        }
    }


    public List<Book> filterBooksByTitle(String bookTitle) throws IOException {

        String url = endpoint + "/books/filter" + "?title=" + bookTitle + "&userId=" + userId;

        // Create a connection to the backend API
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        if (connection.getResponseCode() == 200) {
            JsonArray responseJsonArray = getJsonArrayFromResponse(connection.getInputStream());
            assert responseJsonArray != null;
            return getBookListFromJsonArray(responseJsonArray);
        }
        throw new RuntimeException("Unexpected response: " + connection.getResponseMessage());
    }


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

    private List<Book> getBookListFromJsonArray(JsonArray jsonArray) {
        List<Book> books = new ArrayList<>();
        jsonArray.forEach(jsonElement -> {

            JsonObject obj = jsonElement.getAsJsonObject();

            // Process rates
            JsonArray ratesArray = obj.getAsJsonArray("rates");
            List<Integer> rates = new ArrayList<>();
            for (JsonElement bookIdElement : ratesArray) {
                rates.add(bookIdElement.getAsInt());
            }

            books.add(new Book(obj.get("bookId").getAsInt(),
                    obj.get("title").getAsString(),
                    (obj.get("cover") != null) ? Base64.getDecoder().decode(obj.get("cover").getAsString()) : null,
                    obj.get("barcode").getAsString(),
                    obj.get("activNotif").getAsBoolean(),
                    rates));
        });
        return books;
    }
}