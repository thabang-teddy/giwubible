import client from './client'

export async function getChapter(bible, book, chapter) {
  const { data } = await client.get('/chapter', {
    params: { bible, book, chapter },
  })
  return data.data
}
