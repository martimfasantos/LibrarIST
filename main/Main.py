import json
from Server import Server

def main():
    server = Server()
    server.add_new_library("lib", "location", "photo")
    server.add_new_library("lib1", "location1", "photo1")
    
    server.add_new_book("title", "photo", "barcode", 0)
    print(server.get_library_by_id(0))
    print(server.get_book_by_id(0))
    print(server.filter_libraries_with_book(0))
    print(server.filter_books_by_title("ti"))

    server.add_new_user("username", "password")
    server.add_favorite_lib(0, 0)
    print(server.get_user_by_id(0))
    server.remove_favorite_lib(0, 0)
    print(server.get_user_by_id(0))

    server.checkin_book(0, 0, 0)
    print(server.filter_libraries_with_book(0))
    server.return_book(0, 0, 0)
    print(server.filter_libraries_with_book(0))

    server.add_user_book_notif(0, 0)
    print(server.get_books())

    
    
if __name__ == "__main__":
    main()