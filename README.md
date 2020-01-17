# CO657-IoT_Server

A Central Server for my IoT system, to work with my <a href="https://github.com/d-w-arnold/CO657-IoT_Device/" target="_blank">IoT device</a>.

Receives input from my IoT device over Serial, detailing the BLE MAC address of a BLE device just come home.

Pulls BLE device information from a database, storing the information locally and periodically checking for updates in the database.

When input is received over Serial from the IoT device, the IoT server toggles the living room smart home light (according to the information for the given BLE device obtained from the database).

---

Libraries imported using Maven:

- [com.fazecast:jSerialComm:2.5.22](https://mvnrepository.com/artifact/com.fazecast/jSerialComm)
- [mysql:mysql-connector-java:8.0.182](https://mvnrepository.com/artifact/mysql/mysql-connector-java)
