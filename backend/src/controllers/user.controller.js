import User from '../models/user.model.js';
import logger from '../utils/logger.js';

export async function index(req, res) {
    logger.info(`GET: /api/user [INDEX]`);
    const users = await User.findAll();
    res.end(JSON.stringify(users));
}

export async function fetch(req, res) {
    logger.info(`GET: /api/user/${req.params.id} [FETCH]`);

    const user = await User.findById(req.params.id);
    if (!user) {
      logger.error(`FETCH Operation FAILED: User ${req.params.id} not found`);
      return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(user));
}

export async function store(req, res) {
    logger.info(`POST: /api/user [CREATE]`);

    if (await User.findByEmail(req.body.email)) {
      logger.error(`CREATE Operation FAILED: E-Mail ${req.body.email} already taken`);
      return res.status(422).end(JSON.stringify({"error": "E-Mail already taken"}));
    }

    if (await User.findByUsername(req.body.username)) {
      logger.error(`CREATE Operation FAILED: Username ${req.body.username} already taken`);
      return res.status(422).end(JSON.stringify({"error": "Username already taken"}));
    }

    const user = await User.create(req.body);
    res.status(200).end(JSON.stringify(user));
}

export async function update(req, res) {
    logger.info(`POST: /api/user/${req.params.id} [UPDATE]`);

    const user = await User.update(req.params.id, req.body);
    if (!user) {
      logger.error(`UPDATE Operation FAILED: User ${req.params.id} not found`);
      return res.status(404).end();
    }

    res.status(200).end(JSON.stringify(user));
}

export async function destroy(req, res) {
    logger.info(`POST: /api/user/${req.params.id}/delete [DELETE]`);
    await User.delete(req.params.id);
    res.status(200).end();
}