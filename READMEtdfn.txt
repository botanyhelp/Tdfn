Facebase 3D Facial Norms Human Data Manager README

The Java program is started by running:
java HumanDataController.java 
It supports the online 3D Facial Norms program running 
at:
www.facebase.org/facial_norms  

It finds, packages and makes 
available in encrypted form all of the data 
that the user of the web application requests. 
Lots of things, discussed here, must be in place for 
proper operation.  The program must be run as root 
because it is the parent process of another process 
that mounts and unmounts a filesystem.
The source code java files have inline 
documentation of good quality.  


DOCUMENTATION NOTES: 
There are several pieces of documentation you'll
want to obtain: 
1. This file.  
2. All of the Java classes have embedded
JavaDoc and some inline documentation.  
3. A class diagram (NOTE TO SELF--UPDATE CLASS DIAGRAM).  
4. An activity diagram (NOTE TOSELF--WRITE ACTIVITY DIAGRAM).  
5. All of the class files log heavily to

log.debug("logs are docs too").  The strings inside log.debug() generally
serve as local documentation of what the code is doing.  6. Changing
log4j.properties to DEBUG mode will generate lots of logging data that
is useful to read and learn what the application is doing.

OVERVIEW: The qmgr program is part of Facebase's human data architecture.
Its job is to learn about user requests for human data, to assemble the
files and data associated with that request, to package that data into
an encrypted (TrueCrypt) file and to make the TrueCrypt file available
to the user for download.

FIREWALL The Facebase human data architecture consists of two machines
running linux--the Facebase Hub (www.facebase.org) and a firewalled-off
human data server.  The firewall, roughly speaking, allows the human data
server to contact the Hub on a few ports (80, 443, 3306, 22) but disallows
all connections from the Hub to the human data server; the firewall also
disallows connections to the human data server from the Internet.

SOFTWARE COMPONENTS The qmgr runs as a single process on the human data
server.  It spawns child processes during operation, shell processes, to
perform tasks related to TrueCrypt operation.  As java, the qmgr consists
of a dozen classes and uses a dozen libraries.  The qmgr communicates
with modules running inside Drupal one on the Hub and another Drupal
module running on the human data server.  The Hd server's Drupal has one
custom module installed for managing and encryption keys, fb_keychain,
and another for synchronizing human data, fb_bridge.  The Hub's Drupal
has two custom modules, fb_access and fb_queue that manage, respectively
access-to and packaging-of, human data.

