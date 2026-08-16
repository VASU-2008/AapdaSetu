import userModel from "../models/user.model.js";
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';

const registerUser = async (req, res) => {
    try {
        const { username, email, password } = req.body;
        if (!username || !email || !password) {
            return res.status(400).json({
                message: "Please provide credentials (username, email, password)"
            });
        }

        const isAlreadyExists = await userModel.findOne({
            $or: [{ username }, { email }]
        });

        if (isAlreadyExists) {
            return res.status(400).json({
                message: "User Already Exists"
            });
        }

        const hashedPassword = await bcrypt.hash(password, 12);

        const user = await userModel.create({
            username,
            email,
            password: hashedPassword
        });

        const token = jwt.sign(
            { id: user._id },
            process.env.JWT_SECRET || "19371bb30e273afee29178d3712905e5cb619354e50168a9dc8cde125828f385",
            { expiresIn: "1d" }
        );

        res.cookie("token", token, {
            httpOnly: true,
            maxAge: 24 * 60 * 60 * 1000
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

const loginUser = async (req, res) => {
    try {
        const { email, password } = req.body;

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

        const token = jwt.sign(
            { id: user._id },
            process.env.JWT_SECRET || "19371bb30e273afee29178d3712905e5cb619354e50168a9dc8cde125828f385",
            { expiresIn: "1d" }
        );

        res.cookie("token", token, {
            httpOnly: true,
            maxAge: 24 * 60 * 60 * 1000
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

const logoutUserController = async (req, res) => {
    try {
        res.clearCookie("token");
        res.clearCookie("JWT_TOKEN");
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

export { registerUser, loginUser, logoutUserController };
export default { registerUser, loginUser, logoutUserController };