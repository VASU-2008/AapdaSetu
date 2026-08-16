import mongoose from 'mongoose';

const userSchema = new mongoose.Schema({
    username: {
        type: String,
        unique: true,
        required: [true, "Username is required"],
        trim: true
    },
    email: {
        type: String,
        unique: true,
        required: [true, "Email is required"],
        lowercase: true,
        trim: true,
        match: [/^[^\s@]+@[^\s@]+\.[^\s@]+$/, "Invalid Email Address"]
    },
    password: {
        type: String,
        required: [true, "Password is required"]
    },
    systemUser: {
        type: Boolean,
        default: false,
        select: false
    }
}, { timestamps: true });

const userModel = mongoose.model("user", userSchema);

export default userModel;