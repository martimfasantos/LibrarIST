class Library:

    def __init__(self, id, name, location, photo, address):
        self.id = id
        self.name = name
        self.location = location
        self.photo = photo               # this is the photo filename not the file itself
        self.availableBooks = []         # ids of the books that are in the library
        self.address = address

    def add_book(self, book_id):
        if (book_id not in self.availableBooks):
            self.availableBooks.append(book_id)

    def remove_book(self, book_id):
        if (book_id in self.availableBooks):
            self.availableBooks.remove(book_id)