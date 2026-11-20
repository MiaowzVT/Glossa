import { Router } from 'express';
import * as UGCCourseController from '../controllers/ugc_course.controller.js';

const router = Router();

router.get('/',              UGCCourseController.index);
router.get('/:id',           UGCCourseController.fetch);
router.get('/author/:id',    UGCCourseController.fetchAllCoursesByAuthor);
router.get('/:id/lectures',  UGCCourseController.fetchAllLecturesFromCourse);
router.post('',              UGCCourseController.store);
router.post('/:id',          UGCCourseController.update);
router.post('/:id/delete',   UGCCourseController.destroy);

export default router;