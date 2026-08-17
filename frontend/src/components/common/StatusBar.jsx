import React from 'react';
import { Wifi, BatteryMedium, Signal } from 'lucide-react';
import './StatusBar.css';

const StatusBar = ({ time = '9:41' }) => {
  return (
    <div className="status-bar">
      <span className="status-time">{time}</span>
      <div className="status-icons">
        <Signal size={14} strokeWidth={2.5} className="status-icon" />
        <Wifi size={14} strokeWidth={2.5} className="status-icon" />
        <BatteryMedium size={16} strokeWidth={2.5} className="status-icon" />
      </div>
    </div>
  );
};

export default StatusBar;
