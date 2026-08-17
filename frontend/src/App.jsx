import React from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import AuthContainer from './components/auth/AuthContainer';
import DashboardView from './components/dashboard/DashboardView';
import Toast from './components/common/Toast';

const MainAppContent = () => {
  const { user, toast, showToast } = useAuth();

  return (
    <div className="app-root-container">
      {/* Toast Notification Layer */}
      <Toast toast={toast} onClose={() => { }} />

      {/* Main Content: Dashboard when logged in, or Auth Showcase when logged out */}
      {user ? <DashboardView /> : <AuthContainer />}
    </div>
  );
};

const App = () => {
  return (
    <AuthProvider>
      <MainAppContent />
    </AuthProvider>
  );
};

export default App;