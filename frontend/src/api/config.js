const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

export default {
  baseURL: API_BASE_URL,
  timeout: 30000
}

