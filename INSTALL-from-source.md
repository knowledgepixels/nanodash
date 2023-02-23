Nanodash Installation from Sources
==================================

You can also install Nanodash from the source code, you need to have [maven installed](https://maven.apache.org/install.html) to build the application.

First clone the repository:

```bash
git clone https://github.com/knowledgepixels/nanodash.git
cd nanodash
```

### Run for development

Build and run Nanodash on http://localhost:37373

```bash
mvn clean tomcat7:run
```

### Build the jar

Build the `.jar` using maven at the root of the repository:

```bash
mvn clean install tomcat7:exec-war-only
```

It will generate a `.jar` and a `.war` file in the `target` folder

Rename the jar:

```bash
cp target/nanodash-*.jar target/nanodash.jar
```

Start Nanodash:

```bash
java -jar target/nanodash.jar -httpPort 37373 -resetExtract
```

## Problems?

If you run into problems, [open an issue](https://github.com/knowledgepixels/nanodash/issues) or contact [Tobias Kuhn](mailto:kuhntobias@gmail.com).
