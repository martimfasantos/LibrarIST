class Book:

    def __init__(self, id, title, photo, barcode, lib_id):
        self.id = id
        self.title = title
        self.photo = photo
        self.barcode = barcode
        self.usersToNotify = []
        

    def add_user_to_notify(self, user_id):
        if (user_id not in self.usersToNotify):
            self.usersToNotify.append(user_id)

    def remove_user_to_notify(self, user_id):
        if (user_id in self.usersToNotify):
            self.usersToNotify.remove(user_id)
