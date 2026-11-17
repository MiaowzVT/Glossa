import db from '../config/db.js';

const User = {
  async findAll() {
    const [rows] = await db.query(
      'SELECT id, username, email, pw_hash, created_at, updated_at FROM users ORDER BY created_at DESC'
    );
    return rows;
  },

  async findById(id) {
    const [rows] = await db.query(
      'SELECT id, username, email, pw_hash, created_at, updated_at FROM users WHERE id = ?',
      [id]
    );
    return rows[0] ?? null;
  },

  async findByEmail(email) {
    const [rows] = await db.query('SELECT * FROM users WHERE email = ?', [email]);
    return rows[0] ?? null;
  },

  async findByUsername(username) {
    const [rows] = await db.query('SELECT * FROM users WHERE username = ?', [username]);
    return rows[0] ?? null;
  },

  async create({ username, email, pw_hash }) {
    const [result] = await db.query(
      'INSERT INTO users (username, email, pw_hash, created_at, updated_at) VALUES (?, ?, ?, ?, ?)',
      [username, email, pw_hash, new Date(), new Date()]
    );
    return this.findById(result.insertId);
  },

  async update(id, { username, email, pw_hash }) {
    const [result] = await db.query(
      'UPDATE users SET username = ?, email = ?, pw_hash = ?, updated_at = ? WHERE id = ?',
      [username, email, pw_hash, new Date(), id]
    );
    return result.affectedRows > 0 ? this.findById(id) : null;
  },

  async delete(id) {
    const [result] = await db.query('DELETE FROM users WHERE id = ?', [id]);
    return result.affectedRows > 0;
  },
};

export default User;