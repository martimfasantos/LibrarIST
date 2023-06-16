import base64
import json
import time
from server import Server
from flask import Flask, request, jsonify
from flask_sock import Sock
from flask_cors import CORS
from gevent import monkey
from gevent.pywsgi import WSGIServer

latest_book_title = None 
latest_library = None
websocket_connections = []
interested_connections = []


app = Flask(__name__)
server = Server()
sockets = Sock(app)
cors = CORS(app)
app.debug = True


@app.route("/", methods=["GET"])
def handle_call():
    return jsonify({}), 200


# Create guest user
# body: none
@app.route("/users/guest/create", methods=['POST'])
def create_guest_user():
    return server.create_guest_user()

# Validate user
# path:
#   - userId: int
@app.route("/users/validate", methods=['GET'])
def validate_user():
    return server.validate_user(int(request.args.get("userId")))

# Create user
# body:
#   - username: string
#   - passwords: string
@app.route("/users/register", methods=['POST'])
def create_user():
    request_data = request.json
    username = request_data["username"]
    password = request_data["password"]
    return server.create_new_user(int(request.args.get("userId")), username, password)

# Login user
# body:
#   - username: string
#   - passwords: string
@app.route("/users/login", methods=['POST'])
def login_user():
    return server.login_user(request.args.get("username"), request.args.get("password"))


# List libraries info for markers in a given radius
# path:
#   - lat: float - latitude
#   - lon: float - longitude
#   - radius: int - radius in kilometers
#   - userId: int
@app.route("/libraries/markers", methods=['GET'])
def get_libraries_markers():

    global latest_book_title, interested_connections, websocket_connections, latest_library

    print("Current websockets: ", websocket_connections)
    print("Current interested connections before adding: ", interested_connections)



    # for connection in websocket_connections:
    #     interested_connections.append(connection)

    for user  in server.users.values():
        if(user.sockets != []):
            print("User sockets not null:", user.sockets)
            for socket in user.sockets:
                if(socket not in interested_connections):
                    interested_connections.append(socket)

    print("Current interested connections: ", interested_connections)

    latest_book_title = "Pachinko"
    latest_library = "IST-Central"


    return server.get_libraries_markers(float(request.args.get("lat")), float(request.args.get("lon")),
                                        int(request.args.get("radius")), int(request.args.get("userId")))

# List libraries in a given radius
# path:
#   - lat: float - latitude
#   - lon: float - longitude
#   - radius: int - radius in kilometers
#   - userId: int
@app.route("/libraries/load", methods=['GET'])
def get_libraries_to_load_cache():
    return server.get_libraries_to_load_cache(float(request.args.get("lat")), float(request.args.get("lon")),
                                        int(request.args.get("radius")), int(request.args.get("userId")))


# Create library
# body:
#   - name: string
#   - address: string
#   - location: (int, int)
#   - location: string
#   - photo: image
@app.route("/libraries/create", methods=['POST'])
def create_library():
    request_data = request.json
    name = request_data["name"]
    address = request_data["address"]
    location = (request_data["latitude"], request_data["longitude"])
    photo = base64.b64decode(request_data["photo"])
    return server.create_new_library(name, location, photo, address)


# Get library by id
# query:
#   - libId: int
#   - userId: int
@app.route("/libraries/get", methods=['GET'])
def get_library():
    return server.get_library(int(request.args.get("libId")), int(request.args.get("userId")))


# Add library to user's favorites
# path:
#   - userId: int - user to which add the library
#   - libId: int - library to add
@app.route("/libraries/<int:lib_id>/add_fav", methods=['POST'])
def add_favorite_library_to_user(lib_id):
    return server.add_favorite_lib(lib_id, int(request.args.get("userId")))


# Remove library from user's favorites
# path:
#   - userId: int - user from which to remove library
#   - libId: int - library to remove
@app.route("/libraries/<int:lib_id>/remove_fav", methods=['POST'])
def remove_favorite_library_from_user(lib_id):
    return server.remove_favorite_lib(lib_id, int(request.args.get("userId")))


# Report library
# path:
#   - libId: int - library to report
# query:
#   - userId: int - user reporting the library
@app.route("/libraries/<int:lib_id>/report", methods=['POST'])
def report_library(lib_id):
    return server.report_library(lib_id, int(request.args.get("userId")))


# User checkin book
# path:
#   - userId: int - user checking in the book
# body:
#   - bookId: int - book being checkin
#   - libId: int - library from which the book will be checked in
@app.route("/libraries/<int:lib_id>/books/checkin", methods=['POST'])
def check_in_book(lib_id):
    request_data = request.json
    barcode = request_data["barcode"]
    lib_id = request_data["libId"]

    #Broadcast New Book Message!
    global interested_connections, latest_book_title, latest_library

    bookId = server.get_book_id_from_barcode(barcode)
    book = server.books.get(bookId)
    library = server.libraries.get(lib_id)

    if book.hidden == False:
        for user_id in book.users_to_notify:
            user = server.users.get(user_id)
            if(bookId not in user.reported_books):
                for socket in user.sockets:
                    if(socket not in interested_connections):
                        interested_connections.append(socket)

        latest_book_title = book.title
        latest_library = library.name


    return server.check_in_book(barcode, lib_id)

# TODO : receive also the user id


# User checkin new book
# path:
#   - userId: int - user checking in the book
# body:
#   - bookId: int - book being checkin
#   - libId: int - library from which the book will be checked in
@app.route("/libraries/<int:lib_id>/books/checkin/newbook", methods=['POST'])
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
@app.route("/libraries/<int:lib_id>/books/checkout", methods=['POST'])
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


