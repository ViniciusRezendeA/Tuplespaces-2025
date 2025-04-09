# TupleSpaces

Distributed Systems Project 2025

**Group A30**


**Difficulty level: I am Death incarnate!**


### Team Members

*(fill the table below with the team members, and then delete this line)*

| Number | Name               | User                                   | Email                                                  |
|--------|--------------------|----------------------------------------|--------------------------------------------------------|
| 107079 | Diogo Rodrigues    | <https://github.com/diogoadr>          | <mailto:diogo.d.rodrigues@tecnico.ulisboa.pt>          |
| 1115485  | Vinicius Moreira   | <https://github.com/ViniciusRezendeA>  | <mailto:vinicius.r.arantes@tecnico.ulisboa.pt>         |
| 107219 | David Ferreira     | <https://github.com/David-LEIC-IST>    | <mailto:david.r.a.v.ferreira@tecnico.ulisboa.pt>       |
>

## Getting Started

The overall system is made up of several modules.
The definition of messages and services is in _Contract_.

See the [Project Statement](https://github.com/tecnico-distsys/Tuplespaces-2025) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
