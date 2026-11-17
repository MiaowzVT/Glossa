import db from '../config/db.js';

const Course = {
    async findAll() {
        const [rows] = await db.query(
            'SELECT * FROM official_course ORDER BY course_language DESC'
        );

        return rows;
    },

    async findById(id) {
        const [rows] = await db.query(
            'SELECT * FROM official_course WHERE id = ?',
            [id]
        );

        return [rows] ?? null;
    },

    async findByLanguage(language) {
        const [rows] = await db.query(
            'SELECT * FROM official_course WHERE language = ?',
            [language]
        );

        return [rows] ?? null;
    },

    async fetchAllLecturesFromCourse(id) {
        const [rows] = await db.query(
            'SELECT * FROM official_course_lecture WHERE course_id = ?',
            [id]
        );

        return [rows] ?? null;
    },

    async create({ course_name, course_description, course_level, course_language }) {
        const [result] = await db.query(
            'INSERT INTO official_course (course_name, course_description, course_level, course_language) VALUES (?, ?, ?, ?)',
            [course_name, course_description, course_level, course_language]
        );

        return this.findById(result.insertId);
    },

    async update(id, { course_name, course_description, course_level, course_language }) {
        const [result] = await db.query(
            'UPDATE official_course SET course_name = ?, course_description = ?, course_level = ?, course_language = ?',
            [course_name, course_description, course_level, course_language]
        );

        return result.affectedRows > 0 ? this.findById(id) : null;
    },

    async delete(id) {
        const [result] = await db.query('DELETE * FROM official_course WHERE id = ?', id);

        return result.affectedRows > 0;
    }
}

export default Course;