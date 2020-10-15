Nanobench Installation Instructions
===================================

Nanobench is a client application that you can run on your own computer to browse and publish nanopublications by connecting to a decentralized network of services.

Follow the three simple steps below to install and run Nanobench.
(Alternatively, see [these instructions](INSTALL-with-Docker.md) to run it with Docker Compose, if you are familiar with that technology.)


## Step 1: Install Java

You need to have Java installed (version 1.7 or higher), which you can [download here](https://www.java.com/download/).
If you are unsure whether you have Java installed already, you can continue with the steps below and you will see an error message if Java is not found.


## Step 2: Download and Unpack Nanobench ZIP File

Download the ZIP file with a name like `nanobench-1.2.zip` for the latest release [here](https://github.com/peta-pico/nanobench/releases/latest).
Unpack it at a convenient location in your file system.
This should create three files: `nanobench.jar`, `run`, and `run-under-windows.bat`.


## Step 3: Run Nanobench and Follow Instructions

To start Nanobench, double-click on `run-under-windows.bat` (for Windows) or `run` (other operating systems, such as Mac or Linux).
Under Unix systems, you might have to start it from the commandline with `./run`.
After a few seconds, a new browser tab with the Nanobench interface should automatically open.
If not, open [http://localhost:37373](http://localhost:37373) manually.
Follow the instructions to complete your profile as shown on your [profile page](http://localhost:37373/profile).

Now you are ready to publish your own nanopublications via the "publish" menu item at the top.

## Update

To update to the latest version of Nanobench, delete all the files you downloaded and then redo Step 2.

## Install from source

You can also install Nanobench from the source code to get the latest updates, or improve it. 

You will need to have [maven installed](https://maven.apache.org/install.html) to build the application.

First clone the repository:

```bash
git clone https://github.com/peta-pico/nanobench.git
cd nanobench
```

### Run for development

Build and run Nanobench on http://localhost:37373

```bash
mvn clean tomcat7:run
```

### Build the jar

Build the `.jar` using maven at the root of the repository:

```bash
mvn clean install tomcat7:exec-war-only
```

> It will generate a `.jar` and a `.war` file in the `target` folder

Rename the jar:

```bash
cp target/nanobench-*.jar target/nanobench.jar
```

Start Nanobench:

```bash
java -jar target/nanobench.jar -httpPort 37373 -resetExtract
```

## Problems?

If you run into problems, [open an issue](https://github.com/peta-pico/nanobench/issues) or contact [Tobias Kuhn](mailto:kuhntobias@gmail.com).
