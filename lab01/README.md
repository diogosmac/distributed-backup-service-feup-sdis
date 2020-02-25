# Lab 01

#### Client-Server application to associate DNS names and IP addresses

In this application, the user can register DNS names with a given IP address, and that association will be stored in the server.
The user can then look up a registered DNS name, to obtain its IP address.

## Server
Through your Terminal, you can run the server at your port of choice.

> **Usage**: java Server \<port>
>
> **Example**: `java Server 8000`

## Client
Through your Terminal, you can send requests to the server, and receive the response.

### Required Functionalities:

#### Register a DNS name:

> **Usage:** java Client \<host> \<port> register \<DNS name> \<IP address>
>
> **Example**: `java Client 192.168.1.75 8000 register website.com 192.168.1.76`
 
The server will respond:
- the new table size (e.g. **1**), if the DNS name was successfully registered
- ALREADY_REGISTERED, if the DNS name is already on the database

#### Lookup a DNS name:

> **Usage:**: java Client \<host> \<port> lookup \<DNS name>
>
> **Example**: `java Client 192.168.1.75 8000 lookup website.com`

The server will respond:
- the corresponding IP address (e.g. **192.168.1.76**), if the DNS name had been previously registered
- NOT_FOUND, if the DNS name does not exist on the database

### Additional Functionalities:

#### Reset the Database:

> **Usage:** java Client \<host> \<port> reset
>
> **Example**: `java Client 192.168.1.75 8000 reset`

The server will respond:
- RESET_SUCCESSFULLY, if no problems occurred
- ERROR, if the operation wasn't completed

#### Shut Down the Server:

> **Usage** java Client \<host> \<port> close
>
> **Example**: `java Client 192.168.1.75 8000 close`

The server will respond:
- CLOSED_SUCCESSFULLY, if no problems occurred
- ERROR, if the operation wasn't completed
