import Course from '../models/course.model.js';
import logger from '../utils/logger.js';

export async function index(req, res) {
    logger.info(`GET: /api/course [INDEX]`);
    const courses = await Course.findAll();
    res.json(courses);
}

export async function fetch(req, res) {
    logger.info(`GET: /api/course/${req.params.id} [FETCH]`);
    const course = await Course.findById(req.params.id);

    if(!course) {
        logger.error(`FETCH Operation FAILED: Course ${req.params.id} not found`);
        return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(course));
}

export async function fetchAllLecturesFromCourse(req, res) {
    logger.info(`GET: /api/course/${req.params.id}/lectures [FETCH]`);

    const [lectures] = await Course.fetchAllLecturesFromCourse(req.params.id);

    if(!lectures) {
        logger.error(`FETCH Operation FAILED: Course ${req.params.id} not found`);
        return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(lectures));
}

export async function store(req, res) {
    logger.info(`POST: /api/course [CREATE]`);
    console.log(req.body);

    const course = await Course.create(req.body);
    if (!course) {
        logger.error(`CREATE Operation FAILED. ${req.body}`);
        return res.status(500).end();
    }

    res.status(200).end(JSON.stringify(course));
}

export async function update(req, res) {
    logger.info(`POST: /api/course/${req.params.id} [UPDATE]`);

    const course = await Course.update(req.params.id, req.body);

    if (!course) {
        logger.error(`UPDATE Operation FAILED: Course ${req.params.id} not found`);
        return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(course));
}

export async function destroy(req, res) {
    logger.info(`POST: /api/course/${req.params.id}/delete [DELETE]`);

    await Course.delete(req.params.id);
    res.status(200).end();
}