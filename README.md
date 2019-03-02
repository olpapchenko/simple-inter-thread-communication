[![Build Status](https://travis-ci.com/rocketmannot/simple-inter-thread-communication.svg?branch=master)](https://travis-ci.org/rocketmannot/simple-inter-thread-communication) [![codecov](https://codecov.io/gh/rocketmannot/simple-inter-thread-communication/branch/master/graph/badge.svg)](https://codecov.io/gh/rocketmannot/simple-inter-thread-communication)

# simple-inter-thread-communication  
Simple inter-thread communication facility, mimics Disruptor concept. Created for academic purposes.  
  
This project created solely in academic purposes - mainly to better understand concepts behind LMAX/Disruptor.  
The implementation is tiny - only a few classes - however, it uses the same concepts used in original Disruptor lib.  
This lib should not be used in production.  
  
The goal is to write a simple data structure that enables to exchange data between threads. In Java API you can use ***BlockingQueue*** collections. However, these collections  
are implemented using plain locks. Acquire/release lock both have big price especially under big contention. The solution is to use lock free algorithm based on low level memory barriers concepts. 
  
## Low-level API used in the implementation  
For optimization CPU can reorder operations. To force order of operation performed with memory we should use memory barriers. 
Implementation is lock-free. Mainly based on memory barriers. Let's define them:  
  
-  **storestore barrier** - all store operations defined before barrier are guaranteed to happen before all subsequent stores. Note: for other threads the value is not guaranteed to be visible instantly 
-  **storeload barrier** - all stores before barrier happen before all  
loads, also this barrier guarantees that all store operations before barrier are  
visible to all other threads  
-  **loadload barrier** - load operations before barrier a not reordered with  
load operations after barrier  
**CAS (compare and set)** - special CPU instruction, has two arguments, first argument - expected value, second argument- new value  
This CPU instruction enables atomically compare value and if current value equals to expected value - set operation  is performed.
  
## Java API used in implementation.  
Memory barriers are not presented in Java API. However we can use it through high level  APIs.  
For memory barriers, we can use ***volatile*** keyword and ***Unsafe*** class. For **CAS** operations - ***java.util.concurrent.atomic.Atomic**** classes  
Let's clarify how barriers are presented in Java API:  
  
- **Volatile write** - is follower by **storeload  mem barrier** -  
which means that write to the volatile var is instantly visible to  
other threads.  
- **Volatile read** is acting as **loadload barrier**  
  
In such a way **volatile var write** is working as monitor release and **volatile read** as monitor acquire.  
For details see: http://psy-lob-saw.blogspot.com/2014/08/the-many-meanings-of-volatile-read-and.html  
***java.util.concurrent.atomic.Atomic*.lazySet*** - acts as storestore  barrier  
  
## Requirements  
This lib enables one producer thread to share data to several consumer threads.  
  
As primary data structure is used ring buffer. Ring buffer in our case is just an array. Ring buffer is convenient, because to track next element we can use one pointer -  number of the long type. We can then get actual position of the element on the array by ***(pointer mod array_size)***  
  
Entries of the ring buffer are preallocated. The preallocation here is performed not to save time on object allocation. In Java object allocation can be even faster  
than malloc operation in C. Preallocation here is used to prevent creation of new entries each time - thus producing a huge amount of garbage - as the result we can experience latency spikes due to  
JVM garbage collection routine - to prevent that we use preallocation of entries.  
  
When producer wants to publish new entry, it should perform two steps:  
  
1. Acquire current position on the ring buffer free to be written. We  
have to check here that we are not wrapping out the ring buffer -  
all consumers should consume the event to be overwritten by  
producer.  
  
2. After acquiring a new position - producer copies all data to the entry  
from the ring buffer  
  
3. Producer performs publication on new changes by updating the pointer to  
the next available for consumer slot followed by ****storestore  barrier**  
  
Consumer:
1. Is waiting for the next available entry on the ring buffer - in this implementation, the only available strategy is a busy spin.  
2. Read next available for consumption sequence number (read is followed by **loadload barrier**)  
3. Tries to process already read entry  
4. Performs update of the consumer sequence number followed by **storestore  barrier**
