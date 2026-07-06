import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.lunahub.android',
  appName: 'Luna Hub',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
  },
}

export default config
