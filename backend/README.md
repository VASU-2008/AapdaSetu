# AapdaSetu Backend API

Node.js & TypeScript Express service for **AapdaSetu** disaster management and emergency relief app.

## Features
- **Express & TypeScript**: Type-safe REST API server
- **Security Middleware**: CORS & Helmet security headers
- **Disaster Alerts API**: Endpoints to publish and retrieve active emergency alerts

## Getting Started

### Installation
```bash
cd backend
npm install
```

### Development Server
```bash
npm run dev
```

### Build & Production Run
```bash
npm run build
npm start
```

## API Endpoints
- `GET /health` - Service health status
- `GET /api/v1/alerts` - List active disaster alerts
- `POST /api/v1/alerts` - Create a new disaster alert
