# Data Safety notes (MS-052, for the Play Data Safety form)

- Data collected: none by the app itself.
- Data shared: none. Network requests go only to user-chosen servers.
- Permissions: videos/images (user-selected media), notifications (playback
  controls, opt-in on Android 13+). No location, no contacts, no all-files
  access (SAF grants only).
- Encryption in transit: yes (HTTPS/TLS; cleartext HTTP unsupported).
- Account deletion: n/a (no accounts, no server-side data).
