class Book:

    def __init__(self, id, title, cover, barcode, lib_id):
        self.id = id
        self.title = title
        self.cover = cover # path to cover image
        self.barcode = barcode
        self.lib_id = lib_id
        self.users_to_notify = []
        self.ratings = [0, 0, 0, 0, 0]
        self.nr_reports = 0
        self.hidden = False
    

    def add_user_to_notify(self, user_id):
        if (user_id not in self.users_to_notify):
            self.users_to_notify.append(user_id)

    def remove_user_to_notify(self, user_id):
        if (user_id in self.users_to_notify):
            self.users_to_notify.remove(user_id)
    
    def is_user_to_notify(self, user_id):
        return user_id in self.users_to_notify
    
    def add_rate(self, stars):
        self.ratings[stars-1] += 1

    def remove_rate(self, stars):
        self.ratings[stars-1] -= 1

    def get_average_rate(self):
        sum_ratings = 0
        for star in range(len(self.ratings)):
            sum_ratings += (star+1) * self.ratings[star]
        return sum_ratings / sum(self.ratings) if sum(self.ratings) > 0 else 0
    
    def add_report(self):
        self.nr_reports += 1
        if self.nr_reports >= 2:
            self.hidden = True
        print("BOOK " + str(self.id) + " REPORTS: " + str(self.nr_reports))
        