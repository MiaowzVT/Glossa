import { Router } from 'express';
import * as CourseLectureController from '../controllers/course_lecture.controller.js';

const router = Router();

router.get('/',              CourseLectureController.index);
router.get('/:id',           CourseLectureController.fetch);
router.post('',              CourseLectureController.store);
router.post('/:id',          CourseLectureController.update);
router.post('/:id/delete',   CourseLectureController.destroy);

export default router;