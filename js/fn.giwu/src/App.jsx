import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './hooks/useAuth'
import HomePage from './pages/HomePage'
import ReadPage from './pages/ReadPage'
import DownloadPage from './pages/DownloadPage'
import LoginPage from './pages/LoginPage'
import BookmarksPage from './pages/BookmarksPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/read" element={<ReadPage />} />
          <Route path="/download" element={<DownloadPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/bookmarks" element={<BookmarksPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
