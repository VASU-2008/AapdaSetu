import mongoose from "mongoose";

const ConnectDB = async () => {
    try {
        const conn = await mongoose.connect(process.env.MONGO_URI);
        const dbName = conn?.connection?.name || "AapdaSetu";
        console.log("DataBase Connected Successfully ✅");
        console.log("Database Name:", dbName);
    }
    catch (error) {
        console.error("❌ Database Connection Error:", error.message);
    }
}

export default ConnectDB;