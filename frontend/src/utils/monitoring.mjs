export const isActiveBinding = (binding = {}) => {
  const active = binding.isActive ?? binding.active ?? binding.enabled
  return active !== false
}

export const getPatientDeviceBindings = (patient = {}) => {
  if (Array.isArray(patient.patientDevices)) return patient.patientDevices
  if (Array.isArray(patient.devices)) return patient.devices
  if (Array.isArray(patient.deviceBindings)) return patient.deviceBindings
  if (Array.isArray(patient.bindings)) return patient.bindings
  return []
}

export const isPatientMonitored = (patient = {}) => {
  if (patient.deviceId || patient.device?.id || patient.monitoringDeviceId) return true
  if (patient.monitoringStatus === 'monitoring' || patient.monitoringStatus === 'monitored') return true

  return getPatientDeviceBindings(patient).some(isActiveBinding)
}
