Nanodash Installation Instructions
==================================

Nanodash is a client application that you can run on your own computer to browse and publish nanopublications by connecting to a decentralized network of services.

Follow the three simple steps below to install and run Nanodash.
(Alternatively, see [these instructions](INSTALL-with-Docker.md) to run it with Docker Compose, if you are familiar with that technology.)


## Step 1: Install Java

You need to have Java installed (version 11 or higher), which you can [download here](https://www.oracle.com/java/technologies/downloads/).
If you are unsure whether you have Java installed already, you can continue with the steps below and you will see an error message if Java is not found.

To find out what Java version you have, you can type `java -version` in a terminal window. If your version number is lower than 11, you need to install a newer version with the link above.

## Step 2: Download and Unpack Nanodash ZIP File

Download the ZIP file with a name like `nanodash-1.61.zip` for the latest release [here](https://github.com/knowledgepixels/nanodash/releases/latest).
Unpack it at a convenient location in your file system.
This should create the files `nanodash.jar`, `run`, and `run-under-windows.bat` (and also `update` and `update-under-windows.bat`).


## Step 3: Run Nanodash and Follow Instructions

To start Nanodash, double-click on `run-under-windows.bat` (for Windows) or `run` (for other operating systems, such as Mac or Linux).
Under Unix systems, you might have to start it from the commandline with `./run`.
After a few seconds, a new browser tab with the Nanodash interface should automatically open.
If not, open [http://localhost:37373](http://localhost:37373) manually.
Follow the instructions to complete your profile as shown on your [profile page](http://localhost:37373/profile).

Now you are ready to publish your own nanopublications via the "publish" menu item at the top.

If Nanodash doesn't start up and you are seeing an error message like the one below, then your installed Java is too old and you need to follow Step 1 above to install Java 11 or above:

> java.lang.UnsupportedClassVersionError: ... has been compiled by a more recent version of the Java Runtime (class file version 55.0), this version of the Java Runtime only recognizes class file versions up to 52.0

## Update

To update to the latest version, shut down Nanodash and then run `update` or `update-under-windows.bat`, depending on your operating system.

## Problems?

If you run into problems, [open an issue](https://github.com/knowledgepixels/nanodash/issues) or contact [Tobias Kuhn](mailto:kuhntobias@gmail.com).
