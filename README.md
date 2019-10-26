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

Furthermore, publishing an event creates a memory barrier to make sure the caches are up to date with this event. 
It allows adding an event in the ring buffer structure, without any locking which gives us a huge performance improvement.

So when you publish event from netty threads using disruptor (ringBuffer.publish) 
it will be transfered to receiver dusruptor thread with full correctnes in therms of concurrency.

Also disruptor doesnt use any locks inside, thats why its also very fast and garbage free.
This framework is heavily use in High-Frequency trading applications.

https://itnext.io/understanding-the-lmax-disruptor-caaaa2721496

Pros:
- thread safe
- very fast network
- almost zero allocations
- low gc preasure
- no lock, no syncronisation

Cons:
- hard read
- hard to test
- hard to maintain
- hard to extend

Use it only if you really need high load and perfromance.
I also created more simplified version of this task, which is easy to read and undestand:
https://github.com/vlyutenko/transfer-processor-simple


