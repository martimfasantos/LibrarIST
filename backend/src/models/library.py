class Library:

    def __init__(self, id, name, location, photo, address):
        self.id = id
        self.name = name
        self.location = location
        self.address = address
        self.photo = photo               # this is the photo filename not the file itself
        self.available_books = []         # ids of the books that are in the library
        self.favorite_users = []          # ids of the users that favorited the library

    def add_book(self, book_id):
        if (book_id not in self.available_books):
            self.available_books.append(book_id)

    def remove_book(self, book_id):
        if (book_id in self.available_books):
            self.available_books.remove(book_id)

    def add_favorite_user(self, user_id):
        if (user_id not in self.favorite_users):
            self.favorite_users.append(user_id)
    
    def remove_favorite_user(self, user_id):
        if (user_id in self.favorite_users):
            self.favorite_users.remove(user_id)
    
    def is_user_favorite(self, user_id):
        return user_id in self.favorite_users
    
    def is_book_available(self, book_id):
        return book_id in self.available_books
