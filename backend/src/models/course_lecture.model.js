import db from '../config/db.js';

const CourseLecture = {
    async findAll() {
        const [rows] = await db.query(
            'SELECT * FROM official_course_lecture ORDER BY lecture_name DESC'
        );

        return rows;
    },

    async findById(id) {
        const [rows] = await db.query(
            'SELECT * FROM official_course_lecture WHERE id = ?',
            [id]
        );

        return [rows] ?? null;
    },

    async create({ course_id, lecture_name, lecture_contents }) {
        const [result] = await db.query(
            'INSERT INTO official_course_lecture (course_id, lecture_name, lecture_contents, created_at, updated_at) VALUES (?, ?, ?, ?, ?)',
            [course_id, lecture_name, lecture_contents, new Date(), new Date()]
        );

        return this.findById(result.insertId);
    },

    async update(id, { course_id, lecture_name, lecture_contents }) {
        const [result] = await db.query(
            'UPDATE official_course_lecture SET course_id = ?, lecture_name = ?, lecture_contents = ?, updated_at = ? WHERE id = ?',
            [course_id, lecture_name, lecture_contents, new Date(), id]
        );

        return result.affectedRows > 0 ? this.findById(id) : null;
    },

    async delete(id) {
        const [result] = await db.query('DELETE FROM official_course_lecture WHERE id = ?', id);

        return result.affectedRows > 0;
    }
}

export default CourseLecture;