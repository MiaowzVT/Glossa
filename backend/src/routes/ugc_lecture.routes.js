import { Router } from 'express';
import * as UGCLectureController from '../controllers/ugc_lecture.controller.js';

const router = Router();

router.get('/',             UGCLectureController.index);
router.get('/:id',          UGCLectureController.fetch);
router.get('/course/:id',   UGCLectureController.fetchByCourseId);
router.post('/',            UGCLectureController.store);
router.post('/:id',         UGCLectureController.update);
router.post('/:id/delete',  UGCLectureController.destroy);

export default router;