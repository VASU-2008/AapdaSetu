import express from "express";
import cors from "cors";
import cookieParser from "cookie-parser";
import authRouter from "./routes/auth.route.js";
import alertRoutes from "./routes/alertRoutes.js";

const app = express();

app.use(cors());
app.use(express.json());
app.use(cookieParser());

// API Routes
app.use("/api/v1/auth", authRouter);
app.use("/api/v1", alertRoutes);

// Health Check Endpoint
app.get("/health", (req, res) => {
    res.status(200).json({
        status: "UP",
        message: "AapdaSetu Backend API is running",
        timestamp: new Date().toISOString()
    });
});

export default app;
