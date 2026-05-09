import client from './client'

export async function getBooks() {
  const { data } = await client.get('/books')
  return data.data
}
