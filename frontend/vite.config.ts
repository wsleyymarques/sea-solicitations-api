import { defineConfig } from 'vite'

export default defineConfig({
  server: {
    proxy: Object.fromEntries(
      ['/auth', '/admin', '/analyst', '/solicitations', '/actuator', '/v3', '/swagger-ui']
        .map(path => [path, { target: process.env.VITE_API_TARGET || 'http://localhost:8080', changeOrigin: true }])
    ),
  },
})
