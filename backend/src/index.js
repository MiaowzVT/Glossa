import dotenv from 'dotenv/config';
import express from 'express';
import logger from './utils/logger.js';

import userRoutes from './routes/user.routes.js';
import courseRoutes from './routes/course.routes.js';
import courseLectureRoutes from './routes/course_lecture.routes.js';

const app = express();
const port = 8096;

app.use(express.json());
app.use(express.urlencoded({extended: true}));

app.use('/users', userRoutes);
app.use('/courses', courseRoutes);
app.use('/course_lectures', courseLectureRoutes);

app.listen(port, () => {
    console.log(`
 _____ _     ____  ____  ____  ____ 
/  __// \\   /  _ \\/ ___\\/ ___\\/  _ \\
| |  _| |   | / \\||    \\|    \\| / \\|
| |_//| |_/\\| \\_/|\\___ |\\___ || |-||
\\____\\\\____/\\____/\\____/\\____/\\_/ \\|
                                    `);
    logger.info(`Server listening on port ${port}`);

    if(process.env.DB_USER === undefined) {
        logger.error(".env was NOT configured. Database Connection can NOT be established.");
        logger.error("Stop the server and re-configure.");
    }
});