import React from 'react';
import { CheckCircle2, Circle } from 'lucide-react';
import './PasswordChecklist.css';

const PasswordChecklist = ({ password = '' }) => {
  const criteria = [
    {
      id: 'length',
      label: 'At least 8 characters',
      valid: password.length >= 8,
    },
    {
      id: 'number',
      label: 'Include a number',
      valid: /\d/.test(password),
    },
    {
      id: 'uppercase',
      label: 'Include an uppercase letter',
      valid: /[A-Z]/.test(password),
    },
  ];

  return (
    <div className="password-checklist">
      {criteria.map((item) => (
        <div
          key={item.id}
          className={`checklist-item ${item.valid ? 'is-valid' : ''}`}
        >
          {item.valid ? (
            <CheckCircle2
              size={17}
              strokeWidth={2.4}
              className="check-icon active-check"
            />
          ) : (
            <div className="check-placeholder" />
          )}
          <span className="checklist-label">{item.label}</span>
        </div>
      ))}
    </div>
  );
};

export default PasswordChecklist;
