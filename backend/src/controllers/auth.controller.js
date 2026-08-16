import userModel from "../models/user.model.js";
import blacklistTokenModel from "../models/blacklistToken.model.js";
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

export const registerUser = async (req, res) => {
    try {
        const { username, email, password } = req.body;
        if (!username || !email || !password) {
            return res.status(400).json({
                message: "Please provide all required fields (username, email, password)."
            });
        }

        const isAlreadyExists = await userModel.findOne({
            $or: [{ username }, { email }]
        });

        if (isAlreadyExists) {
            return res.status(400).json({
                message: "User with this username or email already exists."
            });
        }

        const hashedPassword = await bcrypt.hash(password, 12);

        const user = await userModel.create({
            username,
            email,
            password: hashedPassword
        });

        const secret = process.env.JWT_SECRET || "aapda_setu_jwt_secret_key_2026";

        const token = jwt.sign(
            { id: user._id },
            secret,
            { expiresIn: "1d" }
        );

        res.cookie("token", token, {
            httpOnly: true,
            maxAge: 24 * 60 * 60 * 1000 // 1 day
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
            message: "Server Error",
            error: error.message
        });
    }
};

export const loginUser = async (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({
                message: "Please provide email and password."
            });
        }

        const user = await userModel.findOne({ email });

        if (!user) {
            return res.status(400).json({
                message: "Invalid Credentials"
            });
        }

        const isPasswordValid = await bcrypt.compare(password, user.password);

        if (!isPasswordValid) {
            return res.status(400).json({
                message: "Invalid Password"
            });
        }

        const secret = process.env.JWT_SECRET || "aapda_setu_jwt_secret_key_2026";

        const token = jwt.sign(
            { id: user._id },
            secret,
            { expiresIn: "1d" }
        );

        res.cookie("token", token, {
            httpOnly: true,
            maxAge: 24 * 60 * 60 * 1000 // 1 day
        });

        return res.status(200).json({
            message: "User LoggedIn Successfully",
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
            message: "Server Error",
            error: error.message
        });
    }
};

export const logoutUserController = async (req, res) => {
    try {
        const token = req.cookies?.token || req.headers.authorization?.split(" ")[1];
        if (token) {
            await blacklistTokenModel.create({ token });
        }

        res.clearCookie("token");
        return res.status(200).json({
            message: "User logged out Successfully"
        });
    } catch (error) {
        console.error("Error in logoutUserController:", error);
        return res.status(500).json({
            message: "Server Error",
            error: error.message
        });
    }
};

export default { registerUser, loginUser, logoutUserController };
