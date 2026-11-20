import CourseLecture from '../models/course_lecture.model.js';
import logger from '../utils/logger.js';

export async function index(req, res) {
    logger.info(`GET: /api/course_lecture [INDEX]`);
    const lectures = await CourseLecture.findAll();
    res.end(JSON.stringify(lectures));
}

export async function fetch(req, res) {
    logger.info(`GET: /api/course_lecture/${req.params.id} [FETCH]`);
    const lecture = await CourseLecture.findById(req.params.id);

    if(!lecture) {
        logger.error(`FETCH Operation FAILED: Lecture ${req.params.id} not found`);
        return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(lecture));
}

export async function store(req, res) {
    logger.info(`POST: /api/course_lecture [CREATE]`);
    console.log(req.body);

    const lecture = await CourseLecture.create(req.body);
    if (!lecture) {
        logger.error(`CREATE Operation FAILED. ${req.body}`);
        return res.status(500).end();
    }

    res.status(200).end(JSON.stringify(lecture));
}

export async function update(req, res) {
    logger.info(`POST: /api/course_lecture/${req.params.id} [UPDATE]`);

    const lecture = await CourseLecture.update(req.params.id, req.body);

    if (!lecture) {
        logger.error(`UPDATE Operation FAILED: Lecture ${req.params.id} not found`);
        return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(lecture));
}

export async function destroy(req, res) {
    logger.info(`POST: /api/course_lecture/${req.params.id}/delete [DELETE]`);

    await CourseLecture.delete(req.params.id);
    res.status(200).end();
}