const appointmentTypeLabels = {
  'MEASUREMENT': 'Medición',
  'NUTRITION': 'Nutrición',
  'CONSULTATION': 'Consulta',
};

String appointmentTypeLabel(String type) => appointmentTypeLabels[type] ?? type;