# Rate book
# path:
#   - book_id: int - book being rated
# query:
#   - stars: int - rate given to the book in stars (1-5)   
#   - userId: int - user giving the rate
@app.route("/books/<int:book_id>/rate", methods=["POST"])
def rate_book(book_id):
    request_data = request.json
    rating = request_data["rating"]
    return server.rate_book(book_id, rating, int(request.args.get("userId")))


# Report book
# path:
#   - book_id: int - book being reported
# query:
#   - userId: int - user reporting the book
@app.route("/books/<int:book_id>/report", methods=["POST"])
def report_book(book_id):
    return server.report_book(book_id, int(request.args.get("userId")))


# Find a book with a given barcode in a given library
# body:
#   - barcode: string
@app.route("/libraries/<int:lib_id>/books/find", methods=['GET'])
def find_book_from_library(lib_id):
    return server.get_book_from_library(request.args.get("barcode"), lib_id)


# Get a book with a given id
# body:
#   - book_id: int
@app.route("/books/get", methods=['GET'])
def get_book():
    return server.get_book(int(request.args.get("bookId")), int(request.args.get("userId")))


# Get all the books
# body: none
@app.route("/books", methods=['GET'])
def get_all_books():
    return server.get_all_books(int(request.args.get("userId")))


# Get books by page
# path:
#   - page: int
# query:
#   - userId: int
@app.route("/books/pages/<int:page>", methods=['GET'])
def get_books_by_page(page):
    return server.get_books_by_page(page, int(request.args.get("userId")))


# Get books for a given library
# query:
#   - libraryId: int
@app.route("/libraries/<int:lib_id>/books", methods=['GET'])
def list_all_books_from_library(lib_id):
    return server.list_all_books_from_library(lib_id, int(request.args.get("userId")))


# Get libraries with given book available
# query:
#   - bookId: int
@app.route("/books/<int:book_id>/libraries", methods=['GET'])
def get_libraries_with_book(book_id):
    return server.get_libraries_with_book(book_id, int(request.args.get("userId")))


# Add user to book notifications
# path:
#   - bookId: int - notifications for this book
# body:
#   - userId: int - user to be notified
@app.route("/books/<int:book_id>/notifications/on", methods=['POST'])
def add_user_to_book_notifications(book_id):
    return server.add_user_book_notif(book_id, int(request.args.get("userId")))


# Remove user from book notifications
# path:
#   - bookId: int - notifications for this book
#   - userId: int - user to be removed from notifications
@app.route("/books/<int:book_id>/notifications/off", methods=['POST'])
def remove_user_from_book_notifications(book_id):
    return server.remove_user_book_notif(book_id, int(request.args.get("userId")))


# Get books filtered by title
# query:
#   - title: string - text from which to filter
#   - userId: int - user to which the books will be filtered
@app.route("/books/filter", methods=['GET'])
def filter_books_by_title():
    return server.filter_books_by_title(request.args.get("title"), int(request.args.get("userId")))


# Get books filtered by title in page
# query:
#   - title: string - text from which to filter
#   - page: int - number of page to get the books
#   - userId: int - user to which the books will be filtered
@app.route("/books/filter/pages", methods=['GET'])
def filter_books_by_title_by_page():
    return server.filter_books_by_title_by_page(request.args.get("title"), 
                                        int(request.args.get("page")),
                                        int(request.args.get("userId")))


    
##################   SOCKETS  ##################


# @sockets.route('/ws/<int:user_id>/<string:user_password>')
# def ws(ws, user_id, user_password):

@sockets.route('/ws/<int:user_id>')
def ws(ws, user_id):  
  print("Tried to connect to socket")
  global websocket_connections, latest_book_title, interested_connections, latest_library

  for user in server.users.values():
        if user.id == user_id: # and user.password == "":
            print("Entered loop")
            user.add_socket(ws)
            print("User sockets??", user.sockets)
        # if user.id == user_id and user.password == user_password:
        #     user.add_socket(ws)

  if(ws not in websocket_connections):
    websocket_connections.append(ws)

  while True:
    try:
        if latest_book_title != None and latest_library != None:
            for connection in websocket_connections:
                if connection in interested_connections:
                    try:
                        connection.send(json.dumps({"title": latest_book_title, "library_name": latest_library}))
                    except Exception as e:
                        print(f"Error sending notification to this socket: {str(e)}")
                        print(f"Trying next socket....")
                        continue  # Skip to the next iteration if the connection is closed


            latest_book_title = None
            latest_library = None
            interested_connections = []

    except Exception as e:
        print(f"Error - Could not send notifications to any connection: {str(e)}")
    
    time.sleep(10)


if __name__ == "__main__":
     #app.run(host="0.0.0.0", port=5000)
    monkey.patch_all()
        
    # SSL context for the certificate
    ssl_context = ('/etc/letsencrypt/live/gp-cmov2-cmu-project-1.vps.tecnico.ulisboa.pt/fullchain.pem',
                   '/etc/letsencrypt/live/gp-cmov2-cmu-project-1.vps.tecnico.ulisboa.pt/privkey.pem')

    # Start the server with SSL enabled
    http_server = WSGIServer(('0.0.0.0', 5000), app, keyfile=ssl_context[1],
                            certfile=ssl_context[0])
    http_server.serve_forever()

    #WSGIServer(('0.0.0.0', 5000), app).serve_forever()
    # http_server = WSGIServer(('0.0.0.0', 5000), app, handler_class=WebSocketHandler)
    # http_server.serve_forever()