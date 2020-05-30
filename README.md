# Distributed key-value store with Akka

The goal of this project is to provide a distributed key-value store using [Akka](https://akka.io/). The store stores each data element into R replicas to tolerate up to R-1 simultaneous failures without losing any information. Upon the failure of a node, the data it stored is replicated to a new node to ensure that the system has again R copies.

## Architecture

The architecure proposed is based in layers. The first layer is the API layer which provides an easy way to interact with the cluster, which is on top of Akka as it is shown below.

![Akka architecture](docs/arch.png)

## API

The API is divided in two modules: (I) one provides endpoints in order to add new elements, update its values or get the object associated to a key, and (II) the other provides different endpoints in order to check the status of the cluster.

There are three endpoints which can be used to get, add or update the objects stored in the database. In order to make it simpler, they have been all set as GET.

| Path | HTTP method | Required form fields |
| ---- | ----------- | -------------------- |
| `/database/get` | GET | Returns all the keys stored in the database. |
| `/database/get/:key` | GET | Returns the value associated to `:key`. |
| `/database/put/:key/:value` | GET | Adds a new entry with key `:key` and value `:value`. |
| `/database/node/get` | GET | Returns the keys stored on the current node. |

### AkkaManagement

The Akka Management API provides several routes in order to test the state of the cluster and its members.

| Path | HTTP method | Required form fields |
| ---- | ----------- | -------------------- |
| `/cluster/members` | GET | Returns the status of the Cluster in JSON format. |
| `/cluster/members/:address` | GET | Returns the status of `:address` in the Cluster in JSON format. |
| `/cluster/shards/:name` | GET | Returns shard info for the shard region with the provided `:name`. |

## Deployment

The deployment is made using [Docker](https://docs.docker.com/engine/docker-overview/) and
[Docker Compose](https://docs.docker.com/compose/). It is based on minimum two containers: the
first one is the seed, the node of the cluster where other nodes should join automatically at startup.

```shell
docker-compose up
```

If some changes are made in the code, it is needed to rebuild the image. It could be done
at the same time as the deployment by using the following command.

```shell
docker-compose up --build
```

If the user wants to build the image without running it, the following command can be used.

```shell
docker build -t middleware/akka .
```

## Environment variables

Some environment variables are provided to have a minimum configuration for the service.

| Variable | Default value | Purpose |
| -------- | ------------- | ------- |
| `AKKA_LOG_LEVEL`         | INFO          | Log level used by the configured loggers. |
| `AKKA_CLUSTER_IP`        | 127.0.0.1     | The ip where the cluster is running. It is the address where other nodes should connect. |
| `AKKA_CLUSTER_NAME`      | ClusterSystem | The name of the cluster. It must be the same for all the nodes that belongs to the same cluster. |
| `AKKA_CLUSTER_PORT`      | 2552          | The port where the cluster is listening. It is the port where other nodes should connect to. |
| `AKKA_CLUSTER_ROLE`      | ""            | The role of this node in the cluster. |
| `AKKA_CLUSTER_SEED_IP`   | 127.0.0.1     | The ip of the node to join automatically at startup. |
| `AKKA_CLUSTER_SEED_PORT` | 2552          | The port of the node to join automatically at startup. |
| `AKKA_SERVER_PORT`       | 8080          | The port where the http server listens to. |
| `AKKA_SERVER_MANAGEMENT` | true          |Enable the api management of the cluster.  |
| `AKKA_CLUSTER_PARTITION_NUMBER`   | 2E16       | Number maximum of nodes in the cluster. It must be the same for all of them. |
| `AKKA_CLUSTER_REPLICATION_NUMBER` | 2          | Replication factor. It makes the system tolerates up to `R-1` failures. |
