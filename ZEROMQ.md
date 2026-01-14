# ZeroMQ

# Testing setup

To run tests, you will need the following:

- The M3 microservices running on your local computer against a non-production database. Not to worry, the M3 stack has just such a database configured for you already. 
- A ZeroMQ subscriber (SUB) listening to port 5563 to the vars topic
- A script to create annotations so that you’re subscriber has something to work with.

---

## Start M3 services

To run the microservices and database, you will need to have [docker](https://www.docker.com) installed.

```shell
git clone git@bitbucket.org:mbari/vars-quickstart-mbari.git
cd vars-quickstart-mbari
./varsq configure etc/env/localhost.env
./varsq start
```

---

## Run Test Using Python

You can use any python, but I developed against [miniconda](https://docs.conda.io/en/latest/miniconda.html). If not using conda, refer to the environment.yml file for the required python dependencies.

### Setup

```shell
git clone git@bitbucket.org:mbari/m3py.git
cd m3py
conda env create -f environment.yml
```

### Run ZMQ Sbscriber

```shell
cd m3py
conda activate m3py
python zmq_subsciber.py
```

### Create test annotations

```shell
cd m3py
conda activate m3py
export ANNO_CLIENT_SECRET=foo    #bash
# set -x ANNO_CLIENT_SECRET foo  #fish
python zmq_test_create_annotation.py \
  --annosaurus_url http://localhost:8082/anno/v1 \
  "Blue Dog"
```

## Example Subscriber

### zmq_subscriber.sc

```scala
import scala.util.control.NonFatal
import $ivy.`org.zeromq:jeromq:0.5.2`
import org.zeromq.SocketType
import org.zeromq.ZMQ
import org.zeromq.ZMQ.Socket
import org.zeromq.ZContext

@doc("Listens to annosaurus for new annotation messages")
@main
def main(
    @doc("The ZMQ publisher port") port: Int = 5563,
    @doc("The ZMQ publisher host") host: String = "localhost",
    @doc("The ZMQ topic to listen to") topic: String = "vars") {
  try {
    val context = new ZContext
    val socket = context.createSocket(SocketType.SUB)
    socket.connect(s"tcp://$host:$port")
    socket.subscribe(topic.getBytes(ZMQ.CHARSET))
    // socket.connect("tcp://localhost:5563")
    // socket.subscribe("vars".getBytes(ZMQ.CHARSET))
    println(s"Listening to tcp://$host:$port")
    while (!Thread.currentThread().isInterrupted()) {
      val address = socket.recvStr()
      val contents = socket.recvStr()
      println(s"$address : $contents")
    }

  } catch {
    case NonFatal(e) => println("Boom! An exception occurred :-(")
  }

}

```