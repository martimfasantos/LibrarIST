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
        self.id_users_counter = 1
        self.id_library_counter = 0
        self.id_books_counter = 0
        self.page_size = 8

        # dictionaries to store objects
        self.books: Dict[int, Book] = {}
        self.libraries: Dict[int, Library] = {}
        self.users: Dict[int, User] = {0: User(0, "admin", "admin")} # admin user (for testing purposes)

        # populate 
        self.populate()


    # ------------------------------------------------------------
    # -                         USERS                            -
    # ------------------------------------------------------------

    # Create guest user
    def create_guest_user(self):
        user_id = self.id_users_counter
        self.id_users_counter += 1

        self.users[user_id] = User(user_id, None, None)
        return jsonify({"userId": user_id}), 200
    
    # Validate user
    def validate_user(self, user_id: int):
        if user_id in self.users.keys() and user_id != 0:
            return jsonify({"validUser": True}), 200
        else:
            return jsonify({"validUser": False}), 200

    # Create new user
    def create_new_user(self, user_id: int, username: str, password: str):
        # check if username already exists
        for user in self.users.values():
            if user.username == username:
                return jsonify({"userId": -1}), 200
        
        if user_id <= 0:
            return jsonify({"userId": -1}), 200
        
        # modify guest user to new user
        self.users[user_id] = User(user_id, username, password)
        return jsonify({"userId": user_id}), 200

    # Login user
    def login_user(self, username: str, password: str):
        for user in self.users.values():
            if user.username == username and user.password == password:
                return jsonify({"userId": user.id}), 200
        return jsonify({"userId": -1}), 200
    

    # Create new library
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

        self.libraries[lib_id] = Library(lib_id, name, location, address, photo_path)
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
            if ( 
                not lib.hidden and \
                lib not in self.users[user_id].reported_libraries and \
                haversine((lat, lon), lib.location, unit=Unit.KILOMETERS) <= radius 
            ):
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
            if ( 
                not lib.hidden and \
                lib.id not in self.users[user_id].reported_libraries and \
                haversine((lat, lon), lib.location, unit=Unit.KILOMETERS) <= radius 
            ):
                books_list = [self.books[book_id] for book_id in lib.available_books]
                books_sorted = self.sort_books_by_average_rate(books_list)
                books.extend(self.book_to_json(book, user_id) for book in books_sorted)
        return jsonify({"libraries": libraries, "books": books}), 200
    
    # Get library
    def get_library(self, lib_id: int, user_id: int):
        return jsonify(self.library_to_json(self.libraries[lib_id], user_id)), 200
    
    # Get library without photo
    def get_library_no_photo(self, lib_id: int, user_id: int):
        library_json = self.library_to_json(self.libraries[lib_id], user_id)
        del library_json["photo"]
        print(library_json)
        return jsonify(library_json), 200
    
    # Get library photo
    def get_library_photo(self, lib_id: int):
        photo = self.libraries[lib_id].photo
        with open(photo, "rb") as file:
            photo_data = file.read()
        return jsonify({"photo": base64.b64encode(photo_data).decode("utf-8")}), 200
                
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
    
    # Report library
    def report_library(self, lib_id: int, user_id: int):
        self.users[user_id].report_library(lib_id)
        self.libraries[lib_id].add_report()
        return jsonify({}), 200
    

    # Get libraries with the given book available
    def get_libraries_with_book(self,  book_id: int, lat: float, lon: float, radius: int, user_id: int):
        libraries = []
        for library in self.libraries.values():
            if ( 
                not library.hidden and \
                library.id not in self.users[user_id].reported_libraries and \
                book_id in library.available_books and \
                haversine((lat, lon), library.location, unit=Unit.KILOMETERS) <= radius 
            ):
                library_json = self.library_to_json(library, user_id)
                del library_json["photo"]
                libraries.append(library_json)
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
                "activNotif": book.is_user_to_notify(user_id),
                "rates": book.ratings}

    
    # Get book with book id
    def get_book(self, book_id: int, user_id: int):
        return jsonify(self.book_to_json(self.books[book_id], user_id)), 200
    
    # Get library without photo
    def get_book_no_cover(self, book_id: int, user_id: int):
        book_json = self.book_to_json(self.books[book_id], user_id)
        del book_json["cover"]
        print(book_json)
        return jsonify(book_json), 200
    
    # Get library photo
    def get_book_cover(self, book_id: int):
        cover = self.books[book_id].cover
        with open(cover, "rb") as file:
            cover_data = file.read()
        return jsonify({"cover": base64.b64encode(cover_data).decode("utf-8")}), 200
    
    # Get all books
    def get_all_books(self, user_id: int):
        all_books = []
        books_sorted = self.sort_books_by_average_rate(
            book for book in self.books.values() if not book.hidden)
        
        for book in books_sorted:
            if book.id not in self.users[user_id].reported_books:
                all_books.append(self.book_to_json(book, user_id))

        print([book_json["bookId"] for book_json in all_books])
        return jsonify(all_books), 200
    
    # Get books by page
    def get_books_by_page(self, page, user_id):
        books_in_page_json, books_in_page = [], []

        books_sorted = self.sort_books_by_average_rate(
            book for book in self.books.values() \
                if not book.hidden and book.id not in self.users[user_id].reported_books)
        
        books_in_page = books_sorted[self.page_size * page: self.page_size + self.page_size * page]
        for book in books_in_page:
            books_in_page_json.append(self.book_to_json(book, user_id))

        return jsonify(books_in_page_json), 200        

    
    # TODO : receive also the user id
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

    def get_books_available_in_library(self, lib_id: int, user_id: int):
        books = []
        for book_id in self.libraries[lib_id].available_books:
            if ( 
                not self.books[book_id].hidden and \
                self.books[book_id].id not in self.users[user_id].reported_libraries
            ):
                books.append(self.books[book_id])
        books_sorted = self.sort_books_by_average_rate(books)
        
        available_books = []
        for book in books_sorted:
            book_json = self.book_to_json(book, user_id)
            del book_json["cover"]
            available_books.append(book_json)
            print(book.id)

        return jsonify(available_books), 200

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
    def filter_books_by_title(self, filter_title, user_id):
        books = list(self.books.values())
        sorted_filtered_books = list(filter(lambda book: book.title.upper().find(filter_title.upper()) != -1, books))
        filtered_books_json = []
        for book in sorted_filtered_books:
            filtered_books_json.append(self.book_to_json(book, user_id))
        return jsonify(filtered_books_json), 200
    
    # Filter book by title by page
    def filter_books_by_title_by_page(self, filter_title, page, user_id):
        filtered_books_json = []
        
        books = self.sort_books_by_average_rate(
            book for book in self.books.values() \
                if not book.hidden and book.id not in self.users[user_id].reported_books)

        sorted_filtered_books = list(filter(lambda book: book.title.upper().find(filter_title.upper()) != -1, books))
        books_in_page = sorted_filtered_books[self.page_size * page: self.page_size + self.page_size * page]

        for book in books_in_page:
            filtered_books_json.append(self.book_to_json(book, user_id))

        return jsonify(filtered_books_json), 200
    
    # Rate a book
    def rate_book(self, book_id: int, stars: int, user_id: int):
        user = self.users[user_id]
        book = self.books[book_id]

        # if user already rated the book, remove that rate
        if user.already_rated_book(book_id):
            book.remove_rate(user.get_book_rate(book_id))
        # give new rate
        user.give_rate(book_id, stars)
        book.add_rate(stars)

        return jsonify({"rates": book.ratings}), 200
    
    # Report book
    def report_book(self, book_id: int, user_id: int):
        self.users[user_id].report_book(book_id)
        self.books[book_id].add_report()
        return jsonify({}), 200
    
    # Sort books by average rating
    def sort_books_by_average_rate(self, books):
        return sorted(books, key=lambda book: book.get_average_rate(), reverse=True)
 

    # Add user to book notifications
    def add_user_book_notif(self, user_id, book_id):
        self.books[book_id].add_user_to_notify(user_id)
        return jsonify({}), 200

    # Remove user from book notifications
    def remove_user_book_notif(self, user_id, book_id):
        self.books[book_id].remove_user_to_notify(user_id)
        return jsonify({}), 200

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
    
    def get_book_by_id(self, book_id):
        return json.dumps(self.books[book_id], default=vars)
    
    def get_user_by_id(self, user_id):
        return json.dumps(self.users[user_id], default=vars)
    
    def get_users(self):
        return json.dumps(list(self.users.values()), default=vars)
    
    
    def populate(self):

        # Add libraries
        l0 = Library(0, "Library 0", (38.931680, -9.256503), "Malveira 0", f'./images/populate/library0.jpg')
        l1 = Library(1, "Library 1", (38.931680, -9.257503), "Malveira 1", f'./images/populate/library1.jpg')
        l2 = Library(2, "Library 2", (37.42190712476596, -122.08428114652634), 
                     "Near my location", f'./images/populate/library2.jpg')
        l3 = Library(3, "Library 3", (37.42186052743562, -122.08372090011834),
                     "Near my location", f'./images/populate/library3.jpg')

        self.libraries[0] = l0
        self.libraries[1] = l1
        self.libraries[2] = l2
        self.libraries[3] = l3
        self.id_library_counter = 4

        # Add books
        b0 = Book(0, "Book 0", f'./images/populate/book0.jpg', "book_0", 0)
        b1 = Book(1, "Book 1", f'./images/populate/book1.jpg', "book_1", 1)
        b2 = Book(2, "Book 2", f'./images/populate/book0.jpg', "book_2", 2)
        b3 = Book(3, "Book 3", f'./images/populate/book0.jpg', "book_3", 3)
        b4 = Book(4, "Book 4", f'./images/populate/book0.jpg', "book_4", 4)
        b5 = Book(5, "Book 5", f'./images/populate/book0.jpg', "book_5", 5)
        b6 = Book(6, "Book 6", f'./images/populate/book0.jpg', "book_6", 6)
        b7 = Book(7, "Book 7", f'./images/populate/book0.jpg', "book_7", 7)
        b8 = Book(8, "Book 8", f'./images/populate/book0.jpg', "book_8", 8)
        b9 = Book(9, "Book 9", f'./images/populate/book0.jpg', "book_9", 9)
        b10 = Book(10, "Book 10", f'./images/populate/book0.jpg', "book_10", 10)
        self.books[0] = b0
        self.books[1] = b1
        self.books[2] = b2
        self.books[3] = b3
        self.books[4] = b4
        self.books[5] = b5
        self.books[6] = b6
        self.books[7] = b7
        self.books[8] = b8
        self.books[9] = b9
        self.books[10] = b10

        self.id_books_counter = 10
        
        l0.add_book(0)
        l1.add_book(1)
        l1.add_book(2)
        l1.add_book(3)
        l1.add_book(4)
        l1.add_book(5)
        l1.add_book(6)
        l1.add_book(7)
        l1.add_book(8)
        l1.add_book(9)
        l1.add_book(10)
        l2.add_book(0)
        l2.add_book(1)
        l3.add_book(0)



