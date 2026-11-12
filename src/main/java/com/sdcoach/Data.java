package com.sdcoach;
import java.util.*;
public class Data {
  public record Stage(String id,String name,String q){}
  public record Problem(String id,String title,String brief,List<String> tags){}
  public record Concept(String pick,String use,String avoid){}
  public record Drill(String s,List<String> o,int a,String why){}
  public static final List<Stage> STAGES=List.of(
    new Stage("requirements","Requirements","Before drawing anything: what are the functional and non-functional requirements? Who are the users and what must never break?"),
    new Stage("estimation","Estimation","Estimate scale. Use the calculator: users, requests per user, read/write ratio, object size. What do QPS and storage tell you?"),
    new Stage("design","High-level design","Walk me through the components and the request path. Why each component, and what data store did you pick and why?"),
    new Stage("deepdive","Deep dive","Pick the hardest component and go deep: schema, keys, API, and how it stays correct under concurrent use."),
    new Stage("bottlenecks","Bottlenecks","Traffic grows 10x and your cache node dies. What breaks first, and how do you fix it without downtime?"));
  public static final List<Problem> PROBLEMS=List.of(
    new Problem("url","URL shortener","Design a service like bit.ly.",List.of("hashing","caching","databases","scaling-reads")),
    new Problem("feed","News feed","Design an Instagram-style home feed.",List.of("fanout","caching","queues","sharding")),
    new Problem("chat","Chat app","Design WhatsApp-style messaging.",List.of("websockets","queues","consistency","sharding")),
    new Problem("rate","Rate limiter","Design a distributed API rate limiter.",List.of("caching","consistency","scaling-reads")));
  public static final Map<String,Concept> KB=Map.of(
    "caching",new Concept("Redis / Memcached","Reads dominate, same keys repeat, latency target is tight.","Data must always be fresh or each key is read once."),
    "databases",new Concept("Postgres vs Cassandra/DynamoDB","Postgres for relations and transactions; Cassandra/DynamoDB for huge key-based, write-heavy workloads.","Wide-column stores when you need joins or multi-row transactions."),
    "hashing",new Concept("Base62 of counter/ID, or hash + collision check","You need short unique keys without a lookup on write.","Plain random keys at scale without collision handling."),
    "scaling-reads",new Concept("Read replicas, CDN, cache","Read:write ratio is 10:1 or more.","Replicas when reads must see the latest write."),
    "fanout",new Concept("Fan-out on write vs on read","On write for normal users (fast reads); on read for celebrities (avoid millions of writes).","Pure fan-out on write for accounts with huge followings."),
    "queues",new Concept("Kafka vs RabbitMQ vs SQS","Kafka for ordered, replayable high-throughput streams; RabbitMQ/SQS for task queues with simple retries.","A queue on a path that needs a synchronous answer."),
    "sharding",new Concept("Hash or range sharding with consistent hashing","One node can no longer hold the data or write load.","Sharding early; it makes joins and transactions hard."),
    "websockets",new Concept("WebSocket gateway + pub/sub","Server must push updates with low latency (chat, presence).","Plain polling at large scale, or sockets for rare updates."),
    "consistency",new Concept("Strong vs eventual; CAP trade-off","Strong for money and counters; eventual for feeds and likes.","Claiming strong consistency with async replicas."));
  public static final List<Drill> DRILLS=List.of(
    new Drill("10M writes/day, events must be ordered per user, three services consume the same stream.",List.of("Kafka","SQS","Redis cache","Postgres"),0,"Kafka keeps ordered, replayable partitions and lets many consumer groups read the same stream. SQS does not give multi-consumer replay."),
    new Drill("Profile pages read 50x more than they are written and the same users get hit repeatedly.",List.of("Cassandra","Redis cache","Kafka","More write replicas"),1,"Repeated hot reads are the textbook cache case. Watch invalidation on update."),
    new Drill("Celebrity with 50M followers posts; feed must stay fast for everyone.",List.of("Fan-out on write for all","Fan-out on read for celebrities","One giant SQL join","Poll every second"),1,"Writing to 50M inboxes is too heavy. Merge celebrity posts at read time."),
    new Drill("Bank balance transfers between two accounts must never double-spend.",List.of("Eventual consistency","Cache-aside","ACID transaction in Postgres","Fire-and-forget queue"),2,"Money needs atomic, isolated, durable writes."));
  public static final List<String> COMPONENTS=List.of("Client","Load balancer","API server","Redis","Postgres","Cassandra","Kafka","CDN","Object store","WebSocket gateway","Worker");
}
