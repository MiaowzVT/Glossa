import { Router } from 'express';
import * as CourseController from '../controllers/course.controller.js';

const router = Router();

router.get('/',              CourseController.index);
router.get('/:id',           CourseController.fetch);
router.get('/:id/lectures',  CourseController.fetchAllLecturesFromCourse);
router.post('/',             CourseController.store);
router.post('/:id',          CourseController.update);
router.post('/:id/delete',   CourseController.destroy);

export default router;