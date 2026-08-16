import { Router } from 'express';
import { getAlerts, createAlert } from '../controllers/alertController';

const router = Router();

router.get('/alerts', getAlerts);
router.post('/alerts', createAlert);

export default router;
