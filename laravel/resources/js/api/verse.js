import client from './client'

export async function getVerse(bible, book, chapter, verse) {
  const { data } = await client.get('/verse', {
    params: { bible, book, chapter, verse },
  })
  return data.data
}
