# LibrarIST

LibrarIST is a mobile application developed for the Android operating system as part of the Mobile and Ubiquitous Computing master's course @ IST. It serves as a platform to support free libraries, similar to the Little Free Library initiative, by providing users with a mobile interface to find, explore, and manage these libraries.

Authors | Github
--------|--------
Martim Santos   | https://github.com/martimfasantos
Marina Gomes    | https://github.com/marinagomes02
João Bagorro    | https://github.com/Bag0rr0

**Project Grade:** 23 / 20

---

## Overview
In this project, we have developed a non-trivial mobile application for the Android Operating System called LibrarIST. This application explores various key aspects of mobile development, including location awareness, resource management, and social behavior. LibrarIST aims to provide mobile support for free libraries, similar to the ones promoted by the Little Free Library initiative. It helps users find free libraries in their surroundings, discover the books they contain, and manage library inventories.

---


## Mandatory Features
The LibrarIST app includes the following core library management functionalities:

### Map Screen
- The map can be dragged around to show more libraries.
- There is a search bar to look up and center the map on a given address.
- The user can center the map on their current location with the press of a button.
- Free libraries are displayed on the map with markers.
- The user's favorite libraries are highlighted with a different marker.
- Tapping a marker takes the user to the respective library's information panel, which includes:
  - The library's name, location (shown on a map), and photo.
  - A button to help the user navigate to the library.
  - A button to add/remove the library from the user's favorites.
  - A button to check in/donate a book by scanning the barcode. If the code is unknown, a new book is created with a title and cover photo taken from the camera.
  - A button to check out a book by scanning the barcode.
  - The list of books currently available at the library. Tapping a book opens a panel with more information about the book.


<p align="center">
  <img src="https://i.imgur.com/mPrkyBx.png" width="250" />
  <img src="https://i.imgur.com/q61mzQm.png" width="250" />
  <img src="https://i.imgur.com/rpuYF13.png" width="250" />
</p>

<p align="center">
  <strong>Map Screen with visible libraries &nbsp || &nbsp</strong>
  <strong>Creation of a Library &nbsp || &nbsp </strong>
  <strong>Library's information panel</strong>
</p>


### Book Search Screen
- Users can see the full list of books ever donated to any library managed within the app.
- Books can be filtered using a text search.
- Tapping a book takes the user to a detailed information panel with more information about the book, including:
  - The book's title and cover picture.
  - A button to enable/disable notifications when the book becomes available in one of the user's favorite libraries.
  - A list of libraries where the book is available, sorted by distance and indicating the distance from the user's location. Tapping a library brings up its information panel.


<p align="center">
  <img src="https://i.imgur.com/UQF3qyp.png" width="250" />
  <img src="https://i.imgur.com/coXzJQB.png" width="250" />
  <img src="https://i.imgur.com/4SDWebl.png?1" width="250" />
</p>

<p align="center">
  <strong>Map centered on searched location &nbsp || &nbsp</strong>
  <strong>Book Search Screen &nbsp || &nbsp </strong>
  <strong>Book's information panel</strong>
</p>

---

### Back-end
The LibrarIST project includes a robust back-end service that facilitates explicit data sharing and crowd-sourcing among multiple devices. The back-end service is responsible for holding and processing shared data, such as library and book information, and enables synchronization of the application state across devices. The implementation of the back-end service can be customized according to the developer's choice, whether it is a self-implemented server using technologies like Java RMI, gRPC, or RESTful services, or by utilizing existing server software or databases. While the focus of the class is not on the specific implementation of the back-end service, it should be designed to handle data synchronization efficiently and can store data in memory without the need for persistence.

---

### Resource Frugality
The LibrarIST project addresses the challenges of resource frugality in data-intensive mobile applications. It optimizes the trade-offs between timeliness, data size, and efficiency to provide a seamless user experience while conserving network usage and device resources. The application ensures timely synchronization of library and book information across devices, minimizing data transfer and power consumption. When the user actively interacts with the application, new content is fetched quickly to provide real-time updates. However, when the user is not actively engaged, more efficient messaging techniques are employed to save network resources, even if it leads to increased latency. The application also adopts a strategy to download data related to visible UI elements only, conserving resources by avoiding unnecessary data transmissions. Additionally, the project optimizes network usage by selectively retrieving photos, such as book covers, based on the user's network connection type. Placeholder images are displayed when on a metered connection, and photos are automatically retrieved when the user is on a WiFi network.

