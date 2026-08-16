import mongoose from "mongoose";

const ConnectDB = async (req, res) => {
    try {
        const conn = await mongoose.connect(process.env.MONGO_URI)
        console.log("DataBase Connected Successfully✅")
        console.log("Database: ", conn.Connection.name)
    }
    catch (error) {
        console.log("❌Error happen", error)
    }
}

export default ConnectDB;