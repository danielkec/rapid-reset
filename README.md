# HTTP/2.0 Rapid Reset reproducer
Reproducer for [CVE-2023-44487](https://nvd.nist.gov/vuln/detail/CVE-2023-44487) HTTP/2.0 rapid rest vulnerability
built on top of [Helidon.io](https://helidon.io) HTTP/2 toolset.

Usage of the tool is meant only for testing and at one's own responsibility.

## Build

1. Build Java artefact
    ```shell
    make
    ```
2. Build native image binary(don't forget set graal as your SDK `sdk use java 21-graal`) 
    ```shell
    make native-image
    ```
3. Copy the binary to `/usr/bin`
    ```shell
    make install
    ```


## Usage
Server is either expected to be overwhelmed or the connection gets cut off 
when server is immune to the attack (floating window counting). 

By default, reproducer sends 100M requests with HEADERS frame followed 
immediately by RST frame to http://localhost:8080. 

Reproducer uses only single connection as it is meant for testing.

```shell
rapid-reset [uri [number-of-requests]]
```

### Java archive (JRE 21 or higher required)
```shell
java -jar rapid-reset-1.0-SNAPSHOT.jar https://localhost:8080
```

### Native binary
```shell
rapid-reset https://localhost:8080
```