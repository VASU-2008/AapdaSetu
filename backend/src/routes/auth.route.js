import express from "express";
import { registerUser, loginUser, getMe, logoutUserController } from "../controllers/auth.controller.js";
import { authMiddleware } from "../middleware/auth.middleware.js";

const authRouter = express.Router();

authRouter.post("/register", registerUser);
authRouter.post("/login", loginUser);
authRouter.get("/me", authMiddleware, getMe);
authRouter.get("/logout", logoutUserController);

export default authRouter;