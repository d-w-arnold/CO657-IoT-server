# CO657 IoT Server

A Central Server for my IoT system, to work with my <a href="https://github.com/d-w-arnold/bluetooth-IoT-device/" target="_blank">IoT Device</a>.

Receives input from my IoT device over Serial, detailing the BLE MAC address of a BLE device just come home.

Pulls BLE device information from a database, storing the information locally and periodically checking for updates in the database.

BLE device information (stored in a database) can be created, viewed, updated, and deleted all from my <a href="https://github.com/d-w-arnold/CO657-IoT-admin-web-app/" target="_blank">IoT Admin Web App</a>.

When input is received over Serial from the IoT device, the IoT server toggles the living room smart home light (according to the information for the given BLE device obtained from the database).

---

Libraries imported using Maven:

- [com.fazecast:jSerialComm:2.7.0](https://mvnrepository.com/artifact/com.fazecast/jSerialComm)
- [mysql:mysql-connector-java:8.0.182](https://mvnrepository.com/artifact/mysql/mysql-connector-java)
