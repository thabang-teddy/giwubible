import client from './client'

// One call returns the verse in every translation that has it.
export async function getVerseComparisons(book, chapter, verse) {
  const { data } = await client.get('/verses', {
    params: { book, chapter, verse },
  })
  return data.data
}
