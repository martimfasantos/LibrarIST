import base64
import json
import sys
import os.path
from flask import jsonify
from PIL import Image
from io import BytesIO

models_path = os.path.abspath(os.path.join(os.path.dirname(__file__),'../models'))
sys.path.append(models_path)

from book import Book
from library import Library
from user import User

class Server:

    def __init__(self):
        self.id_library_counter = 0
        self.id_books_counter = 0
        self.books = {}
        self.libraries = {}
        self.users = {}


    # Add new library
    def add_new_library(self, name, location, photo, address):
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
        return jsonify({"libId": lib_id}), 200
    
    # List books from library
    def list_all_books_from_library(self, lib_id):
        return jsonify(self.libraries[lib_id].getavailableBooks)
    
    # Add library to users favorites
    def add_favorite_lib(self, lib_id, user_id):
        self.users[user_id].add_library(lib_id)
        return jsonify({"status": 200})

    # Remove library from users favorites
    def remove_favorite_lib(self, lib_id, user_id):
        self.users[user_id].remove_library(lib_id)
        return json.dumps({"status": 200})

    # Get libraries with the given book available
    def filter_libraries_with_book(self, book_id):
        libraries = []
        for library in list(self.libraries.values()):
            if book_id in library.availableBooks:
                libraries.append(library)
        return json.dumps(libraries, default=vars)



    # Get book id from barcode
    def get_book(self, barcode):
        return jsonify({"bookId": self.get_book_id_from_barcode(barcode)}), 200
    
    def check_in_book(self, barcode, lib_id):
        user_id = 0 # TODO: get user id from token
        book_id = self.get_book_id_from_barcode(barcode)
        self.libraries[lib_id].add_book(book_id)

        cover = self.books[book_id].cover

        with open(cover, "rb") as file:
            cover_data = file.read()

        cover_base64 = base64.b64encode(cover_data).decode("utf-8")

        return jsonify({"bookId": book_id,
                        "title": self.books[book_id].title,
                        "barcode": self.books[book_id].barcode,
                        "cover": cover_base64,
                        "activNotif": self.books[book_id].is_user_to_notify(user_id)}), 200
    
    # User checkin book at a library
    def check_in_new_book(self, title, cover, barcode, lib_id): 
        book_id = self.id_books_counter
        self.id_books_counter += 1

        book_cover = f'./images/books/{book_id}.jpg'

        print(title)
        print(cover)
        print(barcode)
        print(lib_id)

        if cover is not None:
            img = Image.open(BytesIO(cover))
            img.save(book_cover, "JPEG")

        self.books[book_id] = Book(book_id, title, book_cover, barcode, lib_id)
        self.libraries[lib_id].add_book(book_id)

        print(self.libraries[lib_id].availableBooks)

        return jsonify({"bookId": book_id}), 200      

    # User checkin book at a library
    def check_out_book(self, barcode, lib_id):

        # find book id for the given barcode
        book_id = None
        for book in self.books.values():
            if book.barcode == barcode:
                book_id = book.id
                break

        # if book not found return 404
        if book_id is None:
            return 404
        
        # remove book from books and library
        self.books.pop(book_id)
        self.libraries[lib_id].remove_book(book_id)

        # remove book cover
        book_cover = f'./images/books/{book_id}.jpg'
        if os.path.exists(book_cover):
            os.remove(book_cover)

        print(book_id)
        print(lib_id)
        print(self.libraries[lib_id].availableBooks)
        return jsonify({"bookId": book_id}), 200

    # User return book at library
    def return_book(self, book_id, lib_id, user_id):
        self.users[user_id].return_book(book_id)
        self.libraries[lib_id].add_book(book_id)
        self.notify_users(book_id, lib_id)
        return json.dumps({"status": 200})

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
    
    def get_books(self):
        return json.dumps(list(self.books.values()), default=vars)
    
    def get_users(self):
        return json.dumps(list(self.users.values()), default=vars)


