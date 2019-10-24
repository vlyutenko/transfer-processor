# transfer-processor

Some explanation for my solution:

As http server I used netty so all incoming requests are served by Netty threads (3 worker threads),
then for managing thread-safety for data access I use cycle buffer - Disruptor from LMAX.
This cycle buffer support Multiple producers - Single consumer mode;

So main data storage application receice AccountEvents from multiple netty threads,
but all these events are handled in one and only one same single thread (Disruptor receiver thread). 

Thats why I use simple (not thread safety map) as a storage because access to this map is always perfromed 
from the same Disruptor Thread. (I have different types of events like TRANSFER, INFO, ADD but still they are
served in the SAME Disruptor thread.

Disruptor provides safe publications for events and thats why is thread safe.
Also disruptor doesnt use any locks inside, thats why its also very fast and garbage free.
This framework is heavily use in High-Frequency trading applications.
