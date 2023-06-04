import base64
import json
from flask import jsonify
from PIL import Image
from io import BytesIO
from typing import Dict
from haversine import haversine, Unit

from models.book import Book
from models.library import Library
from models.user import User

class Server:

    def __init__(self):
        # counters to attribute ids to new objects
        self.id_users_counter = 0
        self.id_library_counter = 0
        self.id_books_counter = 0

        # dictionaries to store objects
        self.books: Dict[int, Book] = {}
        self.libraries: Dict[int, Library] = {}
        self.users: Dict[int, User] = {0: User(0, "admin", "admin")} # admin user (for testing purposes)


    # Add new library
    def create_new_library(
            self, 
            name: str, 
            location: tuple, 
            photo: base64, 
            address: str
        ):
        lib_id = self.id_library_counter
        self.id_library_counter += 1

        photo_path = f'./images/libraries/{lib_id}.jpg'

        print(name)
        print(location)
        print(address)
        print(photo_path)

        img = Image.open(BytesIO(photo))
        img.save(photo_path, "JPEG")

        self.libraries[lib_id] = Library(lib_id, name, location, photo_path, address)
        print("ID")
        print(lib_id)
        print(self.libraries)
        print("Users: ", self.users)
        return jsonify({"libId": lib_id}), 200
    
    # List books from library
    def list_all_books_from_library(self, lib_id: int):
        return jsonify(self.libraries[lib_id].available_books), 200
    

    # ------------------------------------------------------------
    # -                         MARKERS                          -
    # ------------------------------------------------------------

    # List libraries markers info in a given radius
    def get_libraries_markers(self, lat: float, lon: float, radius: int, user_id: int):
        libraries_markers = []
        for lib in self.libraries.values():
            if (haversine((lat, lon), lib.location, unit=Unit.KILOMETERS) <= radius):
                libraries_markers.append(self.library_marker_to_json(lib, user_id))
        return jsonify(libraries_markers), 200

     # Covert library marker into json
    def library_marker_to_json(self, library: Library, user_id: int):
        return {"libId": library.id,
                "name": library.name,
                "latitude": library.location[0],
                "longitude": library.location[1],
                "address": library.address,
                "isFavorite": library.is_user_favorite(user_id)} 
    

    # ------------------------------------------------------------
    # -                         LOAD                             -
    # ------------------------------------------------------------

    # List libraries in a given radius
    def get_libraries_to_load_cache(self, lat: float, lon: float, radius: int, user_id: int):
        libraries, books = [], []
        for lib in self.libraries.values():
            if (haversine((lat, lon), lib.location, unit=Unit.KILOMETERS) <= radius):
                libraries.append(self.library_to_json(lib, user_id))
                books.extend(self.book_to_json(self.books[book_id], user_id) for book_id in lib.available_books)
        return jsonify({"libraries": libraries, "books": books}), 200
                
    # Covert library into json
    def library_to_json(self, library: Library, user_id: int):
        photo = library.photo

        with open(photo, "rb") as file:
            photo_data = file.read()

        photo_base64 = base64.b64encode(photo_data).decode("utf-8")

        return {"libId": library.id,
                "name": library.name,
                "latitude": library.location[0],
                "longitude": library.location[1],
                "address": library.address,
                "photo": photo_base64,
                "bookIds": library.available_books,
                "isFavorite": library.is_user_favorite(user_id)}    
                 
    # Add library to users favorites
    def add_favorite_lib(self, lib_id: int, user_id: int):
        self.users[user_id].add_library(lib_id)
        self.libraries[lib_id].add_favorite_user(user_id)
        print(f"User: {user_id} \t Fav Lib: {self.users[user_id].favorite_libraries}")
        return jsonify({}), 200

    # Remove library from users favorites
    def remove_favorite_lib(self, lib_id: int, user_id: int):
        self.users[user_id].remove_library(lib_id)
        self.libraries[lib_id].remove_favorite_user(user_id)
        print(f"User: {user_id} \t Fav Lib: {self.users[user_id].favorite_libraries}")
        return jsonify({}), 200
    

    # Get libraries with the given book available
    def get_libraries_with_book(self, book_id: int, user_id: int):
        libraries = []
        for library in self.libraries.values():
            if book_id in library.available_books:
                libraries.append(self.library_to_json(library, user_id))
        return jsonify(libraries), 200

    # Find book id from barcode
    def find_book(self, barcode: str):
        return jsonify({"bookId": self.get_book_id_from_barcode(barcode)}), 200
    

    # Covert book into json
    def book_to_json(self, book: Book, user_id: int):
        cover = book.cover

        with open(cover, "rb") as file:
            cover_data = file.read()

        cover_base64 = base64.b64encode(cover_data).decode("utf-8")

        return {"bookId": book.id,
                "title": book.title,
                "barcode": book.barcode,
                "cover": cover_base64,
                "activNotif": book.is_user_to_notify(user_id)}

    
    # Get book with book id
    def get_book(self, book_id: int, user_id: int):
        return jsonify(self.book_to_json(self.books[book_id], user_id)), 200
    
    # Get all books
    def get_all_books(self, user_id: int):
        all_books = []
        for book in self.books.values():
            all_books.append(self.book_to_json(book, user_id))

        print([book_json["bookId"] for book_json in all_books])
        return jsonify(all_books), 200
    
    def check_in_book(self, barcode: str, lib_id: int):
        book_id = self.get_book_id_from_barcode(barcode)
        self.libraries[lib_id].add_book(book_id)

        return self.get_book(book_id)
    
    # User checkin book at a library
    def check_in_new_book(
            self,
            title: str,
            cover: base64,
            barcode: str,
            lib_id: int
        ): 
        book_id = self.id_books_counter
        self.id_books_counter += 1

        book_cover = f'./images/books/{book_id}.jpg'

        print(title)
        print(book_cover)
        print(barcode)
        print(lib_id)

        if cover is not None:
            img = Image.open(BytesIO(cover))
            img.save(book_cover, "JPEG")

        # Create new book and add it to books
        self.books[book_id] = Book(book_id, title, book_cover, barcode, lib_id)
        # Add book to library
        self.libraries[lib_id].add_book(book_id)

        print(self.libraries[lib_id].available_books)

        return jsonify({"bookId": book_id}), 200   

    # Get book id from barcode for a given library
    def get_book_from_library(self, barcode: str, lib_id: int):
        return jsonify({"bookId": self.get_book_id_from_barcode_in_library(barcode, lib_id)}), 200   

    # User checkin book at a library
    def check_out_book(self, barcode: str, lib_id: int):
        # find book id for the given barcode
        book_id = self.get_book_id_from_barcode(barcode)

        # if book not found return 404
        if book_id == -1:
            return 404
        
        # remove book from library (note: it is not deleted from server)
        self.libraries[lib_id].remove_book(book_id)

        print(book_id)
        print(lib_id)
        print(self.libraries[lib_id].available_books)
        return jsonify({"bookId": book_id}), 200

    # Filter book by title
    def filter_books_by_title(self, filter_title):
        books = list(self.books.values())
        filtered_books = list(filter(lambda book: book.title.upper().find(filter_title.upper()) != -1, books))
        return json.dumps(filtered_books, default=vars)

    # Add new user
    def add_new_user(self, username, password):        
        id = len(self.users)
        self.users[id] = User(id, username, password)
        return json.dumps({"status": 200})

    # Add user to book notifications
    def add_user_book_notif(self, user_id, book_id):
        self.books[book_id].add_user_to_notify(user_id)
        return json.dumps({"status": 200})

    # Remove user from book notifications
    def remove_user_book_notif(self, user_id, book_id):
        self.books[book_id].remove_user_to_notify(user_id)
        return json.dumps({"status": 200})

    # Notify all users when book available in favorite libraries
    def notify_users(self, book_id, lib_id):    
        for user_id in self.books[book_id].usersToNotify:
            if (lib_id in self.users[user_id].favoriteLibs):
                return 0
                # notify user

    # Get book id from barcode
    def get_book_id_from_barcode(self, barcode):
        book_id = -1
        for book in self.books.values():
            if book.barcode == barcode:
                book_id = book.id
                break
        return book_id
    
    # Get book id from barcode
    def get_book_id_from_barcode_in_library(self, barcode, lib_id):
        book_id = -1
        for book_id in self.libraries[lib_id].available_books:
            if self.books[book_id] == barcode:
                book_id = self.books[book_id]
                break
        return book_id

    def get_users_fav_libs(self, user_id):
        return json.dumps(self.users[user_id].favoriteLibs, default=vars)

    def get_library_by_id(self, lib_id):
        return json.dumps(self.libraries[lib_id], default=vars)
    
    def get_book_by_id(self, book_id):
        return json.dumps(self.books[book_id], default=vars)
    
    def get_user_by_id(self, user_id):
        return json.dumps(self.users[user_id], default=vars)
    
    def get_libraries(self):
        return json.dumps(list(self.libraries.values()), default=vars)
    
    def get_users(self):
        return json.dumps(list(self.users.values()), default=vars)


