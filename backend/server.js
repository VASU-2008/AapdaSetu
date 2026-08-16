import 'dotenv/config';
import app from "./src/app.js";
import ConnectDB from "./src/db/db.js";

const PORT = process.env.PORT || 3000;

ConnectDB().then(() => {
    app.listen(PORT, () => {
        console.log(`🚀 Server is running on port: ${PORT}`);
    });
}).catch((err) => {
    console.error("Failed to connect to database before starting server:", err);
});
