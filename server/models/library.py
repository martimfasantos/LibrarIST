class Library:

    def __init__(self, id, name, location, photo):
        self.id = id
        self.name = name
        self.location = location
        self.photo = photo
        self.availableBooks = []
        

    def add_book(self, book_id):
        if (book_id not in self.availableBooks):
            self.availableBooks.append(book_id)

    def remove_book(self, book_id):
        if (book_id in self.availableBooks):
            self.availableBooks.remove(book_id)