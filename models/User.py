class User:

    def __init__(self, id, username, password):
        self.id = id
        self.username = username
        self.password = password
        self.currentBooks = []
        self.favoriteLibs = []


    def checkin_book(self, book_id):
        if (book_id not in self.currentBooks):
            self.currentBooks.append(book_id)

    def return_book(self, book_id):
        if (book_id in self.currentBooks):
            self.currentBooks.remove(book_id)

    def add_library(self, lib_id):
        if (lib_id not in self.favoriteLibs):
            self.favoriteLibs.append(lib_id)

    def remove_library(self, lib_id):
        if (lib_id in self.favoriteLibs):
            self.favoriteLibs.remove(lib_id)


