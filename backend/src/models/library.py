from typing import Any


class Library:

    def __init__(self, id, name, location, photo, address):
        self.id = id
        self.name = name
        self.location = location
        self.photo = photo               # this is the photo filename not the file itself
        self.available_books = []         # ids of the books that are in the library
        self.address = address

    def add_book(self, book_id):
        if (book_id not in self.available_books):
            self.available_books.append(book_id)

    def remove_book(self, book_id):
        if (book_id in self.available_books):
            self.available_books.remove(book_id)