---

### Context Awareness
The LibrarIST application incorporates context awareness by leveraging the device's location capabilities. It automatically detects the user's location and, when in proximity to a free library (e.g., within 100 meters), opens the information panel for that specific library. This feature enhances the user experience by providing relevant information and facilitating the seamless discovery of nearby libraries.

---

### Caching
To address the challenges posed by spotty data connections and short-term outages, the LibrarIST project implements a caching mechanism. Content retrieved from the server is stored in a cache, minimizing redundant downloads and improving the application's resilience to network disruptions. The cache optimizes data usage and ensures that recently viewed content remains accessible offline when needed. Additionally, the project includes careful pre-loading of content when the user connects to a WiFi network. This approach maximizes the utilization of WiFi data by loading the most relevant content, such as library data within a 10km radius and their respective books. By doing so, the application enables users to browse a significant set of nearby libraries even when they no longer have access to a WiFi network, minimizing data usage while still providing a comprehensive library browsing experience.

With these features implemented, the LibrarIST project showcases a well-rounded and efficient mobile application that focuses on resource frugality, context awareness, and optimized data synchronization to deliver an exceptional user experience.

---

## Additional Features
In addition to the mandatory features, we have implemented the following additional features to enhance the functionality of LibrarIST:

- **Securing Communication**
  - Implement SSL encryption for secure communication between the mobile application and the back-end server.
  - Protect user privacy and prevent data interception or tampering.

- **Meta Moderation**
  - Allow users to flag fake libraries or books.
  - Hide flagged entities from the flagger's view and eventually from everyone once a threshold is surpassed.
  - Maintain data integrity and ensure a reliable and trustworthy platform.

- **User Ratings**
  - Enable users to rate books with 1-5 stars.
  - Overwrite previous ratings if a user submits a new rating for the same book.
  - Display a histogram of all ratings in the book information panel.
  - Sort book lists based on their average rating.

- **User Accounts**
  - Provide users with the option to create accounts for cross-device synchronization.
  - Allow seamless syncing of favorite libraries and data between multiple devices.
  - Enable login/logout functionality for personalized experiences.
  
- **Localization**
  - Translate static user-facing strings into multiple languages (EN and PT).
  - Support multilingual usage and sharing of data without barriers.

- **UI Adaptability: Rotation**
  - Enable automatic adaptation of LibrarIST's user interface to different device orientations.
  - Ensure optimal viewing and usability in both portrait and landscape modes.

- **UI Adaptability: Light/Dark Theme**
  - Implement both light and dark themes for user interface customization.
  - Provide visual comfort and consistency with the device's overall UI.


### Visual Examples


<p align="center">
  <img src="https://i.imgur.com/AuzySUj.png" width="200" />
  <img src="https://i.imgur.com/EoufESp.png" width="200" />
  <img src="https://i.imgur.com/2EmYBmP.png" width="200" />
  <img src="https://i.imgur.com/JOBfSSK.png" width="200" />
</p>

<p align="center">
  <strong>Rating a book &nbsp || &nbsp</strong>
  <strong>User authentication menu &nbsp || &nbsp </strong>
  <strong>Register User &nbsp || &nbsp</strong>
  <strong>Login User</strong>
</p>

<p align="center">
  <img src="https://i.imgur.com/IF5hPin.png?1" width="400" />
  <img src="https://i.imgur.com/Dk24MIg.png" width="400" />
</p>

<p align="center">
  <strong>Landscape Map Screen &nbsp &nbsp || &nbsp &nbsp</strong>
  <strong>Landscape Book Search Screen</strong>
</p>

By integrating these additional features, LibrarIST aims to provide a comprehensive and customizable library management experience, catering to users' diverse needs and preferences.


---

## Conclusion
In conclusion, our project, LibrarIST, is a robust user-friendly mobile application for Android that enables users to discover, explore, and manage free libraries. With its intuitive interface, powerful functionalities, and emphasis on community engagement, LibrarIST aims to foster a love for reading and support access to knowledge.
