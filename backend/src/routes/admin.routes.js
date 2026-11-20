import express, { Router } from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import * as AdminController from '../controllers/admin.controller.js';

const router = Router();
const assets = path.join(path.dirname(fileURLToPath(import.meta.url)), '../admin/public');

router.use('/assets', express.static(assets));

router.get('/',                          AdminController.index);
router.get('/courses/new',               AdminController.newCourse);
router.post('/courses',                  AdminController.storeCourse);
router.get('/courses/:id',               AdminController.showCourse);
router.get('/courses/:id/lectures/new',  AdminController.newLecture);
router.post('/courses/:id/lectures',     AdminController.storeLecture);
router.get('/lectures/:id',              AdminController.showLecture);
router.get('/lectures/:id/edit',         AdminController.editLecture);
router.post('/lectures/:id',             AdminController.updateLecture);

export default router;
