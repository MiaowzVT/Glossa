import db from '../config/db.js';

const UGCCourseLecture = {
    async findAll() {
        const [rows] = await db.query(
            'SELECT * FROM ugc_lecture ORDER BY author_id DESC'
        );

        return rows;
    },

    async findById(id) {
        const [rows] = await db.query(
            'SELECT * FROM ugc_lecture WHERE id = ?',
            [id]
        );

        return rows ?? null;
    },

    async findByCourseId(courseId) {
        const [rows] = await db.query(
            'SELECT * FROM ugc_lecture WHERE ugc_course_id = ?',
            [courseId]
        );

        return rows ?? null;
    },

    async create({ course_id, lecture_name, lecture_contents }) {
        const [result] = await db.query(
            'INSERT INTO ugc_lecture (ugc_course_id, lecture_name, lecture_contents, created_at, updated_at) VALUES (?, ?, ?, ?, ?)',
            [course_id, lecture_name, lecture_contents, new Date(), new Date()]
        );

        return this.findById(result.insertId);
    },

    async update(id, { course_id, lecture_name, lecture_contents }) {
        const [result] = await db.query(
            'UPDATE ugc_lecture SET ugc_course_id = ?, lecture_name = ?, lecture_contents = ?, updated_at = ? WHERE id = ?',
            [course_id, lecture_name, lecture_contents, new Date(), id]
        );

        return result.affectedRows > 0 ? this.findById(id) : null;
    },

    async delete(id) {
        const [result] = await db.query('DELETE FROM ugc_lecture WHERE id = ?', id);

        return result.affectedRows > 0;
    }
}

export default UGCCourseLecture;