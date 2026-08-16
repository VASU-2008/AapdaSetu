import { Router } from 'express';
import alertController from '../controllers/alertController.js';

const router = Router();

router.get('/alerts', alertController.getAlerts);
router.post('/alerts', alertController.createAlert);

export default router;
