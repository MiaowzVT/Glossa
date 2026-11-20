import UGCCourse from '../models/ugc_course.model.js';
import logger from '../utils/logger.js';

export async function index(req, res) {
    logger.info(`GET: /api/ugc_course [INDEX]`);
    const courses = await UGCCourse.findAll();
    res.end(JSON.stringify(courses));
}

export async function fetch(req, res) {
    logger.info(`GET: /api/ugc_course/${req.params.id} [FETCH]`);
    const course = await UGCCourse.findById(req.params.id);

    if(!course) {
        logger.error(`FETCH Operation FAILED: Course ${req.params.id} not found`);
        return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(course));
}

export async function fetchAllCoursesByAuthor(req, res) {
    logger.info(`GET: /api/ugc_course/author/${req.params.id} [FETCH]`);
    const course = await UGCCourse.findByAuthor(req.params.id);

    if(!course) {
        logger.error(`FETCH Operation FAILED: No courses from author ${req.params.id} found`);
        return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(course));
}

export async function fetchAllLecturesFromCourse(req, res) {
    logger.info(`GET: /api/ugc_course/${req.params.id}/lectures [FETCH]`);

    const [lectures] = await UGCCourse.fetchAllLecturesFromCourse(req.params.id);

    if(!lectures) {
        logger.error(`FETCH Operation FAILED: Course ${req.params.id} not found`);
        return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(lectures));
}

export async function store(req, res) {
    logger.info(`POST: /api/ugc_course [CREATE]`);
    console.log(req.body);

    const course = await UGCCourse.create(req.body);
    if (!course) {
        logger.error(`CREATE Operation FAILED. ${req.body}`);
        return res.status(500).end();
    }

    res.status(200).end(JSON.stringify(course));
}

export async function update(req, res) {
    logger.info(`POST: /api/ugc_course/${req.params.id} [UPDATE]`);

    const course = await UGCCourse.update(req.params.id, req.body);

    if (!course) {
        logger.error(`UPDATE Operation FAILED: Course ${req.params.id} not found`);
        return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(course));
}

export async function destroy(req, res) {
    logger.info(`POST: /api/ugc_course/${req.params.id}/delete [DELETE]`);

    await UGCCourse.delete(req.params.id);
    res.status(200).end();
}