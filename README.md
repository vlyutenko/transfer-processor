# transfer-processor

Some explanation for my solution:
As http server I used netty so all incoming requests are served by Netty threads,
then for manage thread-safety I user cycle buffer - Disruptor from LMAX,
its cycle buffer which wokrs in my situation in multiple producers - single consumer mode;
So application get requests from multiple netty threads and submited events to disruptor,
which handle all events in one and only one single thread, 
thats why I use simple (not thread safety map) as storage because access to this storage is always from one Thread.
disruptor provides safe publications for events and thats why is thread safe. 
