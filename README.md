# Advanced Programming Project

## Background

This project was developed as part of the Advanced Programming course.

The project implements a local computational graph system based on the Publisher-Subscriber design pattern.
The system allows the user to load a configuration file, create a computational graph, display it visually in the browser, send messages to specific topics, and view the updated values of the topics.

The project includes a custom HTTP server, servlets, dynamic HTML generation, configuration loading, topic management, and a graphical view layer.

## Installation

1. Clone the repository:

```bash
git clone https://github.com/Shabtai80/Advanced-Programming-Project.git
```

2. Open the project folder in VS Code, IntelliJ, or any Java IDE.

3. Make sure Java is installed:

```bash
java -version
javac -version
```

## Run Commands

From the project root folder, compile the project:

```bash
javac -d bin src/Main.java src/**/*.java
```

Run the server:

```bash
java -cp bin Main
```

After the server starts, open the following address in the browser:

```text
http://localhost:8080/app/index.html
```

## How to Use

1. Open the web page in the browser.
2. Choose a configuration file from the `files_config` folder.
3. Click `deploy`.
4. The computational graph will be displayed in the center of the page.
5. Send a message to a specific topic using the publish form.
6. The current topic values will be displayed and updated in the topic table.
7. The graph can be refreshed to show the updated topic values.

## Project Structure

```text
src/
├── graph/
├── configs/
├── server/
├── servlets/
└── views/

html_files/
├── index.html
├── form.html
├── graph.html
└── temp.html

files_config/
├── conf.simple
└── conf.complex
```

## Main Features

* Custom lightweight HTTP server
* Servlet-based request handling
* Publisher-Subscriber communication model
* Dynamic configuration loading
* Computational graph creation
* HTML-based graph visualization
* Topic value table
* Graph refresh functionality
* Separation between model, controller, and view logic

## Demo Video

Demo video link:

```text
PASTE_DEMO_VIDEO_LINK_HERE
```

The demo video includes:

* Course and submitter details
* Project background
* Project design
* Live demonstration of the main features
* Loading a configuration file
* Displaying the computational graph
* Publishing messages to topics
* Updating and refreshing topic values
* Summary of what was learned during the course

## Authors

Submitter 1:

```text
Name: Eliezer Shabtai
ID: 206523516
Email: shabtaieli273@gmail.com
```



