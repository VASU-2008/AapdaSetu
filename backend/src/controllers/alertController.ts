import { Request, Response } from 'express';

export interface DisasterAlert {
  id: string;
  title: string;
  type: 'FLOOD' | 'EARTHQUAKE' | 'CYCLONE' | 'LANDSLIDE' | 'GENERAL';
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  location: string;
  description: string;
  timestamp: string;
}

const mockAlerts: DisasterAlert[] = [
  {
    id: 'ALT-101',
    title: 'Flash Flood Warning',
    type: 'FLOOD',
    severity: 'HIGH',
    location: 'District North Sector 4',
    description: 'Heavy rainfall expected. Immediate evacuation advised for low-lying areas.',
    timestamp: new Date().toISOString(),
  },
  {
    id: 'ALT-102',
    title: 'Emergency Medical Camp Opened',
    type: 'GENERAL',
    severity: 'MEDIUM',
    location: 'Central Community Center',
    description: 'Medical relief and food supplies available at shelter #3.',
    timestamp: new Date().toISOString(),
  },
];

export const getAlerts = (req: Request, res: Response): Response => {
  try {
    return res.status(200).json({
      success: true,
      count: mockAlerts.length,
      data: mockAlerts,
    });
  } catch (error: any) {
    return res.status(500).json({
      success: false,
      message: 'Server Error',
      error: error?.message || 'Unknown error',
    });
  }
};

export const createAlert = (req: Request, res: Response): Response => {
  try {
    const { title, type, severity, location, description } = req.body;

    if (!title || !type || !severity || !location) {
      return res.status(400).json({
        success: false,
        message: 'Missing required alert fields (title, type, severity, location).',
      });
    }

    const newAlert: DisasterAlert = {
      id: `ALT-${Date.now().toString().slice(-4)}`,
      title,
      type,
      severity,
      location,
      description: description || '',
      timestamp: new Date().toISOString(),
    };

    mockAlerts.unshift(newAlert);

    return res.status(201).json({
      success: true,
      message: 'Alert created successfully',
      data: newAlert,
    });
  } catch (error: any) {
    return res.status(500).json({
      success: false,
      message: 'Server Error',
      error: error?.message || 'Unknown error',
    });
  }
};

export default { getAlerts, createAlert };
