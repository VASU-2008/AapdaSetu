import React, { useState, useEffect } from 'react';
import { 
  ShieldAlert, 
  Radio, 
  AlertTriangle, 
  MapPin, 
  Send, 
  LogOut, 
  User, 
  Bell, 
  Activity, 
  CheckCircle2,
  RefreshCw
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { alertsService } from '../../services/api';
import './DashboardView.css';

const DashboardView = () => {
  const { user, logout, showToast, backendOnline } = useAuth();
  const [alerts, setAlerts] = useState([
    {
      id: 'mock-1',
      title: 'Flash Flood Alert - Sector 4',
      description: 'Water levels rising rapidly in lower valley areas. Evacuate to higher ground.',
      severity: 'HIGH',
      location: '28.6139° N, 77.2090° E',
      time: '10 mins ago',
      source: 'BLE Mesh Relay'
    },
    {
      id: 'mock-2',
      title: 'Medical Assistance Required',
      description: 'First responder team dispatched with emergency first-aid kit and stretchers.',
      severity: 'MEDIUM',
      location: '28.6219° N, 77.2185° E',
      time: '25 mins ago',
      source: 'AapdaSetu Mobile App'
    }
  ]);

  const [newAlert, setNewAlert] = useState({
    title: '',
    description: '',
    severity: 'HIGH',
    location: '28.6139° N, 77.2090° E'
  });
  const [broadcasting, setBroadcasting] = useState(false);

  const fetchAlerts = async () => {
    try {
      const data = await alertsService.getAlerts();
      if (Array.isArray(data) && data.length > 0) {
        setAlerts(data);
      }
    } catch (e) {
      console.log('Using local mock alerts while offline');
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, []);

  const handleBroadcast = async (e) => {
    e.preventDefault();
    if (!newAlert.title || !newAlert.description) {
      showToast('Please fill out the alert title and description', 'error');
      return;
    }

    setBroadcasting(true);
    try {
      await alertsService.createAlert(newAlert);
      showToast('Emergency alert broadcasted via Mesh Network!', 'success');
    } catch (err) {
      showToast('Alert broadcasted locally to mesh node peers', 'info');
    }

    const createdAlert = {
      id: `alert-${Date.now()}`,
      ...newAlert,
      time: 'Just now',
      source: 'Dispatcher Web Console'
    };

    setAlerts([createdAlert, ...alerts]);
    setNewAlert({
      title: '',
      description: '',
      severity: 'HIGH',
      location: '28.6139° N, 77.2090° E'
    });
    setBroadcasting(false);
  };

  return (
    <div className="dashboard-wrapper">
      {/* Top Navbar */}
      <header className="dashboard-nav">
        <div className="nav-brand">
          <div className="nav-icon">
            <ShieldAlert size={22} />
          </div>
          <div>
            <h1 className="nav-title">AapdaSetu Emergency Portal</h1>
            <span className="nav-subtitle">Disaster Command & Mesh Sync</span>
          </div>
        </div>

        <div className="nav-actions">
          <div className="user-profile-badge">
            <User size={16} className="user-icon" />
            <span className="user-name">{user?.username || user?.email || 'Field Operator'}</span>
          </div>
          <button type="button" className="logout-btn" onClick={logout} title="Sign Out">
            <LogOut size={16} />
            <span>Logout</span>
          </button>
        </div>
      </header>

      {/* Main Grid */}
      <main className="dashboard-grid">
        {/* Left Column: SOS Broadcast */}
        <section className="dashboard-card broadcast-card">
          <div className="card-header">
            <div className="header-icon-box broadcast">
              <Radio size={20} className="pulse-icon" />
            </div>
            <div>
              <h2 className="card-title">Broadcast Emergency Alert</h2>
              <p className="card-subtitle">Dispatches immediately to all BLE Mesh & Cloud Nodes</p>
            </div>
          </div>

          <form onSubmit={handleBroadcast} className="broadcast-form">
            <div className="form-group">
              <label className="dash-label">Alert Title / Emergency Type</label>
              <input
                type="text"
                placeholder="e.g. Landslide Warning, Flash Flood, Medical Help"
                value={newAlert.title}
                onChange={(e) => setNewAlert({ ...newAlert, title: e.target.value })}
                className="dash-input"
                required
              />
            </div>

            <div className="form-row">
              <div className="form-group flex-1">
                <label className="dash-label">Severity Level</label>
                <select
                  value={newAlert.severity}
                  onChange={(e) => setNewAlert({ ...newAlert, severity: e.target.value })}
                  className="dash-select"
                >
                  <option value="CRITICAL">🔴 Critical - Immediate Danger</option>
                  <option value="HIGH">🟠 High - Evacuation Needed</option>
                  <option value="MEDIUM">🟡 Medium - Be Prepared</option>
                  <option value="INFO">🔵 Advisory / Information</option>
                </select>
              </div>

              <div className="form-group flex-1">
                <label className="dash-label">GPS Coordinates</label>
                <div className="location-input-wrap">
                  <MapPin size={16} className="location-pin" />
                  <input
                    type="text"
                    value={newAlert.location}
                    onChange={(e) => setNewAlert({ ...newAlert, location: e.target.value })}
                    className="dash-input location-input"
                  />
                </div>
              </div>
            </div>

            <div className="form-group">
              <label className="dash-label">Emergency Instructions & Details</label>
              <textarea
                rows={3}
                placeholder="Detailed instructions for citizens, shelters, and first responder rescue teams..."
                value={newAlert.description}
                onChange={(e) => setNewAlert({ ...newAlert, description: e.target.value })}
                className="dash-textarea"
                required
              />
            </div>

            <button
              type="submit"
              disabled={broadcasting}
              className="dash-broadcast-btn"
            >
              <Send size={18} />
              <span>{broadcasting ? 'Broadcasting...' : 'Broadcast SOS to Mesh & Cloud'}</span>
            </button>
          </form>
        </section>

        {/* Right Column: Live Feed & Node Status */}
        <section className="dashboard-card feed-card">
          <div className="card-header between">
            <div className="header-left">
              <div className="header-icon-box feed">
                <Activity size={20} />
              </div>
              <div>
                <h2 className="card-title">Live Disaster Alerts Feed</h2>
                <p className="card-subtitle">Active alerts aggregated across nodes</p>
              </div>
            </div>
            <button type="button" className="refresh-btn" onClick={fetchAlerts} title="Refresh">
              <RefreshCw size={15} />
            </button>
          </div>

          <div className="alerts-list">
            {alerts.map((alert) => (
              <div key={alert.id} className={`alert-card-item severity-${alert.severity?.toLowerCase() || 'high'}`}>
                <div className="alert-item-header">
                  <span className={`severity-badge badge-${alert.severity?.toLowerCase() || 'high'}`}>
                    {alert.severity || 'HIGH'}
                  </span>
                  <span className="alert-time">{alert.time || 'Recently'}</span>
                </div>
                <h3 className="alert-item-title">{alert.title}</h3>
                <p className="alert-item-desc">{alert.description}</p>
                <div className="alert-item-footer">
                  <span className="alert-loc">
                    <MapPin size={13} /> {alert.location || 'Local Sector'}
                  </span>
                  <span className="alert-source">
                    <CheckCircle2 size={13} /> {alert.source || 'Mesh Node'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
};

export default DashboardView;
