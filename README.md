# DM874-webserver
## Description
This is the service responsible for serving the static react site and establishing maintaining the websocket connection to the client.

This service can only send new messages out and receive new messages to relay to the users.
## Service description
This service is coupled with the router, but is only decoupled in the service layer for the reason to make routing independantly scalable.
The service can be interacted with through the topic in the webserver-config configmap dynamically specified in the kubernetes config store.
## Configuration
The configuration is supplied in the `application.conf` file in HOCON format (default typesafe configuration).
