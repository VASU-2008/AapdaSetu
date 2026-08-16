import userModel from "../models/user.model.js";
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

const JWT_SECRET = process.env.JWT_SECRET || "f5fe783dde4c41eadb1d15628f58836db510d2a5681b6170fb49e54699da8fad";

const registerUser = async (req, res) => {
    try {
        let { username, fullName, email, password } = req.body;

        // Auto-generate username from fullName or email if not provided
        if (!username) {
            username = fullName 
                ? fullName.trim().toLowerCase().replace(/\s+/g, '_') + '_' + Math.floor(1000 + Math.random() * 9000)
                : email?.split('@')[0] + '_' + Math.floor(1000 + Math.random() * 9000);
        }

        if (!email || !password) {
            return res.status(400).json({
                message: "Please provide all required fields (email, password)"
            });
        }

        // Check if user already exists
        const isAlreadyExists = await userModel.findOne({
            $or: [{ username }, { email: email.toLowerCase() }]
        });

        if (isAlreadyExists) {
            return res.status(400).json({
                message: isAlreadyExists.email === email.toLowerCase() 
                    ? "An account with this email already exists" 
                    : "Username is already taken"
            });
        }

        const hashedPassword = await bcrypt.hash(password, 12);

        const user = await userModel.create({
            username,
            email: email.toLowerCase(),
            password: hashedPassword
        });

        const token = jwt.sign(
            { id: user._id, userID: user._id },
            JWT_SECRET,
            { expiresIn: "7d" }
        );

        res.cookie("token", token, {
            httpOnly: true,
            secure: process.env.NODE_ENV === "production",
            sameSite: "lax",
            maxAge: 7 * 24 * 60 * 60 * 1000
        });

        return res.status(201).json({
            message: "User Registered Successfully",
            user: {
                id: user._id,
                username: user.username,
                email: user.email
            },
            token
        });
    } catch (error) {
        console.error("Error in registerUser:", error);
        return res.status(500).json({
            message: error.message || "Server Error"
        });
    }
};

const loginUser = async (req, res) => {
    try {
        const { email, password } = req.body;

        if (!email || !password) {
            return res.status(400).json({
                message: "Please provide email and password"
            });
        }

        const user = await userModel.findOne({ email: email.toLowerCase() });

        if (!user) {
            return res.status(400).json({
                message: "No account found with this email"
            });
        }

        const isPasswordValid = await bcrypt.compare(password, user.password);

        if (!isPasswordValid) {
            return res.status(400).json({
                message: "Invalid password"
            });
        }

        const token = jwt.sign(
            { id: user._id, userID: user._id },
            JWT_SECRET,
            { expiresIn: "7d" }
        );

        res.cookie("token", token, {
            httpOnly: true,
            secure: process.env.NODE_ENV === "production",
            sameSite: "lax",
            maxAge: 7 * 24 * 60 * 60 * 1000
        });

        return res.status(200).json({
            message: "User Logged In Successfully",
            user: {
                id: user._id,
                username: user.username,
                email: user.email
            },
            token
        });
    } catch (error) {
        console.error("Error in loginUser:", error);
        return res.status(500).json({
            message: error.message || "Server Error"
        });
    }
};

const getMe = async (req, res) => {
    try {
        if (!req.user) {
            return res.status(401).json({ message: "Not authenticated" });
        }
        return res.status(200).json({
            user: {
                id: req.user._id,
                username: req.user.username,
                email: req.user.email
            }
        });
    } catch (error) {
        return res.status(500).json({ message: "Server error", error: error.message });
    }
};

const logoutUserController = async (req, res) => {
    try {
        res.clearCookie("token");
        res.clearCookie("JWT_TOKEN");
        return res.status(200).json({
            message: "User logged out successfully"
        });
    } catch (error) {
        console.error("Error in logoutUserController:", error);
        return res.status(500).json({
            message: "Server Error",
            error: error.message
        });
    }
};

export { registerUser, loginUser, getMe, logoutUserController };
export default { registerUser, loginUser, getMe, logoutUserController };