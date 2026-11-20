import Course from '../models/course.model.js';
import CourseLecture from '../models/course_lecture.model.js';
import logger from '../utils/logger.js';
import * as Views from '../admin/views.js';
import { decode, encode } from '../admin/public/richtext.js';

const { LIMITS } = Views;

// Course.findById / CourseLecture.findById resolve to [rows]; these unwrap to a row or undefined.
async function findCourse(id) {
    if (!/^\d+$/.test(id)) return undefined;
    const [course] = await Course.findById(id);
    return course;
}

async function findLecture(id) {
    if (!/^\d+$/.test(id)) return undefined;
    const [lecture] = await CourseLecture.findById(id);
    return lecture;
}

const text = value => (typeof value === 'string' ? value : '');
const bytes = value => Buffer.byteLength(value, 'utf8');

function requireVarchar(errors, values, name, label) {
    if (values[name] === '') errors[name] = `${label} is required.`;
    else if (values[name].length > LIMITS.varchar) errors[name] = `${label} must be at most ${LIMITS.varchar} characters.`;
}

function validateCourse(body) {
    const values = {
        course_name: text(body.course_name).trim(),
        course_language: text(body.course_language).trim(),
        course_level: text(body.course_level).trim(),
        course_description: text(body.course_description).replace(/\r\n?/g, '\n').trim(),
    };
    const errors = {};
    requireVarchar(errors, values, 'course_name', 'Name');
    requireVarchar(errors, values, 'course_language', 'Language');
    requireVarchar(errors, values, 'course_level', 'Level');
    if (values.course_description === '') errors.course_description = 'Description is required.';
    else if (bytes(values.course_description) > LIMITS.text) errors.course_description = `Description must be at most ${LIMITS.text} bytes.`;
    return { values, errors };
}

function validateLecture(body) {
    // Re-encode so only well-formed RichText JSON (the app's format) reaches the database.
    const contents = decode(text(body.lecture_contents));
    const values = {
        lecture_name: text(body.lecture_name).trim(),
        lecture_contents: encode(contents),
    };
    const errors = {};
    requireVarchar(errors, values, 'lecture_name', 'Title');
    if (contents.text.trim() === '') errors.lecture_contents = 'Contents are required.';
    else if (bytes(values.lecture_contents) > LIMITS.text) {
        errors.lecture_contents = `Contents are too long: ${bytes(values.lecture_contents)} of at most ${LIMITS.text} bytes (formatting counts too).`;
    }
    return { values, errors };
}

async function knownLanguages() {
    const courses = await Course.findAll();
    return courses.map(c => c.course_language).sort((a, b) => a.localeCompare(b));
}

const notFound = (res, what) => res.status(404).send(Views.notFound(what));

// ---------------------------------------------------------------- courses

export async function index(req, res) {
    logger.info(`GET: /admin [INDEX]`);
    const courses = await Course.findAll();
    res.send(Views.courseList(courses));
}

export async function newCourse(req, res) {
    logger.info(`GET: /admin/courses/new`);
    res.send(Views.courseForm({ languages: await knownLanguages() }));
}

export async function storeCourse(req, res) {
    logger.info(`POST: /admin/courses [CREATE]`);
    const { values, errors } = validateCourse(req.body);
    if (Object.keys(errors).length > 0) {
        return res.status(422).send(Views.courseForm({ values, errors, languages: await knownLanguages() }));
    }

    const [course] = await Course.create(values);
    res.redirect(303, `/admin/courses/${course.id}`);
}

export async function showCourse(req, res) {
    logger.info(`GET: /admin/courses/${req.params.id}`);
    const course = await findCourse(req.params.id);
    if (!course) return notFound(res, 'Course');

    const [lectures] = await Course.fetchAllLecturesFromCourse(course.id);
    lectures.sort((a, b) => a.id - b.id);
    res.send(Views.courseDetail(course, lectures));
}

// ---------------------------------------------------------------- lectures

export async function newLecture(req, res) {
    logger.info(`GET: /admin/courses/${req.params.id}/lectures/new`);
    const course = await findCourse(req.params.id);
    if (!course) return notFound(res, 'Course');

    res.send(Views.lectureForm({ course }));
}

export async function storeLecture(req, res) {
    logger.info(`POST: /admin/courses/${req.params.id}/lectures [CREATE]`);
    const course = await findCourse(req.params.id);
    if (!course) return notFound(res, 'Course');

    const { values, errors } = validateLecture(req.body);
    if (Object.keys(errors).length > 0) {
        return res.status(422).send(Views.lectureForm({ course, values, errors }));
    }

    const [lecture] = await CourseLecture.create({ course_id: course.id, ...values });
    res.redirect(303, `/admin/lectures/${lecture.id}`);
}

export async function showLecture(req, res) {
    logger.info(`GET: /admin/lectures/${req.params.id}`);
    const lecture = await findLecture(req.params.id);
    if (!lecture) return notFound(res, 'Lecture');

    const course = await findCourse(String(lecture.course_id));
    res.send(Views.lectureDetail(course, lecture));
}

export async function editLecture(req, res) {
    logger.info(`GET: /admin/lectures/${req.params.id}/edit`);
    const lecture = await findLecture(req.params.id);
    if (!lecture) return notFound(res, 'Lecture');

    const course = await findCourse(String(lecture.course_id));
    res.send(Views.lectureForm({ course, lecture, values: lecture }));
}

export async function updateLecture(req, res) {
    logger.info(`POST: /admin/lectures/${req.params.id} [UPDATE]`);
    const lecture = await findLecture(req.params.id);
    if (!lecture) return notFound(res, 'Lecture');

    const { values, errors } = validateLecture(req.body);
    if (Object.keys(errors).length > 0) {
        const course = await findCourse(String(lecture.course_id));
        return res.status(422).send(Views.lectureForm({ course, lecture, values, errors }));
    }

    await CourseLecture.update(lecture.id, { course_id: lecture.course_id, ...values });
    res.redirect(303, `/admin/lectures/${lecture.id}`);
}
