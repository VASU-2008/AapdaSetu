import userModel from "../models/user.model.js";
import jwt from "jsonwebtoken";

const authMiddleware = async (req, res, next) => {
    const token = req.cookies.token || req.cookies.JWT_TOKEN || req.headers.authorization?.split(" ")[1];

    if (!token) {
        return res.status(401).json({
            message: "Unauthorized: Access token is missing"
        });
    }

    try {
        const decoded = jwt.verify(
            token, 
            process.env.JWT_SECRET || "f5fe783dde4c41eadb1d15628f58836db510d2a5681b6170fb49e54699da8fad"
        );

        const userId = decoded.id || decoded.userID;
        const user = await userModel.findById(userId).select("-password");

        if (!user) {
            return res.status(401).json({
                message: "User no longer exists"
            });
        }

        req.user = user;
        return next();
    } catch (error) {
        console.error("Auth Middleware Error:", error.message);
        return res.status(401).json({
            message: "Token is invalid or expired"
        });
    }
};

const authSystemUserMiddleware = async (req, res, next) => {
    const token = req.cookies.token || req.cookies.JWT_TOKEN || req.headers.authorization?.split(" ")[1];

    if (!token) {
        return res.status(401).json({
            message: "Unauthorized: Access token is missing"
        });
    }

    try {
        const decoded = jwt.verify(
            token, 
            process.env.JWT_SECRET || "f5fe783dde4c41eadb1d15628f58836db510d2a5681b6170fb49e54699da8fad"
        );

        const userId = decoded.id || decoded.userID;
        const user = await userModel.findById(userId).select("+systemUser");

        if (!user || !user.systemUser) {
            return res.status(403).json({
                message: "Forbidden access: Requires system user permissions"
            });
        }

        req.user = user;
        return next();
    } catch (error) {
        console.error("System Auth Middleware Error:", error.message);
        return res.status(401).json({
            message: "Token is invalid or expired"
        });
    }
};

export { authMiddleware, authSystemUserMiddleware };
export default { authMiddleware, authSystemUserMiddleware };