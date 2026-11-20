import db from '../config/db.js';

const UGCCourse = {
    async findAll() {
        const [rows] = await db.query(
            'SELECT * FROM ugc_course ORDER BY author_id DESC'
        );

        return rows;
    },

    async findById(id) {
        const [rows] = await db.query(
            'SELECT * FROM ugc_course WHERE id = ?',
            [id]
        );

        return [rows] ?? null;
    },

    async findByAuthor(id) {
        const [rows] = await db.query(
            'SELECT * FROM ugc_course WHERE author_id = ?',
            [id]
        );

        return [rows] ?? null;
    },

    async fetchAllLecturesFromCourse(id) {
        const [rows] = await db.query(
            'SELECT * FROM ugc_lecture WHERE ugc_course_id = ?',
            [id]
        );

        return [rows] ?? null;
    },

    async create({ author_id, course_name, course_description, course_language }) {
        const [result] = await db.query(
            'INSERT INTO ugc_course (author_id course_name, course_description, course_language, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)',
            [author_id, course_name, course_description, course_language, new Date(), new Date()]
        );

        return this.findById(result.insertId);
    },

    async update(id, { author_id, course_name, course_description, course_language }) {
        const [result] = await db.query(
            'UPDATE ugc_course SET author_id = ?, course_name = ?, course_description = ?, course_language = ?, updated_at = ? WHERE id = ?',
            [author_id, course_name, course_description, course_language, new Date(), id]
        );

        return result.affectedRows > 0 ? this.findById(id) : null;
    },

    async delete(id) {
        const [result] = await db.query('DELETE * FROM ugc_course WHERE id = ?', id);

        return result.affectedRows > 0;
    }
}

export default UGCCourse;