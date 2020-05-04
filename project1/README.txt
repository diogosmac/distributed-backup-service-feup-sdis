This project was developed on Java 11.0.6.

Compiling:

    To compile our project, on both Windows and Linux, you need only change directory to the /src folder, and compile
    all .java files using javac:
        cd <base_directory>
        javac -d build src/*.java

    Note - if you're on Linux, or on Windows and using the Windows subsystem for Linux, you can compile the code using
    the script compile.sh, from the base directory of the project, and remove the compilation files with our script
    decompile.sh, also from the base directory of the project.


Execution:

    1) Starting the RMI Registry:
    To start the RMI Registry, you only need to execute one command on the terminal, after compilation, inside the build
    folder:
        If on Windows:  start rmiregistry
        If on Linux:    rmiregistry &

    2) Starting the Peers:
    To run a Peer from the terminal, you use the following command:
        java Peer <version> <peer_id> <peer_ap> <mc_address> <mc_port> <mdb_address> <mdb_port> <mdr_address> <mdr_port>

    3) Running the test Application:
    The commend to execute the test Application is as following:
        java Application <peer_ap> <operation> [<operand_1> [<operand_2>]]


We have developed additional scripts, before the demonstration scripts were requested, to test the functionalities of
our Backup Service (located in the scripts/testing directory):

    - LaunchPeers.sh >  sh LaunchPeers.sh <n_peers_to_launch> <peer_protocol_version>
        Opens N terminals, and launches a Peer in every terminal;

    - Backup.sh      >  sh Backup.sh <peer_id> <file_path> <desired_replication_degree>
        Opens a terminal and starts a Backup (through Application.java) for a given file, with a given desired
        replication degree, to a given Peer;

    - Restore.sh     >  sh Restore.sh <peer_id> <file_path>
        Opens a terminal and starts a Restore (through Application.java) of a given file in a given Peer;

    - Delete.sh      >  sh Delete.sh <peer_id> <file_path>
        Opens a terminal and sends a Delete message (through Application.java) of a given file in a given Peer;

    - Reclaim.sh     >  sh Reclaim.sh <peer_id> <desired_space_usage>
        Opens a terminal and starts a Reclaim process (through Application.java), of a given Peer's memory:

    - State.sh       >  sh State.sh <peer_id>
        Opens a terminal and asks for the state of a given Peer (through Application.java); stays open for 10
        seconds, in order for the user to be able to read the requested info.
