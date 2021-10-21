# Briar Mailbox Rest API

All mailbox requests are authenticated with a Bearer Authorization HTTP header:

    Authorization: Bearer 0000000000000000000000000000000000000000000000000000000000000000

To make a request to a mailbox via Tor (running with a SOCKS proxy on the system already),
the following command can be used.

```bash
curl --socks5-hostname 127.0.0.1:9050 -v -H "Authorization: Bearer 0000000000000000000000000000000000000000000000000000000000000000" http://example.org
```

## Mailbox Management (owner only)


### Setup/Pairing (owner only)

Used by owner to pair with or setup their mailbox (which needs to be in the setup state).
Request is authenticated by a single-use token from the QR code shown by the mailbox app.
The request contains no other data.

```http
PUT /setup
```

If the token hasn't been used then the call returns the long-term owner auth token for subsequent calls along with a HTTP status code `201 Created`.
Response example:

```json
{
  "token": "2b063589ac6203feffa40c120716b79a472d607c089c42203819f363dee3266c"
}
```

If the token has already been used then the call returns `401 Unauthorized` and Briar should tell the user to reset/wipe the mailbox and try again.

After a successful response, Briar performs the requests it does on each new connection to the mailbox, e.g. syncing contacts.
If Briar crashes after the mailbox processes the pairing call, but before Briar receives the reply, then after restarting Briar the user should be led through the steps to wipe the mailbox and start again, whenever the user visits the mailbox status screen.
If we lose the connection, but Briar doesn't crash after the mailbox processes the pairing call, but before Briar receives the reply, then the Briar UI leads the user through the steps to reset/wipe the mailbox and start again as well.

### Remote wipe (owner only)

Unpairs Briar and the mailbox.
Resets mailbox state to that after first install ( e.g. removes all stored files as well).

```http
DELETE /
```

Returns an empty `204 No Content` response if successful or an error code if not.

### Status request (owner only)

Gets info about whether mailbox is running.

```http
GET /status
```

Always returns `200 OK`.
Later we could add other info like how much data is waiting to be downloaded by contacts and the owner.


## Contact Management (owner only)

### Add a contact (owner only)

Adds a new contact to the mailbox.

```http
POST /contacts
```

Briar generates random 32-byte `token`, `inboxId` and `outboxId` encoded as hexadecimal strings
and sends them along with its `contactId`.

Example request body:

```json
{
  "contactId": 3,
  "token": "3dd6c6b313f692bd10e33099ed0819a5f721478213d8f632af5b3fe203e2e222",
  "inboxId": "07e6eacc0eca14547481498ec6a46cf90ddc4b8d7d87e5a97d03377695fae394",
  "outboxId": "2a067589ac6203feffa40c120716b79a472d607c089c42203819f363dee3266c"
}
```

The API responds with HTTP status code `201 Created` for a successful `POST`.
If the `contactId` is already in use, `409 Conflict` is returned.

### Delete a contact  (owner only)

```http
DELETE /contacts/$contactId
```

`$contactId` is the integer contact ID the contact was added with.
Returns `200 OK` when deletion was successful.

### Get list of contacts  (owner only)

Gets list of contacts managed by the mailbox identified by their `contactId`.

```http
GET /contacts
```

Returns `200 OK` with a list of contact IDs like this:

```json
{
  "contacts": [ 1, 3, 4, 6 ]
}
```


## File Managment (owner and contacts)

A file contains a stream of message, events etc.

### Adding a file to a folder (owner and contacts)

Used by contacts to send files to the owner and by the owner to send files to contacts.
The owner can add files to the contacts' inboxes and the contacts can add files to their own outbox.

```http
POST /files/$folderId
```

The mailbox checks if the provided auth token is allowed to upload to $folderId
which is either an `inboxId` or an `outboxId` of a contact.

The file content is a raw byte stream in the request body.
The mailbox chooses a random string for the file ID (32 hex encoded bytes).
Returns `200 OK` if upload was successful (no `201` as the uploader does not need to know the `$fileName`).

### List files available in a folder (owner and contacts)

Used by owner and contacts to list their files to retrieve.

```http
GET /files/$folderId
```

The mialbox checks if provided auth token is allowed to download from $folderId
which is either an `inboxId` or an `outboxId` of a contact.

Returns `200 OK` with the list of files in JSON (example):

```json
{
  "files": [
    { "name": "ae6751c8e90fa347e24afaa977e180cdd7cfd8fa5194954f6467d2cc51c87640", "time": 1629816408 },
    { "name": "5aff93e611bc338dffa0c2337b416656e583e6eba23602a209633eb2362e2aa3", "time": 1629816410 },
    { "name": "d76dba27aaf763de8a2e8c543b3bb5969fa32fb398816e301c577e619d4d3232", "time": 1629816418 }
  ]
}
```

### Download a file  (owner and contacts)

Used by owner and contacts to retrieve a file.

```http
GET /files/$folderId/$fileName
```

Checks if the provided auth token is allowed to download from `$folderId`
which is either an `inboxId` or an `outboxId` of a contact.

Returns `200 OK` if successful with the files' raw bytes in the response body.

### Delete a file (owner and contacts)

Used by owner and contacts to delete files.

```http
DELETE /files/$folderId/$fileName
```

Checks if provided auth token is allowed to download from $folderId
which is either an `inboxId` or an `outboxId` of a contact.

Returns `200 OK`  if deletion was successful.

### List folders with files available for download (owner only)

Lists all contact outboxes that have files available for the owner to download.

```http
GET /folders
```

Checks if provided auth token is belonging to the owner.

Returns `200 OK`  with the list of folders with files in JSON (example):

```json
{
  "folders": [
    { "id": "ae6751c8e90fa347e24afaa977e180cdd7cfd8fa5194954f6467d2cc51c87640" },
    { "id": "3dd6c6b313f692bd10e33099ed0819a5f721478213d8f632af5b3fe203e2e222" },
    { "id": "5aff93e611bc338dffa0c2337b416656e583e6eba23602a209633eb2362e2aa3" }
  ]
}
```
