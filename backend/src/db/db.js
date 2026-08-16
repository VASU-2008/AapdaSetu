import mongoose from "mongoose";

const ConnectDB = async () => {
    try {
        const conn = await mongoose.connect(process.env.MONGO_URI);
        console.log("DataBase Connected Successfully ✅");
        console.log("Database Name:", conn.connection.name);
    } catch (error) {
        console.error("❌ Error connecting to database:", error);
    }
};

export default ConnectDB;
