# Lab 03

#### Client-Server application to associate DNS names and IP addresses (3rd Iteration)

In this iteration of the application, multicast communication is used. The server broadcasts to a multicast socket, which the client can listen to in order to obtain the address and port of the server, to which it can send the commands.

## Server
Through your Terminal, you can run the server \[xxx\]

> **Usage**: java Server \<tbd>
>
> **Example**: `java Server <tbd>`

## Client
Through your Terminal, you can send requests to the server, and receive the response.

### Required Functionalities:

#### Register a DNS name:

> **Usage:** java Client \<tbd> register \<DNS name> \<IP address>
>
> **Example**: `java Client <tbd> register website.com 192.168.1.76`
 
The server will respond:
- the new table size (e.g. **1**), if the DNS name was successfully registered
- ALREADY_REGISTERED, if the DNS name is already on the database

#### Lookup a DNS name:

> **Usage:**: java Client \<tbd> lookup \<DNS name>
>
> **Example**: `java Client <tbd> lookup website.com`

The server will respond:
- the corresponding IP address (e.g. **192.168.1.76**), if the DNS name had been previously registered
- NOT_FOUND, if the DNS name does not exist on the database

### Additional Functionalities:

#### Reset the Database:

> **Usage:** java Client \<tbd> reset
>
> **Example**: `java Client <tbd> reset`

The server will respond:
- RESET_SUCCESSFULLY, if no problems occurred
- ERROR, if the operation wasn't completed

#### Shut Down the Server:

> **Usage** java Client \<tbd> close
>
> **Example**: `java Client <tbd> close`

The server will respond:
- CLOSED_SUCCESSFULLY, if no problems occurred
- ERROR, if the operation wasn't completed
