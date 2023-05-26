class Book:

    def __init__(self, id, title, cover, barcode, lib_id):
        self.id = id
        self.title = title
        self.cover = cover # path to cover image
        self.barcode = barcode
        self.lib_id = lib_id
        self.users_to_notify = []
        

    def add_user_to_notify(self, user_id):
        if (user_id not in self.users_to_notify):
            self.users_to_notify.append(user_id)

    def remove_user_to_notify(self, user_id):
        if (user_id in self.users_to_notify):
            self.users_to_notify.remove(user_id)
    
    def is_user_to_notify(self, user_id):
        return user_id in self.users_to_notify
