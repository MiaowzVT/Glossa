import dotenv from 'dotenv/config';
import express from 'express';
import logger from './utils/logger.js';

import userRoutes from './routes/user.routes.js';
import courseRoutes from './routes/course.routes.js';
import courseLectureRoutes from './routes/course_lecture.routes.js';
import ugcCourseRoutes from './routes/ugc_course.routes.js';
import ugcLectureRoutes from './routes/ugc_lecture.routes.js';
import adminRoutes from './routes/admin.routes.js';

const app = express();
const port = 8096;

app.use(express.json());
app.use(express.urlencoded({extended: true}));

app.use('/api/user', userRoutes);
app.use('/api/course', courseRoutes);
app.use('/api/course_lecture', courseLectureRoutes);
app.use('/api/ugc_course', ugcCourseRoutes);
app.use('/api/ugc_lecture', ugcLectureRoutes);
app.use('/admin', adminRoutes);

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