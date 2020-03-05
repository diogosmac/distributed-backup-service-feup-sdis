# Lab 03

#### client.Client-server.server application to associate DNS names and IP addresses (2nd Iteration)

In this iteration of the application, multicast communication is used. The server broadcasts to a multicast socket, which the client can listen to in order to obtain the address and port of the server, to which it can send the commands.

## server.server
Through your Terminal, you can run the server at your port of choice.

> **Usage**: java server.server \<servicePort> \<multicastIP> \<multicastPort>
>
> **Example**: `java server.server 8080 224.0.0 8000`

## client.Client
Through your Terminal, you can send requests to the server, and receive the response.

### Required Functionalities:

#### Register a DNS name:

> **Usage:** java client.Client \<multicastIP> \<multicastPort> register \<DNS name> \<IP address>
>
> **Example**: `java client.Client 224.0.0.0 8000 register website.com 192.168.1.76`
 
The server will respond:
- the new table size (e.g. **1**), if the DNS name was successfully registered
- ALREADY_REGISTERED, if the DNS name is already on the database

#### Lookup a DNS name:

> **Usage:**: java client.Client \<multicastIP> \<multicastPort> lookup \<DNS name>
>
> **Example**: `java client.Client 224.0.0.0 8000 lookup website.com`

The server will respond:
- the corresponding IP address (e.g. **192.168.1.76**), if the DNS name had been previously registered
- NOT_FOUND, if the DNS name does not exist on the database

### Additional Functionalities:

#### Reset the Database:

> **Usage:** java client.Client \<multicastIP> \<multicastPort> reset
>
> **Example**: `java client.Client 224.0.0.0 8000 reset`

The server will respond:
- RESET_SUCCESSFULLY, if no problems occurred
- ERROR, if the operation wasn't completed

#### Shut Down the server.server:

> **Usage** java client.Client \<multicastIP> \<multicastPort> close
>
> **Example**: `java client.Client 224.0.0.0 8000 close`

The server will respond:
- CLOSED_SUCCESSFULLY, if no problems occurred
- ERROR, if the operation wasn't completed
