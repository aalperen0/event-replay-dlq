I did this project for demonstration purpose, there are no concrete events(just simulated). Incoming events from the client could be erroneous, corrupted data or 
any other type of issue. For example, application can not process due to incomplete information. Dead-letter queue act as a temporary storage for these 
type of data.

Applied retry-mechanism using exponential-backoff and distributed lock using redis. I used a single instance, but in a 
distributed environment using consensus-based lock would be better. I used redis-lock because of avoid duplicate concurrent processing for efficiency.
So, after max attempt reached, the event moved to dead-letter queue. Then, if  admin wants to identify the causes of the errors, it can change the data,
apply the fixes, and perform new attempts with replay-event. 

<img width="1422" height="747" alt="DLQ" src="https://github.com/user-attachments/assets/417206eb-76f8-4661-bc28-5d834eafaef5" />


## 🔄 How It Works

### Normal Flow

1. Client publish event via REST api and Event Saved to database with PENDING status
2. Event published Kafka event-topic and consumer pick up events with redis-lock
3. Event processed by appropriate processor
4. Status updated to COMPLETED

### Error Flow With Retry
1. Processing fails -> captured in EventProcessingLog
2. Retry scheduled with exponential backoff
3. After retry: success -> COMPLETED, failure -> next retry
4. After max attempts -> moved to DLQ
5. Email notification sent to admin

### Replay Flow 

1. Admin reviews DLQ events and modifies event data
2. Apply replay(single or batch)
3. Event processed from DLQ: success -> removed from DLQ failure -> remains in DLQ
4. Optionally archive events

### Tech Stack

<div align="left">
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/java/java-original.svg" height="40" alt="java logo"  />
  <img width="12" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/apachekafka/apachekafka-original.svg" height="40" alt="apachekafka logo"  />
  <img width="12" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/docker/docker-original.svg" height="40" alt="docker logo"  />
  <img width="12" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/redis/redis-original.svg" height="40" alt="redis logo"  />
  <img width="12" />
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/postgresql/postgresql-original.svg" height="40" alt="postgresql logo"  />
</div>


## ✨ Key Features

### Event Processing
- ✅ Asynchronous event processing with Kafka
- ✅ Automatic retry mechanism with exponential backoff
- ✅ Distributed locking with Redis (prevents duplicate processing)
- ✅ Comprehensive event processing logs

### Dead Letter Queue (DLQ)
- ✅ Automatic DLQ routing after max retry attempts
- ✅ Event status tracking (PENDING → PROCESSING → COMPLETED/DLQ)
- ✅ Failure reason logging and analysis
- ✅ Manual event replay capability

### Other Operations
- ✅ View active DLQ events
- ✅ Manual event replay
- ✅ Batch replay sessions
- ✅ Event data modification before replay



### Key Learnings
- Importance of idempotency in event processing
- Redis distributed locks for preventing duplicate processing
- Kafka consumer group management

## API Endpoints
### Event Publishing
```http
# Publish Event
POST /api/events/publish

# Get all events
GET /api/events

# Get specific event 
GET /api/events{eventId}

# Get event status
GET /api/events{eventId}/status

# Update event
PATCH /api/events/{eventId}
```

### DLQ Management
```http
# Get active DLQ events
GET /api/dlq

# Get specific DLQ event
GET /api/dlq/{eventId}

# Archive DLQ Event
POST /api/dlq/{eventId}/archive

# Retry DLQ Event
POST /api/dlq/{eventId}/retry
```

### Replay Session

```http
# Create replay session
POST /api/replay/sessions

# Start replay session
POST /api/replay/sessions/{sessionId}/start

# Retrieve all replay events
GET /api/replay/events

# Get Replay Progress
GET /replay/progress/{sessionId}
```



## 👤 Author

**Alperen Yücel**


