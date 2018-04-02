# cryptoputty

## Cloning the repo
We are using git submodules. So:

    git clone --recursive git@github.com:alokmenghrajani/cryptoputty.git

## Building & starting

We recommend using Intellij. Import project from sources and you'll be all set. Pick JDK1.8 instead of Java9.

Alternatively:
1. Run `mvn clean install` to build your application
2. Start application with `java -jar target/cryptoputty-1.0-SNAPSHOT.jar server config.yml`
3. To check that your application is running enter url `http://localhost:8080`

## Health Check
To see your applications health enter url `http://localhost:8081/healthcheck`
