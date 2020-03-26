Nanobench Installation Instructions
===================================

Nanobench is a client application that you can run on your own computer to browse and publish nanopublications by connecting to a decentralized network of services.

Follow the three simple steps below to install and run Nanobench.
(Alternatively, see [these instructions](INSTALL-with-Docker.md) to run it with Docker Compose, if you are familiar with that technology.)


## Step 1: Install Java

You need to have Java installed (version 1.8 or higher), which you can [download here](https://www.java.com/download/).
If you are unsure whether you have Java installed already, you can continue with the steps below and you will see an error message if Java is not found.


## Step 2: Download and Unpack Nanobench ZIP File

Download the ZIP file with a name like `nanobench-1.2.zip` for the latest release [here](https://github.com/peta-pico/nanobench/releases/latest).
Unpack it at a convenient location in your file system.
This should create three files: `nanobench.jar`, `run`, and `run-under-windows.bat`.


## Step 3: Run Nanobench and Follow Instructions

To start Nanobench, double-click on `run-under-windows.bat` (for Windows) or `run` (otherwise).
Under Unix systems, you might have to start it from the commandline with `./run`.
After a few seconds, a new browser tab with the Nanobench interface should automatically open.
If not, open [http://localhost:37373](http://localhost:37373) manually.
Follow the instructions to complete your profile as shown on your [profile page](http://localhost:37373/profile).

Now you are ready to publish your own nanopublications via the "publish" menu item at the top.
