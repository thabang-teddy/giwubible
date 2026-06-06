import client from './client'

export async function listBookmarks() {
  const { data } = await client.get('/bookmarks')
  return data
}

export async function saveBookmark(bible, book, chapter, verse, text) {
  const { data } = await client.post('/bookmarks', { bible, book, chapter, verse, text })
  return data
}

export async function deleteBookmark(id) {
  await client.delete(`/bookmarks/${id}`)
}
