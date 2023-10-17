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

```shell
rapid-reset https://localhost:8080
```