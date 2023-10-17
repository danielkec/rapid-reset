

package:
	mvn clean package;

native-image:
	# sdk use java 21-graal
	native-image -jar ./target/*.jar ./target/rapid-reset

install:
	cp ./target/rapid-reset /usr/bin/rapid-reset