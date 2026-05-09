import client from './client'

export async function getBibles() {
  const { data } = await client.get('/bibles')
  return data.data
}
