import json
import sys
import os.path
from flask import jsonify

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
        id = self.id_library_counter
        self.id_library_counter += 1

        photo_filename = 'library_{id}_photo.jpg'

        print(name)
        print(location)
        print(address)

        with open(photo_filename, 'wb') as photo_file:
            photo_file.write(photo)

        self.libraries[id] = Library(id, name, location, photo_filename, address)
        print("ID")
        print(id)
        return jsonify({"id": id}), 200
    
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



    # User add new book to a library
    def add_new_book(self, title, photo, barcode, lib_id):
        id = len(self.books)
        self.books[id] = Book(id, title, photo, barcode, lib_id)
        self.libraries[lib_id].add_book(id)
        return json.dumps({"status": 200})

    # User checkin book at a library
    def checkin_book(self, book_id, lib_id, user_id):        
        self.users[user_id].checkin_book(book_id)
        self.libraries[lib_id].remove_book(book_id)
        return json.dumps({"status": 200})

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


