import assert from 'node:assert/strict'
import { readFileSync, statSync } from 'node:fs'
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
assert.match(
  devicesSource,
  /logDialogVisible/,
  'get logs should show a visible device log dialog instead of only logging to console'
)
assert.match(
  devicesSource,
  /batteryDialogVisible/,
  'battery report should show a visible report dialog instead of only logging to console'
)
assert.match(
  devicesSource,
  /status_history\.csv/,
  'status history export should download a CSV file'
)

const downlinkSource = readSource('src/api/downlink.js')
for (const endpoint of ['logs', 'battery-report', 'history-track', 'export-status-history']) {
  assert.match(
    downlinkSource,
    new RegExp(`/downlink/${endpoint}`),
    `downlink API should expose ${endpoint}`
  )
}

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
assert.match(
  realtimeSource,
  /care-marker/,
  'realtime map should use the high-density patient marker'
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
assert.match(
  patientDetailSource,
  /care-marker/,
  'patient detail map should use the high-density patient marker'
)

const historySource = readSource('src/views/History.vue')
assert.match(
  historySource,
  /normalizeLocationResponse/,
  'history page should normalize paged location responses before rendering markers'
)
assert.doesNotMatch(
  historySource,
  /historyLocations\.value = data/,
  'history page should not assign a PageResponse directly to historyLocations'
)
assert.match(
  historySource,
  /YYYY-MM-DDTHH:mm:ss/,
  'history page should send ISO-like datetime values accepted by the backend'
)
assert.doesNotMatch(
  historySource,
  /format\([^)]+\) \+ ' 00:00:00'/,
  'history quick filters should not send space-separated datetime values'
)
assert.match(
  historySource,
  /hasValidCoordinate/,
  'history page should filter invalid 0,0 locations before drawing the track'
)
assert.match(
  historySource,
  /fitMapToLatLngs\(latLngs\)/,
  'history page should automatically focus the map on valid track points after query'
)
assert.match(
  historySource,
  /formatBeijingTime/,
  'history page should render timestamps as Beijing time'
)
assert.match(
  historySource,
  /batteryLevel \?\? '-'/,
  'history page should render a real 0% battery level instead of replacing it with a dash'
)
assert.doesNotMatch(
  historySource,
  /new Date\(dateString\)\.toLocaleString\(\)/,
  'history page should not rely on browser-default timezone formatting'
)

assert.match(
  dashboardSource,
  /care-marker/,
  'dashboard map should use the high-density patient marker'
)
assert.match(
  readSource('index.html'),
  /favicon\.svg/,
  'app shell should use the branded SVG favicon instead of an inline blue square'
)
assert.match(
  readSource('index.html'),
  /favicon\.ico\?v=\d+/,
  'app shell should version the ICO favicon so browsers do not keep the stale blue square'
)
assert.ok(
  statSync(resolve(root, 'public/favicon.ico')).size > 1000,
  'ICO favicon should be the branded multi-size icon, not the old tiny blue square'
)
