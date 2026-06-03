import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { isPatientMonitored } from '../src/utils/monitoring.mjs'

const root = resolve(import.meta.dirname, '..')
const readSource = (path) => readFileSync(resolve(root, path), 'utf8')

assert.equal(
  isPatientMonitored({
    id: 7,
    deviceId: null,
    patientDevices: [{ patientId: 7, deviceId: 15, isActive: true }]
  }),
  true,
  'active patient_devices binding should mark the patient as monitored'
)

assert.equal(
  isPatientMonitored({
    id: 8,
    deviceId: null,
    patientDevices: [{ patientId: 8, deviceId: 16, isActive: false }]
  }),
  false,
  'inactive patient_devices binding should not mark the patient as monitored'
)

for (const view of ['src/views/Devices.vue', 'src/views/Patient.vue']) {
  const source = readSource(view)
  assert.match(source, /class="[^"]*full-width-table[^"]*"/, `${view} should mark the main table as full width`)
  assert.match(source, /<el-table-column[^>]+min-width="/, `${view} should use flexible min-width columns`)
}

const dashboardSource = readSource('src/views/Dashboard.vue')
assert.match(
  dashboardSource,
  /device\.patient\?\.id/,
  'dashboard patient monitoring stats should include device.patient bindings'
)
assert.doesNotMatch(
  dashboardSource,
  /allPatients\.filter\(p => p\.deviceId\)\.length/,
  'dashboard should not rely only on patient.deviceId for monitoring stats'
)

const devicesSource = readSource('src/views/Devices.vue')
assert.match(
  devicesSource,
  /class="action-row"/,
  'device operation buttons should stay on one row'
)
assert.match(
  devicesSource,
  /device-action-panel/,
  'device dropdown should use the grouped side-panel menu'
)
assert.match(
  devicesSource,
  /device-action-section/,
  'device dropdown should render collapsible action sections'
)

const patientSource = readSource('src/views/Patient.vue')
assert.match(
  patientSource,
  /class="patient-action-row"/,
  'patient operation buttons should stay on one row'
)

const realtimeSource = readSource('src/views/Realtime.vue')
assert.match(
  realtimeSource,
  /formatLocationText\(location\)/,
  'realtime marker popup should show coordinate fallback when address is missing'
)
assert.match(
  realtimeSource,
  /normalizeDeviceId\(selectedDeviceId\.value\)/,
  'realtime marker lookup should normalize route/radio device ids'
)
assert.match(
  realtimeSource,
  /marker\.openPopup\(\)/,
  'selected realtime marker should open its location popup automatically'
)

const patientDetailSource = readSource('src/views/PatientDetail.vue')
assert.match(
  patientDetailSource,
  /await nextTick\(\)[\s\S]*renderPatientMap\(\)/,
  'patient detail map should render after the location DOM exists'
)
assert.match(
  patientDetailSource,
  /map\.invalidateSize\(\)/,
  'patient detail map should invalidate size after rendering in a card'
)
assert.match(
  patientDetailSource,
  /setInterval\(\(\) => fetchLatestLocation\(\), 30000\)/,
  'patient detail map should refresh the latest location periodically'
)
