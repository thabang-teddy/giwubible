import client from './client'

export async function register(name, email, password) {
  const { data } = await client.post('/auth/register', {
    name,
    email,
    password,
    password_confirmation: password,
  })
  return data
}

export async function login(email, password) {
  const { data } = await client.post('/auth/login', { email, password })
  return data
}

export async function logout() {
  await client.post('/auth/logout')
}

export async function me() {
  const { data } = await client.get('/auth/me')
  return data
}
