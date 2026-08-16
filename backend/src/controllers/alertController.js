const mockAlerts = [
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

export const getAlerts = (req, res) => {
  try {
    return res.status(200).json({
      success: true,
      count: mockAlerts.length,
      data: mockAlerts,
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server Error',
      error: error.message,
    });
  }
};

export const createAlert = (req, res) => {
  try {
    const { title, type, severity, location, description } = req.body;

    if (!title || !type || !severity || !location) {
      return res.status(400).json({
        success: false,
        message: 'Missing required alert fields (title, type, severity, location).',
      });
    }

    const newAlert = {
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
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Server Error',
      error: error.message,
    });
  }
};

export default { getAlerts, createAlert };
