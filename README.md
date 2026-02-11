# MatchingEngine

A high-performance trading platform with real-time order matching, WebSocket updates, and event-driven architecture.

## Architecture

**Backend**: Spring Boot (Java) with Kafka event streaming  
**Frontend**: React + TypeScript with real-time WebSocket integration  
**Database**: PostgreSQL with Redis caching  
**Deployment**: Railway (backend) + Vercel (frontend) + Supabase (database) + Confluence (kafka) + Redis 

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (Vercel)                        │
└───────────────┬─────────────────────────────────────────────┘
                │
                ├──── POST /api/orders → Ingress Server Port 8085
                │
                └──── GET /api/orderbook → Any Server
                              |
                              ▼
                            Kafka
                              │
      ┌─────────────────────--┼───────────────────-─┐
      │                       │                     │
┌─────▼────--┐        ┌───────▼────-┐        ┌──────▼─────┐
│ Server 1   │        │ Server 2    │        │ Server 3   │
│ Port 8080  │        │ Port 8081   │        │ Port 8082  │
└─────┬──────┘        └────-─┬──────┘        └──────┬─────┘
      │                      │                      │
      └────────────────────-─┼─────────────────────-┘
                             │
                     ┌───────▼────────┐
                     │  Redis Cache   │
                     │                │
                     │  OrderBooks:   │
                     │  - AAPL        │
                     │  - GOOGL       │
                     │  - ...         │
                     └────────────────┘
```

## Features

- **Real-time Order Matching**: Fast price-time priority matching engine
- **WebSocket Updates**: Live order book and trade notifications via STOMP/SockJS
- **Event-Driven**: Kafka-based messaging for scalability and resilience
- **User Management**: JWT authentication with session management
- **Portfolio Tracking**: Real-time balance and position monitoring

## Project Structure

```
├── backend/              # Spring Boot application
│   ├── src/main/java/    # Java source files
│   └── src/main/resources/ # Configuration files
├── trading-frontend/     # React TypeScript application
    └── src/              # Frontend source files
```


## Tech Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL + Flyway migrations
- **Caching**: Redis with Lettuce client
- **Messaging**: Apache Kafka
- **WebSocket**: STOMP over SockJS
- **Authentication**: JWT + Spring Security

### Frontend
- **Framework**: React 18 + TypeScript
- **WebSocket**: @stomp/stompjs + sockjs-client
- **Routing**: React Router v6
- **Charts**: Recharts
- **Build**: Create React App
