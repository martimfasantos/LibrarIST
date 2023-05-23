import json
import base64
from server import Server
from flask import Flask
from flask import request

app = Flask(__name__)
server = Server()

bookId = 1
libraryId = 1

@app.route("/", methods=["GET"])
def handle_call():
    return json.dumps({"status": 200})


# Create user
# body:
#   - username: string
#   - passwords: string
@app.route("/users", methods=['POST'])
def create_user():
    request_data = request.json
    username = request_data["username"]
    password = request_data["password"]
    return server.add_new_user(username, password)


# Add library to user's favorites
# path:
#   - userId: int - user to which add the library
# body:
#   - libId: int - library to add
@app.route("/users/<userId>/libraries", methods=['POST'])
def add_favorite_library_to_user(userId=None):
    request_data = request.json
    lib_id = request_data["libId"]
    return server.add_favorite_lib(lib_id, userId)


# Remove library from user's favorites
# path:
#   - userId: int - user from which to remove library
#   - libId: int - library to remove
@app.route("/users/<userId>/libraries/<libId>", methods=['DELETE'])
def remove_favorite_library_from_user(userId=None, libId=None):
    return server.remove_favorite_lib(libId, userId)


# User checkin book
# path:
#   - userId: int - user checking in the book
# body:
#   - bookId: int - book being checkin
#   - libId: int - library from which the book will be checked in
@app.route("/users/<userId>/books/checkin", methods=['POST'])
def checkin_book(userId=None):
    request_data = request.json
    book_id = request_data["bookId"]
    lib_id = request_data["libId"]
    return server.checkin_book(book_id, lib_id, userId)


# User returns book
# path:
#   - userId: int - user returning the book
# body:
#   - bookId: int - book being returned
#   - libId: int - library to which the book will be returned
@app.route("/users/<userId>/books/return", methods=['POST'])
def return_book(userId=None):
    request_data = request.json
    book_id = request_data["bookId"]
    lib_id = request_data["libId"]
    return server.return_book(book_id, lib_id, userId)



# Create library
# body:
#   - name: string
#   - location: string
#   - photo: image
@app.route("/libraries", methods=['POST'])
def create_library():
    request_data = request.json
    name = request_data["name"]
    address = request_data["address"]
    location = (request_data["latitude"], request_data["longitude"])
    photo = base64.b64decode(request_data["photo"])
    return server.add_new_library(name, location, photo, address)


# Get libraries with given book available
# query:
#   - bookId: int
@app.route("/libraries/books/filter", methods=['GET'])
def get_libraries_with_book():
    book_id = request.args.get("bookId")
    return server.filter_libraries_with_book(book_id)



# Create book
# body:
#   - title: string
#   - photo: image
#   - barcode: string
#   - lib_id: int - library in which the book was added
@app.route("/books", methods=['POST'])
def create_book():
    request_data = request.json
    title = request_data["title"]
    photo = request_data["photo"]
    barcode = request_data["barcode"]
    lib_id = request_data["libId"]
    return server.add_new_book(title, photo, barcode, lib_id)


# Add user to book notifications
# path:
#   - bookId: int - notifications for this book
# body:
#   - userId: int - user to be notified
@app.route("/books/<bookId>/notifications/users", methods=['POST'])
def add_user_to_book_notifications(bookId = None):
    request_data = request.json
    user_id = request_data["userId"]
    return server.add_user_book_notif(user_id, bookId)


# Remove user from book notifications
# path:
#   - bookId: int - notifications for this book
#   - userId: int - user to be removed from notifications
@app.route("/books/<bookId>/notifications/users/<userId>", methods=['DELETE'])
def remove_user_from_book_notifications(bookId=None, userId=None):
    return server.remove_user_book_notif(userId, bookId)


# Get books filtered by title
# query:
#   - title: string - text from which to filter
@app.route("/books/title/filter", methods=['GET'])
def filter_books_by_title():
    title = request.args.get("title")
    return server.filter_books_by_title(title)

    
    
if __name__ == "__main__":
     app.run(host="0.0.0.0", port=5000)