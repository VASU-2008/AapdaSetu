import userModel from "../models/user.model.js";
import jwt from "jsonwebtoken";


const authMiddleware = async (req, res, next) => {
    const token = req.cookies.JWT_TOKEN || req.headers.authorization?.split(" ")[1]
    console.log(token);


    if (!token) {
        return res.status(401).json({
            message: "Unauthorized Access Token Is Missing"
        })
    }

    try {


        const decoded = jwt.verify(token, process.env.JWT_SECRET)

        const user = await userModel.findById(decoded.userID)
        req.user = user
        return next();

    } catch (error) {
        console.log("Error Happens", error);
        return res.status(401).json({
            message: "Token Is Invalid"
        })
    }
}

const authSystemUserMiddleware = async (req, res, next) => {
    const token = req.cookies.JWT_TOKEN || req.headers.authorization?.split(" ")[1]

    if (!token) {
        return res.status(401).json({
            message: "Unauthorized Access Token Is Missing"
        })
    }

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET)
        console.log("Decoded:", decoded);

        const user = await userModel.findById(decoded.userID).select("+systemUser")
        if (!user.systemUser) {
            return res.status(403).json({
                message: "Forbidden access, not a system user"
            })
        }

        req.user = user
        return next()
    } catch (error) {
        console.log("Error Happens", error);
        return res.status(401).json({
            message: "Token Is Invalid"
        })
    }
}
export default { authMiddleware, authSystemUserMiddleware };