Put the MySQL Connector/J jar here, for example:  mysql-connector-j-8.0.33.jar
Any 8.0.x (or newer) release works.

Download: https://dev.mysql.com/downloads/connector/j/  -> choose "Platform Independent" -> ZIP.
Unzip it and copy only the .jar file into this lib folder.
Or directly from Maven Central:
https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar

VS Code finds it automatically (.vscode/settings.json has "lib/*.jar").
Without it the game still runs but says "The MySQL driver is missing" when you try to save or load.
