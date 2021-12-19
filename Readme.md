# Paxos protocol implementation

The project was written in java.

It needs at least java 8 to run.

<br>

## How to build
Use [ant](https://ant.apache.org/) to build the project.

Example:
    
    $ project % ant
    Buildfile: /Users/elia/projects/paxos/test/java/build.xml

    compile:
        [javac] Compiling 9 source files to /Users/elia/projects/paxos/test/java/bin
        [echo] Compiling classes...

    jar:
        [jar] Building jar: /Users/elia/projects/paxos/test/java/bin/Paxos.jar
        [echo] Building the .jar file...

    main:
        [echo] Building the Project...

    BUILD SUCCESSFUL
    Total time: 0 seconds
<br>

## How to run

A simple execution can be run as follows.

In separate terminal windows (separate machines in our cluster, for example), run the commands:

Start a proposer:

    $ node1 % ./proposer.sh 0 paxos.conf

Start acceptors:

    $ node2 % ./acceptor.sh 0 paxos.conf

    $ node3 % ./acceptor.sh 1 paxos.conf

    $ node4 % ./acceptor.sh 2 paxos.conf

Start learners writing output to a file:

    $ node5 % ./learner.sh 0 paxos.conf > learn1

    $ node6 % ./learner.sh 1 paxos.conf > learn1

Generate the input for clients and start a client:

    $ node7 % ./generate.sh 100 > prop1
    $ node7 % ./client.sh 0 paxos.conf < prop1

<br>
<br>

The application does not perform well for large inputs in a local environment, as there will be a heavy load of messages and high levels of thread contention in the system.

So, for example, the final waiting time in the `run.sh` script (the last sleep cmd) should be increased when running local tests for 10000 input values (in my tests, it took about 2 minutes for the learners to learn all values proposed by clients in this case).

It performs OK in our cluster, though, running each process in different machines.