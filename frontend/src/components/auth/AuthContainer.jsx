import React, { useState } from 'react';
import { Smartphone, LayoutGrid, Radio, ShieldAlert } from 'lucide-react';
import LoginView from './LoginView';
import RegisterView from './RegisterView';
import { useAuth } from '../../context/AuthContext';
import './AuthContainer.css';

const AuthContainer = () => {
  const { backendOnline } = useAuth();
  // 'both' (side-by-side matching screenshot) | 'login' | 'register'
  const [activeTab, setActiveTab] = useState('both');

  return (
    <div className="auth-showcase-wrapper">
      {/* Background Decorative Gradient Waves & Glows */}
      <div className="ambient-glow glow-top-left" />
      <div className="ambient-glow glow-bottom-right" />
      <div className="ambient-wave-bottom" />

      {/* Top App Control Bar */}
      <header className="showcase-header">
        <div className="brand-badge">
          <div className="brand-logo-icon">
            <ShieldAlert size={20} strokeWidth={2.4} />
          </div>
          <div className="brand-info">
            <span className="brand-title">AapdaSetu</span>
            <span className="brand-subtitle">Emergency Response Platform</span>
          </div>
        </div>

        {/* View Switcher Controls */}
        <div className="view-mode-tabs">
          <button
            type="button"
            className={`mode-tab-btn ${activeTab === 'both' ? 'active' : ''}`}
            onClick={() => setActiveTab('both')}
          >
            <LayoutGrid size={15} />
            <span>Dual View</span>
          </button>
          <button
            type="button"
            className={`mode-tab-btn ${activeTab === 'login' ? 'active' : ''}`}
            onClick={() => setActiveTab('login')}
          >
            <Smartphone size={15} />
            <span>Login</span>
          </button>
          <button
            type="button"
            className={`mode-tab-btn ${activeTab === 'register' ? 'active' : ''}`}
            onClick={() => setActiveTab('register')}
          >
            <Smartphone size={15} />
            <span>Register</span>
          </button>
        </div>

        {/* Backend Connectivity Status Badge */}
        <div className={`backend-status-pill ${backendOnline ? 'online' : 'offline'}`}>
          <Radio size={13} className="status-ping" />
          <span>{backendOnline ? 'API Connected (:5000)' : 'API Offline (Demo Mode)'}</span>
        </div>
      </header>

      {/* Showcase Cards Grid */}
      <main className="showcase-grid-container">
        {(activeTab === 'both' || activeTab === 'login') && (
          <div className="card-column slide-up">
            <LoginView onSwitchToRegister={() => setActiveTab('register')} />
          </div>
        )}

        {(activeTab === 'both' || activeTab === 'register') && (
          <div className="card-column slide-up">
            <RegisterView onSwitchToLogin={() => setActiveTab('login')} />
          </div>
        )}
      </main>
    </div>
  );
};

export default AuthContainer;
