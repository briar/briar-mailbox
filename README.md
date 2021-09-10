# Briar Mailbox

This project aims to develop an easy way for Briar users to increase their 
reachability and lower the battery drain of their phone.

In Briar messages are exchanged directly between contacts (peer-to-peer). 
This kind of synchronous message exchange requires contacts to be online and 
connected to each other.  
While this is great for privacy (no central server which
can log things or be censored) it's bad for reachability, especially in
mobile networks where connectivity can be limited.

```mermaid
graph LR
 A[Alice]
 B[Bob]
 A1[Alice]
 B1[Bob]
 style B fill:#8db600
 style A1 fill:#8db600
 subgraph Alice offline
 B-. can't send message .-> A
 end
 subgraph Bob offline
 B1-. can't send message .-> A1
 end
```

Message delivery could be delayed for an arbitrary time (or even indefinitely) 
until both Bob and Alice are online at the same time.  
The repeater solves this problem by providing 
mailbox-like message buffer where contacts can leave messages for the owner 
of the repeater and which is connected to a stable internet connection 
(e.g. the wifi at home, cable internet) and a power source.
 

```mermaid
graph LR
  A[Alice]
  A1[Alice]
  B[Bob]
  B1[Bob]
  RA["Mailbox (always online)"]
  style B fill:#8db600
  style RA fill:#8db600
  style A1 fill:#8db600
  subgraph Alice offline
  B-. can't send message .-> A
  end
  subgraph Alices' Mailbox
  B-- send message --> RA
  end
  subgraph Alice online
  B1-. can't send message .-> A1
  A1-- get message --> RA
  end
```

## Hardware

We want the repeater to be as easy to deploy as possible. The first version 
will come as Android application since it will be easy to setup and besides a 
spare phone no special hardware is required. Once this is done support for
any hardware supporting Java (e.g. unix server, raspberry pi) will be added.

## Features

### Core features

* Allow contacts to store messages for the owner of the repeater
* Allow the owner to store messages for her contacts. Contacts can pick them up
  when syncing with the repeater.
* Owner and contacts connect to the repeater via Tor.

### Extended features/components

* The repeater can sync group messages (from groups the owner is part of) with 
  other group members (increases message circulation)
* Contacts and the owner can connect to the repeater via other transports (Bluetooth, Wifi-Direct, Lan)
* Push-like message notification for the owner to decrease battery drain