BASIC OPERATION The qmgr polls a database table ("fb_queue", installed and
managed by the Drupal module) to learn about new requests for human data.
When a new request comes in, the logic to fulfill the request is executed.
Each user request for data is represented entirely by a single row.
The row has a column named "instructions" which contains JSON text
representing the request.  This JSON string describes files, text and SQL
data that the user is interested in.  The qmgr fetches the SQL (tabular
human data stored on in the human data server's mysql database), fetches
the files (any type of file, stored on the human data server's local
filesystem) and assembles files from text strings (in the JSON).  Once the
requested data has been assembled, the qmgr then creates a suitably large
TrueCrypt volume (by invoking, via the shell, the truecrypt program,
passing in suitable parameters).  The qmgr manages user encryption keys
via a local mysql database table "fb_keychain" that is managed by the
locally running Drupal module.  The qmgr uses the existing key or creates
a new one if needed.  The qmgr copies the requested files to the TrueCrypt
volume.  The qmgr then sends the single TrueCrypt file to the Hub server
so that the user can download the file.  The user can then download
the file and open it with a TrueCrypt program running on their machine.
The qmgr logs errors to the Hub's "fb_queue" database table by writing to
the same row representing the request.  For each request, there are two
database rows used to communicate: #1-Hub:/mysql/fb_queue/request-row
and #2-HDserver:/mysql/fb_keychain/keys-row.  The qmgr can write to
both tables.  The qmgr writes results summary information (also in JSON
format) to the "results" column in  "fb_queue" and errors and timestamps.
The qmgr writes a key to HDserver:mysql/fb_keychain if necessary.
These two rows represent the primary communication between the Drupal
modules and the qmgr.  The qmgr also makes periodic http requests to
certain URLs on the Hub to perform "still-alive" reporting and also to
report problems communicating with the Hub's mysql database.  These http
requests therefore supplement the communication via the database table row
and allow for graceful operation during database communication failure.

-------------------------------------------------------------------------------
DATABASE NOTES:
-------------------------------------------------------------------------------
The qmgr connects via-JDBC...via-Hibernate to two databases
running on two different hosts, Hub/www-fb_queue and Hd-fb_keychain
-------------------------------------------------------------------------------
DATABASE #1: The first database, running on the
Hub/www, has a table named fb_queue, that looks like this:
-------------------------------------------------------------------------------
mysql> describe fb_queue;
+--------------+--------------+------+-----+---------+----------------+
| Field        | Type         | Null | Key | Default | Extra          |
+--------------+--------------+------+-----+---------+----------------+
| qid          | int(11)      | NO   | PRI | NULL    | auto_increment |
| uid          | mediumint(9) | YES  |     | NULL    |                |
| eid          | mediumint(9) | YES  |     | NULL    |                |
| hash         | mediumtext   | YES  |     | NULL    |                |
| status       | mediumtext   | YES  |     | NULL    |                |
| name         | mediumtext   | YES  |     | NULL    |                |
| description  | mediumtext   | YES  |     | NULL    |                |
| instructions | longtext     | YES  |     | NULL    |                |
| results      | longtext     | YES  |     | NULL    |                |
| hits         | mediumint(9) | NO   |     | 0       |                |
| created      | bigint(20)   | YES  |     | NULL    |                |
| received     | bigint(20)   | YES  |     | NULL    |                |
| completed    | bigint(20)   | YES  |     | NULL    |                |
| accessed     | bigint(20)   | YES  |     | NULL    |                |
+--------------+--------------+------+-----+---------+----------------+
14 rows in set (0.02 sec)
-------------------------------------------------------------------------------
...a sample row, after a request
has been made and processes, looks like this:
-------------------------------------------------------------------------------
mysql> select * from fb_queue LIMIT 1;

| qid | uid  | eid  | hash         | status   | name                 |
description                                               | instructions |
results | hits | created    | received   | completed  | accessed   |

|  37 |  330 |   39 | aqp1e9t2i50y | complete | 3D_Facial_Norms_Data
| Males with ages 3 to 3.5 and European Caucasian ancestry. |
{"files":[{"content":"\/files\/tdfn_gui\/meshes\/5f360aca793fa0379053df26421423be.obj","path":"\/meshes\/5f360aca793fa0379053df26421423be.obj"},{"content":"\/files\/tdfn_gui\/meshes\/bf2eb4da51409428a81ff28e1bafe9b1.obj","path":"\/meshes\/bf2eb4da51409428a81ff28e1bafe9b1.obj"},{"content":"\/files\/tdfn_gui\/meshes\/c096272fe2c45fd41184be1e5a41b0c8.obj","path":"\/meshes\/c096272fe2c45fd41184be1e5a41b0c8.obj"}],"text":[{"content":"Test2","path":"\/test.txt"},{"content":"Dataset:\r3D
Normative Data\r\rVariables:\rmaxcranwidth, minfrntwidth, maxfacewidth,
mandwidth, maxcranlength, cranbasewidth, upfacedepth_r, upfacedepth_l,
midfacedepth_r, midfacedepth_l, lowfacedepth_r, lowfacedepth_l,
morphfaceheight, upfaceheight, lowfaceheight, incanthwidth, outcanthwidth,
palpfislength_r, palpfislength_l, nasalwidth, subnasalwidth, nasalpro,
nasalalalength_r, nasalalalength_l, nasalheight, nasalbdglength,
labfiswidth, philwidth, phillength, uplipheight, lowlipheight,
upvermheight, lowvermheight, cutlowlipheight, n_X, n_Y, n_Z, prn_X, prn_Y,
prn_Z, sn_X, sn_Y, sn_Z, ls_X, ls_Y, ls_Z, sto_X, sto_Y, sto_Z, li_X,
li_Y, li_Z, sl_X, sl_Y, sl_Z, gn_X, gn_Y, gn_Z, en_r_X, en_r_Y, en_r_Z,
en_l_X, en_l_Y, en_l_Z, ex_r_X, ex_r_Y, ex_r_Z, ex_l_X, ex_l_Y, ex_l_Z,
al_r_X, al_r_Y, al_r_Z, al_l_X, al_l_Y, al_l_Z, ac_r_X, ac_r_Y, ac_r_Z,
ac_l_X, ac_l_Y, ac_l_Z, sbal_r_X, sbal_r_Y, sbal_r_Z, sbal_l_X, sbal_l_Y,
sbal_l_Z, cph_r_X, cph_r_Y, cph_r_Z, cph_l_X, cph_l_Y, cph_l_Z, ch_r_X,
ch_r_Y, ch_r_Z, ch_l_X, ch_l_Y, ch_l_Z, t_r_X, t_r_Y, t_r_Z, t_l_X, t_l_Y,
t_l_Z, heightcm, weightkg\r\rVariable Count:\r108\r\rSearch Terms:\rMales
with ages 3 to 3.5 and European Caucasian ancestry.\r\rDate:\rThu,
05 Jan 12 12:38:51 -0500\r\rTotal Individuals:\r3\r\rComplete
Individuals:\r2\r\rPartial Individuals:\r1\r\rTotal Values:\r336\r\rNumber
of Missing Values:\r7\r\rUser:\rzdr4 (Email: zdr4@pitt.edu)
(ID: 330)\r\r","path":"\/Summary.txt"}],"csv":[{"content":"SELECT
maxcranwidth, minfrntwidth, maxfacewidth, mandwidth, maxcranlength,
cranbasewidth, upfacedepth_r, upfacedepth_l, midfacedepth_r,
midfacedepth_l, lowfacedepth_r, lowfacedepth_l, morphfaceheight,
upfaceheight, lowfaceheight, incanthwidth, outcanthwidth, palpfislength_r,
palpfislength_l, nasalwidth, subnasalwidth, nasalpro, nasalalalength_r,
nasalalalength_l, nasalheight, nasalbdglength, labfiswidth, philwidth,
phillength, uplipheight, lowlipheight, upvermheight, lowvermheight,
cutlowlipheight, n_X, n_Y, n_Z, prn_X, prn_Y, prn_Z, sn_X, sn_Y,
sn_Z, ls_X, ls_Y, ls_Z, sto_X, sto_Y, sto_Z, li_X, li_Y, li_Z, sl_X,
sl_Y, sl_Z, gn_X, gn_Y, gn_Z, en_r_X, en_r_Y, en_r_Z, en_l_X, en_l_Y,
en_l_Z, ex_r_X, ex_r_Y, ex_r_Z, ex_l_X, ex_l_Y, ex_l_Z, al_r_X,
al_r_Y, al_r_Z, al_l_X, al_l_Y, al_l_Z, ac_r_X, ac_r_Y, ac_r_Z,
ac_l_X, ac_l_Y, ac_l_Z, sbal_r_X, sbal_r_Y, sbal_r_Z, sbal_l_X,
sbal_l_Y, sbal_l_Z, cph_r_X, cph_r_Y, cph_r_Z, cph_l_X, cph_l_Y,
cph_l_Z, ch_r_X, ch_r_Y, ch_r_Z, ch_l_X, ch_l_Y, ch_l_Z, t_r_X, t_r_Y,
t_r_Z, t_l_X, t_l_Y, t_l_Z, heightcm, weightkg, codedid, sex, age,
ancestry FROM tdfn_storage_data WHERE (((`sex` = 1) AND (((`age` >=
3) AND (`age` < 3.5))) AND ((`ancestry` = 1))));","path":"\/Data.csv"}]} |
{"path":"\/var\/www\/html\/sites\/default\/files\/downloads\/3D_Facial_Norms_Data1325785137.tc","size":"23303636","messages":[]}
|   10 | 1325785131 | 1325785136 | 1325785150 | 1325785342 |
-------------------------------------------------------------------------------
...but notice that the "instructions" column is very large:
-------------------------------------------------------------------------------
mysql> SELECT
qid,uid,eid,hash,status,name,description,results,hits,created,received,completed,accessed
FROM fb_queue LIMIT 1
    -> ;
+-----+------+------+--------------+----------+----------------------+-----------------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------+------+------------+------------+------------+------------+
| qid | uid  | eid  | hash         | status   | name                 |
description                                               |
results | hits | created    | received   | completed  | accessed   |
+-----+------+------+--------------+----------+----------------------+-----------------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------+------+------------+------------+------------+------------+
|  37 |  330 |   39 | aqp1e9t2i50y | complete | 3D_Facial_Norms_Data
| Males with ages 3 to 3.5 and European Caucasian ancestry. |
{"path":"\/var\/www\/html\/sites\/default\/files\/downloads\/3D_Facial_Norms_Data1325785137.tc","size":"23303636","messages":[]}
|   10 | 1325785131 | 1325785136 | 1325785150 | 1325785342 |
+-----+------+------+--------------+----------+----------------------+-----------------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------+------+------------+------------+------------+------------+
1 row in set (0.00 sec)
-------------------------------------------------------------------------------
...and without the "results" column, this table has threadbare data:
-------------------------------------------------------------------------------
mysql> SELECT
qid,uid,eid,hash,status,name,description,hits,created,received,completed,accessed
FROM fb_queue LIMIT 1;
+-----+------+------+--------------+----------+----------------------+-----------------------------------------------------------+------+------------+------------+------------+------------+
| qid | uid  | eid  | hash         | status   | name                 |
description                                               |
hits | created | received   | completed  | accessed   |
+-----+------+------+--------------+----------+----------------------+-----------------------------------------------------------+------+------------+------------+------------+------------+
|  37 |  330 |   39 | aqp1e9t2i50y | complete |
3D_Facial_Norms_Data | Males with ages 3 to 3.5 and European Caucasian
ancestry. |   10 | 1325785131 | 1325785136 | 1325785150 | 1325785342 |
+-----+------+------+--------------+----------+----------------------+-----------------------------------------------------------+------+------------+------------+------------+------------+
1 row in set (0.00 sec)
-------------------------------------------------------------------------------
qid is the id number of this request.  uid is the (Drupal) userid of the
logged-in user who made the request.  eid is the XXX hash is XXX status
holds information describing progress of request processing--possible
values are "request", "pending", "complete", "error"...After processing,
all requests will have a status of either "complete" or "error" name is
the human-readable name of the request.  description is string of prose
describing the request.  hits is a counter ...the last four columns
are timestamps: created received completed accessed As shown earlier,
data found in the columns "instructions" and "results" is complex.
Both columns hold a string of JSON-formatted text.  "instructions" holds
a complete description of the data being requested.  "results" holds
a string describing the full path to the encrypted artifact, its size,
and error information (if there was any trouble fulfilling the request).


A typical lifecycle for a row in fb_queue would look like this:
1. A user logs in and completes steps to request a package of
human data.  
2. ...which causes a row to be created in fb_queue with status='request' 
3. The java qmgr program is polling fb_queue, and finds the new row 
"WHERE status='request';" 
4. qmgr updates fb_queue by setting status='pending' for that row.  
5. qmgr parses the "instructions" field to learn what data is being requested.  
6. qmgr calls HumanDataManager.updateInstructionSize() to tell it how much data
needs to be copied (this lets the Hub know that qmgr might go silent
for a few hours of data copying for a huge request).  
7. qmgr fulfills the request--(i.e. querying a database and packaging resultset
into a CSV text file, writing new text files, gathering on-disk data
files and copying everything to a newly-created-and-mounted TrueCrypt
volume, and copying populated-and-dismounted TrueCrypt volume to its
final location-of-user-retrieval).  
8. qmgr keeps track of errors during processing (i.e. bad queries, missing 
files, failed shell execution attempts, and more).  
9. qmgr checks the list of accumulating errors.
10. If any errors exist, qmgr updates the row in fb_queue with
status="error" and populates the "results" column with details about
the nature of the error.  11. If no errors exist, qmgr updates the row
in fb_queue with status="complete" and populates the "results" column
with the full-path-to-TrueCrypt-file and its size.

The "instructions" JSON formatted string is held in a mysql "longtext"
column (up to 4Gigs of text data).  The format/structure of the JSON
string is like this:

-------------------------------------------------------------------------------
{
    "csv": [
        {
            "content": "SELECT name FROM user", "path": "/Data/User
            Names.csv"
        }, {
            "content": "SELECT uid FROM user", "path": "/Data/User
            IDs.csv"
        }
    ], "text": [
        {
            "content": "Your data was retrieved on 11-02-2011 and has 28
missing values...",
            "path": "/Data/Summary.txt"
        }, {
            "content": "The Facebase Data User Agreement specifies...",
            "path": "/FaceBase Agreement.txt"
        }
    ], "files": [
        {
            "content": "/path/on/server/1101.obj", "path":
            "/meshes/1101.obj"
        }, {
            "content": "/path/on/server/1102.obj", "path":
            "/meshes/1102.obj"
        }
    ]
}
-------------------------------------------------------------------------------
We could describe this data structure in one of the
following ways: ..."a dictionary of arrays of dictionaries"
..."a hash of arrays of hashes" There are three keys in the
outermost dictionary/hash, "text", "sql" and "files", which
hold, respectively, text-to-be-written-to-newly-created-files,
sql-to-be-queried-and-resultrows-to-be-stuffed-into-newly-created-csv-files,
and existing-files-on-disk-to-be-copied.

The example JSON shown above would cause 6 files to be written to the
TrueCrypt volume being created for the user-- 4 newly created files,
including two CSVs that were obtained using the SQL provided, and 2
existing *.OBJ files.

A very large portion of the qmgr code is dedicated to parsing the
"instructions" column's value and fulfilling its wishes.

Notice that the "instructions" column is read by the qmgr.
The qmgr never sets/updates this column.  The "instructions" column
is populated by the Hub/www whenever the facbase.org web surfer
requests a human data download.  Likewise, ONLY the Hub/www writes to
qid,uid,eid,hash,name,description,instructions,hits,created,accessed
qmgr updates/sets these columns: -status--when it first finds a
new row (where status='request') in need of processing (it sets
(status='request' WHERE qid=1234) -status--when processing is finished,
(it sets status='error' or -status='complete" WHERE qid=1234, depending
on where errors occurred during processing or not.  -results--when
processing has finished (it writes a JSON formatted string indicating
the full-path-to-TrueCrypt-file and its size....or error details if
any errors occurred during processing.  -received--sets to unixEpicTime
when qmgr finds a new request -completed--sets to unixEpicTime when qmgr
finishes processing a request There is only one column in fb_queue that
the Hub/www and qmgr BOTH write to--status.  The Hub/www sets it to
"request" and the qmgr sets it to "pending", "complete", or "error"
depending on the current disposition of that record.

The JSON strings are written by org.json.simple.* libraries in qmgr.
Likewise in the PHP running on drupal--these strings are never
manually manipulated.  The structure is NOT flexible--you must have
a hash keyed by "text", "csv" and "files" that MUST contain a list of
dictionaries keyed by "content" and "path".  Because the values in the
"instructions" column are written to by the Hub/www, it is the Hub/www's
responsibility to properly construct this string.  Of course the values
inside the dictionaries must/should be accurate--for example, the SQL
query inside JSON->csv->[0]->content must be a valid SQL query...and the
value inside JSON->files->[0]->content must be a valid path to a file.
All of the values of the key "path" in the "instructions" JSON refer
to the path inside the mounted TrueCrypt volume.  In our example shown
above, the mounted TrueCrypt volume would have a directory named "Data"
and a file file named "Summary.txt" inside the "Data" directory.  "Data"
and "Summary.txt" do not refer to anything on the server filesystem.

The JSON string found in the "results" column of fb_queue is written
by qmgr after processing a request.  If zero errors occurred, then
qmgr will write a JSON string to the "results" column describing the
path and size, as shown in the example above.  If any error occurs, the
qmgr will write to the "logs" and "messages" portion of the "results"
column's JSON value.  If zero errors occur, then "messages" and "logs"
lists will be empty.  It is the responsibility of the qmgr to construct
and write properly-formatted JSON with accurate data to the "results"
column afer processing.

During the processing of a request, the "results" column is typically
updated twice by qmgr.  In the lifecycle described earlier, we mentioned
that the qmgr tells the Hub how much data is about to be copied--it
does this by writing a JSON string holding the "size" component--the
"size" referring to how large the TrueCrypt volume is and thus how
much data is being copied to fulfill this request.  The Hub reads this
"size" inside "results" (during qmgr processing) and thus knows how
long the qmgr might fall silent for.  If the qmgr is copying 30gigs of
data to a TrueCrypt volume, then the Hub will need to know that qmgr is
busy copying a large dataset (and is not otherwise unresponsive/dead).
This write effectively allows the qmgr to tell the Hub, "I'm about to
copy a ton of data, and so you won't hear from me for a long time, but
don't worry, I'm not dead, you'll hear from me in 0.8 hours, if you don't
hear from me before 0.8 hours, then assume I'm dead or stuck".  There is
more information about dead-database managment in the HTTP section below.
-------------------------------------------------------------------------------
DATABASE #2: Hd-fb_keychain
-------------------------------------------------------------------------------
The second database, running on the Hd machine, has a table named
fb_keychain, that looks like this: mysql>  describe fb_keychain;
+----------------+--------------+------+-----+---------+----------------+
| Field          | Type         | Null | Key | Default | Extra          |
+----------------+--------------+------+-----+---------+----------------+
| kid            | int(11)      | NO   | PRI | NULL    | auto_increment |
| uid            | mediumint(9) | YES  |     | NULL    |                |
| encryption_key | mediumtext   | YES  |     | NULL    |                |
| created        | bigint(20)   | NO   |     | 0       |                |
+----------------+--------------+------+-----+---------+----------------+
4 rows in set (0.05 sec)
-------------------------------------------------------------------------------
...with a sample row like this:
-------------------------------------------------------------------------------
mysql> select * from fb_keychain LIMIT 1;
+-----+------+-----------------------+------------+
| kid | uid  | encryption_key        | created    |
+-----+------+-----------------------+------------+
|  19 |  165 | ag7gb4sjq4ap3havtsi1    1325875475 |
+-----+------+-----------------------+------------+
1 row in set (0.01 sec)
-------------------------------------------------------------------------------
...kid is a meaningless primary key.  ...uid is the userid of the (Hub
Drupal) user the key belongs to.  ...encryption_key is the 20-digit key
used to encrypt/decrypt that user's TrueCrypt volumes--this field is set
automatically by qmgr if its empty.  This field can also be set inside
Drupal running on the Hd server by an administrator.  ...created is the
unixEpicTime of its creation.

As mentioned, both the qmgr and Drupal-on-the-Hd server can write to
these columns.

A typical lifecycle for a row in fb_keychain would look like this: 
1. qmgr will attempt to retrieve a row (WHERE uid=1234) 
2. If found, qmgr willuse that row's encryption key value to encrypt data 
3. If no record already exists for that user, then qmgr will generate a new 
20-digit random key for that user and will write a new record to fb_keychain.
4. qmgr will use the key found in steps #2 or #3 to encrypt data.

-------------------------------------------------------------------------------
HTTP calls for out-of-DB-band communication (and for app responsiveness)
-------------------------------------------------------------------------------
Returning on the topic of "are things working properly?", we should
note that all important communication happens in the row in fb_queue
that represents the request that we've been discussing.  There is the
problem of what happens when qmgr cannot obtain a database connection to
query fb_queue.  That database runs on another machine (Hub/www).  No DB
connection means no important communication.  To deal with dead databases,
qmgr also communicates via http using its static httpGetter() method,
which is called from HumanDataController.java in four different places:
-------------------------------------------------------------------------------
httpGetter("event", "1");                //DATABASE IS
DEAD httpGetter("status", "0");               //Hear
again from me soon (0 seconds) httpGetter("status",
timeToMakeString);  //Hear again in timeToMakeString secs
httpGetter("update",iqi.getHash());      //This queue item is completed.
-------------------------------------------------------------------------------
..."event" is how we tell the Hub/www that the database connection
is dead. It is rarely called, the other three calls occur frequently.
..."status" is how we tell the Hub/www how long we expect to be busy
(and therfore silent, i.e. blocking on a copy operation) ..."update"
is how we tell the Hub/www that the request has been processed.
The "update" mechanism is about timing and responsiveness--remember
that the fb_queue.status and fb_queue.results columns are where the
official communication occurs.  The http "update" request is a way to
"kick" the Hub and tell it to query fb_queue to find the (hopefully
status='complete') new information about the request that just finished
getting processed.  All of the http response data from the Hub/www
can be ignored...just make the GET request and get on with your life.
jakarta.commons httpclient will make sure the request gets there as
long as the requests (URLs) are properly constructed.  Some of the
URL-to-be-requested is found in the hd.properties Java properties file
(which would be changed if the Hub's hostname changed, for example).


-------------------------------------------------------------------------------
HUMANDATACONTROLLER.java
-------------------------------------------------------------------------------
The qmgr consists of about a dozen class files.  Its
HumanDataController.java is the most interesting class and contains the
main() method.

Most of the hardcoded strings have been extracted out to a
properties file which is read using a PropertiesConfiguration object
from org.apache.commons.  The first 100 lines of HumanDataController.main() 
are dedicated to extracting values from the java properties file hard-named 
"hd.properties", declaring and initializing variables based on values set 
in hd.properties.  Most of these values are assigned to Strings except for 
'trueCryptParams' which is instead a HashMap<String,String>.  trueCryptParams 
holds parameters related to TrueCrypt operation and packing them into a 
HashMap keeps the tidy.  If there are any problems reading hd.properties, the 
program won't crash, but an error will be generated which will cause the 
first user request to get "status"="error" even if things otherwise go well.  
Setting accurate and correct values in hd.properties is absolutely critical.  
Even if hd.properties is read and parsed successfully, the values set in there 
must be accurate.  For example, the value of "trueCryptBin" in hd.properties 
cannot be "/usr/bin/troocript".  Further, even if all of the data in the 
hd.properties file is correct, the execution environment must match the 
expectations--for example, if hd.properties says "trueCryptBin=/bin/truecrypt" 
then truecrypt must be installed locally and available at /bin/truecrypt.  


Values for these property names are read from hd.properties:
-------------------------------------------------------------------------------
sleepFor--how long to sleep between querying Hub/www-fb_queue for new
requests hubURL--the URL hostname target of qmgr's http requests,
scheme included.  responseTrigger--beginning of the path of the
Hub's URL targeted by qmgr hdDbUser--mysql username on Hd server
hdPasswd--mysql passwd on Hd server hdJdbcUrl--JDBC url o  Hd server,
engine/host/port/dbname fbDbUser--mysql username on Hub server
fbPasswd--mysql passwd on Hub server fbJdbcUrl--JDBC url o  Hub
server, engine/host/port/dbname scpBin--location of scp binary on
localhost sshServerUrl--destination user@hostname string used to scp
copy files finalLocation--full path to final location of TrueCrypt
file on destination touchBin--full path to bin binary on localhost
trueCryptBasePath--writable scratch space for file copying (/tmp,
/var/tmp) trueCryptExtension--the filename extension of TrueCrypt
files trueCryptMountpoint--directory (under trueCryptBasePath)
to mount TrueCrypt volume beneath trueCryptBin--full path to
truecrypt binary on localhost algorithm--truecrypt parameter
hash--truecrypt parameter filesystem--truecrypt parameter
volumeType--truecrypt parameter randomSource--truecrypt parameter
protectHidden--truecrypt parameter extraArgs--truecrypt parameter
-------------------------------------------------------------------------------
qmgr's hd.properties file is extremely finicky and not friendly.
Shown below is an actual working hd.properties file--notice carefully
the character escapes and unusual format of some of the values:
-------------------------------------------------------------------------------
sleepFor=5 hubURL=http\://maher-hp.sdmgenetics.pitt.edu
responseTrigger=/fb_queue_response/ trueCryptBasePath=/var/tmp/
trueCryptExtension=.tc trueCryptMountpoint=/mountpoint/ hdDbUser=drupalone
hdPasswd=sekret hdJdbcUrl=jdbc\:mysql\://localhost\:3306/drupalone
fbDbUser=facebase7dev fbPasswd=sekret
fbJdbcUrl=jdbc\:mysql\://maher-hp.sdmgenetics.pitt.edu\:3306/facebase7devdb
trueCryptBin=/usr/bin/truecrypt scpBin=/usr/bin/scp
sshServerUrl= root@maher-hp.sdmgenetics.pitt.edu\:
finalLocation=/var/www/html/sites/default/files/downloads/
touchBin=/bin/touch algorithm=AES hash=RIPEMD-160
filesystem=FAT volumeType=Normal randomSource=/dev/random
protectHidden=no extraArgs=--non-interactive
-------------------------------------------------------------------------------
...not pretty. hd.properties needs to be on the CLASSPATH.  A good
place for it is just underneath the "classes" directory in a standard
java deployment.

-------------------------------------------------------------------------------
ERRORS
-------------------------------------------------------------------------------
HumanDataController.java defines at static method addError: 
addError(String error ,String log)
...that all the classes in the application use to set serious errors.  A serious 
error is one that will cause a request to go unfulfilled.  IO failures, bad 
SQL queries, and other semi-catastrophic problems will cause addError to be 
called with two Strings, the first is a human readable description and the 
second String argument to addError() is detailed failure reporting (like you 
might get from a java.Exception.addMessage() call).  addError() writes to the 
two static ArrayList<String> variables named "logs" and "errors" defined in 
HumanDataController.java.  When the request processing has finished, it checks 
to see if errors.isEmpty().  If there is even one error, then the request 
processing effort is considered a failure and the row in fb_queue for that 
request (WHERE qid=1234) has its status column value set to "error".  This 
will cause the Drupal application to execute its "request-failed" logic.  
Therefore, addError() is only called in situations where a very bad thing has 
happened.  addError() can be called from many places.  It can be called 
multiple times during the processing of one request (in which case the 
ArrayList "error" would have multiple members).  

-------------------------------------------------------------------------------
LOGGING
-------------------------------------------------------------------------------
For logging, the qmgr uses log4j, which reads
log4j.properties, which must also be on the CLASSPATH next
to hd.properties.  This application requires log4j version
1.2.  Here is a real log4j.properties file that works for qmgr:
-------------------------------------------------------------------------------
# All logging output sent to standard out and a file #
WARN is default logging level log4j.rootCategory=WARN,
STDOUT, FILE # Application logging level is DEBUG
log4j.logger.edu.pitt.dbmi.facebase.hd=DEBUG # Configure the Standard
Out Appender log4j.appender.STDOUT=org.apache.log4j.ConsoleAppender
log4j.appender.STDOUT.layout=org.apache.log4j.PatternLayout
log4j.appender.STDOUT.layout.ConversionPattern=%5p
(%F:%L) %m%n # Configure a rolling file appender
log4j.appender.FILE=org.apache.log4j.RollingFileAppender
log4j.appender.FILE.File=output.log log4j.appender.FILE.MaxFileSize=2000KB
log4j.appender.FILE.MaxBackupIndex=5
log4j.appender.FILE.layout=org.apache.log4j.PatternLayout
log4j.appender.FILE.layout.ConversionPattern=%d %-5p %c - %m%n
-------------------------------------------------------------------------------
...this is in "DEBUG" mode and will be very chatty.  Changing "DEBUG"
to "WARN" in the file is a good idea for two reasons: 1. WARN will only
report serious problems to the logfile.  2. DEBUG passes sensitive
data (i.e. keyphrases) to the logfile.  This log4j.properties file
will log output to two places: 1. The console where the program was
started (where 'java' was typed) 2. To a file named output.log This
log4j file will only permit output.log to grow to 2megabytes before
rotating in a new, empty output.log file.  It will keep up to 5 backup
logfiles.  Thus, we are only keeping at most 10megabytes of logfile data.
-------------------------------------------------------------------------------
PASSWORDLESS LOGIN
-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
PATH ENVIRONMENT VARIABLE SECURITY
-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
JAR REQUIREMENTS
-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
PACKAGING AS AN EXECUTABLE JAR
-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
RESULTSET-TO-CSV OPTIONS
-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
TRUECRYPT OPERATION
-------------------------------------------------------------------------------

-------------------------------------------------------------------------------
-------------------------------------------------------------------------------
TROUBLESHOOTING:
-------------------------------------------------------------------------------
1. qmgr is extremely tightly-coupled with the Drupal modules and
their databases.  Don't bother trying to do anything without two
Drupal installations with working modules Hub/www-fb_queue,fb_access
and Hd-fb_keychain,fb_bridge.  2. Use the Hd-Drupal interface
for managing encryption keys, use the Hub/www-Drupal interface
for everything else.  3. Except for editing hd.properties and
log4j.properties, and reading the console logging (also seen in the
logfile "output.log"), there are not ways to interact with the qmgr.
4. The qmgr expects alot of things to be in place, including:
--passwordless login --accurate hd.properties values --firewall
access to ports 80/443 and 3306 on Hub/www --username/pass login
permissions on both databases.  --truecrypt,scp and touch on localhost
-------------------------------------------------------------------------------
EXTENSION NOTES:
-------------------------------------------------------------------------------
One typical extension would be to add
a human-data-related application to the Hub/www
-------------------------------------------------------------------------------
VERBATIM log.debug() MESSAGES FROM HUMANDATACONTROLLER.java:
-------------------------------------------------------------------------------
"propertiesfileloadedsuccessfully");
"initializestaticclassvariableHumanDataManagerdeclaredearlier");
"declareandinitializeInstructionQueueManager");
"passtothelogfile/consoleallstartupparametersfortroubleshooting");
"EnterinfiniteloopwhereprogramwillcontinuouslypollHubserverdatabasefornewrequests");
"LOOPSTART");
"AbouttoinvokeInstructionQueueManager.queryInstructions()--Hibernatetofb_queuestartsNOW");
"Currentlythereare"+aiqi.size()+"itemsinthequeue");
"Abouttosendhttprequest-status-tellingHubwearealive:");
"Thereisatleastonerequest,status=pending,queueitem;commenceprocessingofmostrecentitem");
"Abouttogetexistinguserkey,orcreateanewone,viafb_keychainHibernate");
"AbouttopulltheJSONInstructionsstring,andotheritems,fromtheInstructionQueueItem");
"AbouttocreateanewFileManagerobjectwith:");
instructionName+trueCryptBasePath+trueCryptExtension+trueCryptMountpoint);
"FileManager.makeInstructionsObjects()createsmultipleInstructionobjectsfromtheInstructionQueueItem.getInstructions()value");
"FileManager.makeInstructionsObjects()returnedtrue");
"FileManager.makeFiles()usesitslistofInstructionobjectsandcallsitsmakeFiles()methodtomake/getrequesteddatafiles");
"FileManager.makeFiles()returnedtrue");
"Sendhttprequest-status-toHubwithtotalcreationtimeestimate:");
timeToMakeString);
"Updatethequeue_itemrowwiththetotalsizeofthedatabeingpackagedwithInstructionQueueManager.updateInstructionSize()");
"InstructionQueueManager.updateInstructionSize()returnedtrue");
"AbouttomakenewTrueCryptManagerwiththeseargs:");
key.getEncryption_key()+fm.getSize()+fm.getTrueCryptPath()+fm.getTrueCryptVolumePath()+trueCryptParams);
"TrueCryptManager.touchVolume()returnedtrue,touchedfile");
"TrueCryptManager.makeVolume()returnedtrue,createdTrueCryptvolume");
"TrueCryptManager.mountVolume()returnedtrue,mountedTrueCryptvolume");
"TrueCryptManager.copyFilesToVolume()returnedtrue,copiedrequestedfilestomountedvolume");
"TrueCryptManager.disMountVolume()returnedtrue,umountedTrueCryptvolume");
"TrueCryptManager.sendVolumeToFinalLocation()returnedtrue,copiedTrueCryptvolumetoretreivable,finallocation");
"InstructionQueueManager.updateInstructionToCompleted()returnedtrue");
"Processingofqueueitemisalmostfinished,updatedfb_queueitemrowwithlocation,size,status,errors,logs:");
tcm.getFinalLocation()+fm.getTrueCryptFilename()+fm.getSize()+iqi.getQid()+getErrors()+getLogs());
"Abouttosendhttprequest-update-tellingHubwhichitemisfinished.");
"Finishedprocessingpendingqueueitem,statusshouldnowbecompleteorerror");
"Zeroqueueitems"); "LOOPEND");


