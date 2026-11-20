import { Router } from 'express';
import * as UserController from '../controllers/user.controller.js';

const router = Router();

router.get('/',                UserController.index);
router.get('/:id',             UserController.fetch);
router.post('/',               UserController.store);
router.post('/:id',            UserController.update);
router.post('/:id/delete',     UserController.destroy);

export default router;