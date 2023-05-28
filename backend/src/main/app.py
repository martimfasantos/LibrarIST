import json
import base64
from server import Server
from flask import Flask
from flask import request

app = Flask(__name__)
server = Server()

app.debug = True


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
@app.route("/<int:lib_id>/books/checkin", methods=['POST'])
def check_in_book(lib_id):
    request_data = request.json
    barcode = request_data["barcode"]
    lib_id = request_data["libId"]
    return server.check_in_book(barcode, lib_id)


# User checkin new book
# path:
#   - userId: int - user checking in the book
# body:
#   - bookId: int - book being checkin
#   - libId: int - library from which the book will be checked in
@app.route("/<int:lib_id>/books/checkin/newbook", methods=['POST'])
def check_in_new_book(lib_id):
    request_data = request.json
    title = request_data["title"]
    cover = base64.b64decode(request_data["cover"])
    barcode = request_data["barcode"]
    lib_id = request_data["libId"]
    return server.check_in_new_book(title, cover, barcode, lib_id)


# User checkin book
# path:
#   - userId: int - user checking in the book
# body:
#   - bookId: int - book being checkin
#   - libId: int - library from which the book will be checked in
@app.route("/library/<int:lib_id>/books/checkout", methods=['DELETE'])
def check_out_book(lib_id):
    request_data = request.json
    barcode = request_data["barcode"]
    return server.check_out_book(barcode, lib_id)


# Find a book with a given barcode
# body:
#   - barcode: string
@app.route("/books/find", methods=['GET'])
def find_book():
    return server.find_book(request.args.get("barcode"))


# Find a book with a given barcode in a given library
# body:
#   - barcode: string
@app.route("/library/<int:lib_id>/books/find", methods=['GET'])
def find_book_from_library(lib_id):
    return server.get_book_from_library(request.args.get("barcode"), lib_id)


# Get a book with a given id
# body:
#   - book_id: int
@app.route("/books/get", methods=['GET'])
def get_book():
    return server.get_book(request.args.get("bookId"))


# Get all the books
# body: none
@app.route("/books", methods=['GET'])
def get_all_books():
    return server.get_all_books()


# Create library
# body:
#   - name: string
#   - address: string
#   - location: (int, int)
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


# Get books for a given library
# query:
#   - libraryId: int
@app.route("/libraries/<int:lib_id>/books", methods=['GET'])
def list_all_books_from_library(library_id=None):
    request_data = request.json
    library_id = request_data("libraryId")
    return server.list_all_books_from_library(library_id)


# Get libraries with given book available
# query:
#   - bookId: int
@app.route("/books/<int:book_id>/libraries", methods=['GET'])
def get_libraries_with_book(book_id):
    return server.get_libraries_with_book(book_id)


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