REQUIRED LIBRARIES: The md5 checksums of libraries used to compile the program:
9a4a5af606dadf98a958a8dc22a903fb  c3p0-0.9.1.jar
5bad7a091f94065b05d6d778fee65353  commons-attributes-api.jar
fca6b08002f7075d60ab5c65c9949756  commons-cli.jar
b638c639bd95c8e3b16eb3283dca9586  commons-collections3.jar
0005907480f12ee461cb4cec25af61ea  commons-configuration-1.7.jar
b4c480853c782d7e5370887a6d785f01  commons-csv.jar
07c7573b6db1b2a7b565b343e6d46d34  commons-dbutils-1.4.jar
1551d32e3c8f72751e47cf7663480458  commons-io.jar
4d5c1693079575b362edf41500630bbd  commons-lang-2.6.jar
6b62417e77b000a87de66ee3935edbf5  commons-logging-1.1.jar
4d8f51d3fe3900efc6e395be48030d6d  dom4j-1.6.1.jar
c61a19d88aec7deae74f148ebf57a688  geronimo-spec-jta-1.0.1B-rc4.jar
4f568d01d551f2c3c11521cbc8c37e3a  hibernate3.jar
8796fc5e75a26bc83fd7fc138cdbf8b1  hibernate-c3p0-3.6.2.Final.jar
8ae1ea6c2a3d854c6436f6f70e04f699  hibernate-commons-annotations-3.2.0.Final.jar
d7e7d8f60fc44a127ba702d43e71abec  hibernate-jpa-2.0-api-1.0.1.Final.jar
98b83db263880dc3bd9fd234ac4b8802  javassist.jar
eb342044fc56be9ba49fbfc9789f1bb5  json_simple-1.1.jar
f9637cad64a97cdce4156fddea04c19a  jta-1.1.jar
4a11e911b2403bb6c6cf5e746497fe7a  log4j-1.2.16.jar
78467fb2adf7f02bcfbff3ad022bc4e9  mysql-connector-java-5.1.18-bin.jar
40e6c51f95c7d24f30e24f39f02f2615  opencsv-2.3.jar
e411b9d204b1a91d62b830a86e1f44ff  org.apache.commons.codec_1.3.0.v201101211617.jar
2d24de59066d66b15a94f3d0760175d8  org.apache.commons.codec.source_1.3.0.v201101211617.jar
169fea92000c62622dd87e567b6c04a7  org.apache.commons.httpclient_3.1.0.v201012070820.jar
2758c5772f67f30cdf76cfdd90519162  slf4j-api-1.5.5.jar
20bed4f0cf6028b1ca5be5f470fa77a9  slf4j-simple-1.5.5.jar
