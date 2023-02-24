Nanodash Installation Instructions with Docker
===============================================

Nanodash is a client application that you can run on your own computer to browse and publish nanopublications by connecting to a decentralized network of services.

These are the installation instructions using Docker and Docker Compose.
If you are not familiar with these technologies, see the [regular installation instructions](INSTALL.md).


## Step 1: Install Docker and Docker Compose

Make sure you have [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/) installed.


## Step 2: Download and Run docker-compose.yml

Download [docker-compose.yml](docker/local/docker-compose.yml) to a convenient location in your file system.
Then run `docker-compose up` on the command line in the same directory.


## Step 3: Follow Instructions

After a few seconds, Nanodash will be accessible at [http://localhost:37373](http://localhost:37373) in your browser.
Follow the instructions to complete your profile as shown on your [profile page](http://localhost:37373/profile).

Now you are ready to publish your own nanopublications via the "publish" menu item at the top.


## Update

To update to the latest version of Nanodash, run `docker-compose down && docker-compose pull` in the directory of `docker-compose.yml`. Then start Nanodash as in Step 2.


## Problems?

If you run into problems, [open an issue](https://github.com/knowledgepixels/nanodash/issues) or contact [Tobias Kuhn](mailto:kuhntobias@gmail.com).
