class User:

    def __init__(self, id, username, password):
        self.id = id
        self.username = username
        self.password = password
        self.favorite_libraries = []
        self.book_ratings = {}
        self.reported_libraries = []
        self.reported_books = []


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
    
    def report_library(self, lib_id):
        self.reported_libraries.append(lib_id)
        print("USER " + str(self.id) + " REPORTED LIB: " + str(lib_id))

    def report_book(self, book_id):
        self.reported_books.append(book_id)
        print("USER " + str(self.id) + " REPORTED BOOK: " + str(book_id))

