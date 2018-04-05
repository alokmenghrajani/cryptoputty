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

## Jooq
To run migrations/seeds you’ll do something like:

```
mysql -u root --host=localhost --port=3306 --protocol=TCP < migrations.sql
```

Then to generate Jooq classes you’ll do something like:

```
java -classpath $HOME/.m2/repository/org/jooq/jooq/3.10.6/jooq-3.10.6.jar:$HOME/.m2/repository/org/jooq/jooq-meta/3.10.6/jooq-meta-3.10.6.jar:$HOME/.m2/repository/org/jooq/jooq-codegen/3.10.6/jooq-codegen-3.10.6.jar:$HOME/.m2/repository/mysql/mysql-connector-java/5.1.35/mysql-connector-java-5.1.35.jar org.jooq.util.GenerationTool jooq-codegen.xml
```
