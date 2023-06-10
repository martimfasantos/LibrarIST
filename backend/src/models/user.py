class User:

    def __init__(self, id, username, password):
        self.id = id
        self.username = username
        self.password = password
        self.current_books = []
        self.favorite_libraries = []
        self.book_ratings = {}


    def checkin_book(self, book_id):
        if (book_id not in self.current_books):
            self.current_books.append(book_id)

    def return_book(self, book_id):
        if (book_id in self.current_books):
            self.current_books.remove(book_id)

    def add_library(self, lib_id):
        if (lib_id not in self.favorite_libraries):
            self.favorite_libraries.append(lib_id)

    def remove_library(self, lib_id):
        if (lib_id in self.favorite_libraries):
            self.favorite_libraries.remove(lib_id)

    def give_rate(self, book_id, stars):
        self.book_ratings[book_id] = stars

    def already_rated_book(self, book_id):
        return book_id in self.book_ratings.keys()
    
    def get_book_rate(self, book_id):
        return self.book_ratings[book_id]

